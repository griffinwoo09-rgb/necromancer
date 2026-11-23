package com.example.necromancer.entity;

import com.example.necromancer.NecromancerConfig;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A Necromancer's summoned melee skeleton: sword or spear, sometimes spear + a shield it actually raises.
 * Looks and drops like a vanilla skeleton; its own type so the extra goals survive save/load.
 */
public class NecromancerSkeleton extends Skeleton {
	public NecromancerSkeleton(EntityType<? extends NecromancerSkeleton> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// spear charge holds the spear "in use", which would keep the shield down, so shield bearers just jab
		this.goalSelector.addGoal(2, new SpearUseGoal<>(this, 1.0, 1.0, 10.0F, 2.0F) {
			@Override
			public boolean canUse() {
				return !hasShield() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !hasShield() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(3, new RaiseShieldGoal());
	}

	/** The Necromancer that raised this skeleton; null for ones spawned any other way, which never crumble. */
	private UUID summoner;
	/** Consecutive once-a-second checks that found no living summoner. */
	private int orphanChecks;

	public void setSummoner(Necromancer necromancer) {
		this.summoner = necromancer.getUUID();
	}

	public boolean isSummonedBy(Necromancer necromancer) {
		return necromancer.getUUID().equals(summoner);
	}

	/** Falls apart in a puff of soul smoke, dropping nothing. */
	public void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		playSound(SoundEvents.SKELETON_DEATH, 0.8F, 1.3F);
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		// summoner gone (killed out of range, unloaded, /kill'd): the army goes too after ~5 s
		if (summoner != null && this.tickCount % 20 == 0 && level() instanceof ServerLevel server) {
			Entity owner = server.getEntity(summoner);
			orphanChecks = owner instanceof Necromancer s && s.isAlive() ? 0 : orphanChecks + 1;
			if (orphanChecks >= NecromancerConfig.ORPHAN_CRUMBLE_SECONDS) crumble(server);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (summoner != null) output.store("Summoner", UUIDUtil.CODEC, summoner);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.summoner = input.read("Summoner", UUIDUtil.CODEC).orElse(null);
	}

	/**
	 * Every skeleton and Necromancer is on the same side, whichever Necromancer raised it. Vanilla targeting skips allies,
	 * so armies of two Necromancers never pick each other (or retaliate against each other) as targets.
	 */
	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof AbstractSkeleton || other instanceof Necromancer || super.considersEntityAsAlly(other);
	}

	/** No friendly fire: a spear charge or stray arrow from an ally does nothing, so it can't start a brawl either. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
		if (attacker != null && attacker != this && isAlliedTo(attacker)) return false;
		return super.hurtServer(level, source, amount);
	}

	private boolean hasShield() {
		return getOffhandItem().is(Items.SHIELD);
	}

	/** Melee kit for the army; nothing is droppable, so the summons can't be farmed for gear. */
	public void equipForArmy(boolean shieldBearer, NecromancerVariant variant) {
		boolean iron = this.random.nextBoolean();
		ItemStack weapon;
		if (shieldBearer || this.random.nextBoolean()) {
			weapon = new ItemStack(iron ? Items.IRON_SPEAR : Items.STONE_SPEAR);
		} else {
			weapon = new ItemStack(iron ? Items.IRON_SWORD : Items.STONE_SWORD);
		}
		setItemSlot(EquipmentSlot.MAINHAND, weapon); // also swaps the bow AI for melee
		setItemSlot(EquipmentSlot.OFFHAND, shieldBearer ? variant.makeShield(level().registryAccess()) : ItemStack.EMPTY);
		setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET)); // survives desert daylight
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F); // includes any random armor the normal skeleton setup handed out
		}
	}

	/** Same drops as a vanilla skeleton (bones, arrows) instead of needing its own table. */
	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKill) {
		dropFromLootTable(level, source, playerKill, EntityTypes.SKELETON.getDefaultLootTable().orElseThrow());
	}

	/** Holds the shield up while a target is near, lowering it briefly every few seconds to strike. */
	class RaiseShieldGoal extends Goal {
		private int ticks;

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			double range = NecromancerConfig.SHIELD_RAISE_RANGE;
			return hasShield() && target != null && target.isAlive() && distanceToSqr(target) < range * range;
		}

		@Override
		public void start() {
			ticks = 0;
		}

		@Override
		public void tick() {
			int phase = ticks++ % (NecromancerConfig.SHIELD_UP_TICKS + NecromancerConfig.SHIELD_DOWN_TICKS);
			boolean up = phase < NecromancerConfig.SHIELD_UP_TICKS;
			boolean raised = isUsingItem() && getUsedItemHand() == InteractionHand.OFF_HAND;
			if (up && !raised) startUsingItem(InteractionHand.OFF_HAND);
			if (!up && raised) stopUsingItem();
		}

		@Override
		public void stop() {
			if (isUsingItem() && getUsedItemHand() == InteractionHand.OFF_HAND) stopUsingItem();
		}
	}
}
