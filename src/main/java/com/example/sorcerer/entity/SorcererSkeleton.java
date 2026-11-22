package com.example.sorcerer.entity;

import com.example.sorcerer.SorcererConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A Sorcerer's summoned melee skeleton: sword or spear, sometimes spear + a shield it actually raises.
 * Looks and drops like a vanilla skeleton; its own type so the extra goals survive save/load.
 */
public class SorcererSkeleton extends Skeleton {
	public SorcererSkeleton(EntityType<? extends SorcererSkeleton> type, Level level) {
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

	private boolean hasShield() {
		return getOffhandItem().is(Items.SHIELD);
	}

	/** Melee kit for the army; nothing is droppable, so the summons can't be farmed for gear. */
	public void equipForArmy(boolean shieldBearer) {
		boolean iron = this.random.nextBoolean();
		ItemStack weapon;
		if (shieldBearer || this.random.nextBoolean()) {
			weapon = new ItemStack(iron ? Items.IRON_SPEAR : Items.STONE_SPEAR);
		} else {
			weapon = new ItemStack(iron ? Items.IRON_SWORD : Items.STONE_SWORD);
		}
		setItemSlot(EquipmentSlot.MAINHAND, weapon); // also swaps the bow AI for melee
		setItemSlot(EquipmentSlot.OFFHAND, shieldBearer ? new ItemStack(Items.SHIELD) : ItemStack.EMPTY);
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
			double range = SorcererConfig.SHIELD_RAISE_RANGE;
			return hasShield() && target != null && target.isAlive() && distanceToSqr(target) < range * range;
		}

		@Override
		public void start() {
			ticks = 0;
		}

		@Override
		public void tick() {
			int phase = ticks++ % (SorcererConfig.SHIELD_UP_TICKS + SorcererConfig.SHIELD_DOWN_TICKS);
			boolean up = phase < SorcererConfig.SHIELD_UP_TICKS;
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
