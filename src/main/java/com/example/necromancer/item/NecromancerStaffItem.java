package com.example.necromancer.item;

import com.example.necromancer.NecromancerConfig;
import com.example.necromancer.entity.NecromancerFangs;
import com.example.necromancer.entity.NecromancerVariant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A Necromancer's staff, one per variant. Right-click raises a line of bone spikes where you look; sneak + right-click
 * raises a ring of them around you. Spikes carry the variant's effect (wind knockback, fire, wither, poison, shock).
 * Balanced like a vanilla rare drop: a shared cooldown, durability per cast, weak in melee.
 */
public class NecromancerStaffItem extends Item {
	private final NecromancerVariant variant;

	public NecromancerStaffItem(NecromancerVariant variant, Properties properties) {
		super(properties);
		this.variant = variant;
	}

	public NecromancerVariant variant() {
		return variant;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			if (player.isSecondaryUseActive()) castRing(server, player);
			else castLine(server, player);
			server.playSound(null, player.getX(), player.getY(), player.getZ(), variant.castSound(), SoundSource.PLAYERS, 0.8F, 1.2F);
			stack.hurtAndBreak(1, player, hand);
		}
		player.getCooldowns().addCooldown(stack, NecromancerConfig.STAFF_COOLDOWN);
		return InteractionResult.SUCCESS;
	}

	/** A line of spikes along the player's facing, starting just in front of them. */
	public void castLine(ServerLevel level, Player player) {
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		float angle = (float) Mth.atan2(Mth.cos(yaw), -Mth.sin(yaw)); // facing as a fang angle: x = cos(a), z = sin(a)
		double minY = player.getY() - 2.0, maxY = player.getY() + 1.0;
		for (int i = 0; i < NecromancerConfig.STAFF_LINE_SPIKES; i++) {
			double dist = 1.5 + NecromancerConfig.LINE_FANG_SPACING * i;
			NecromancerFangs.place(level, player.getX() + Mth.cos(angle) * dist, player.getZ() + Mth.sin(angle) * dist, minY, maxY, angle, i, player, variant);
		}
	}

	/** A ring of spikes around the player, to clear space. */
	public void castRing(ServerLevel level, Player player) {
		double minY = player.getY() - 2.0, maxY = player.getY() + 1.0;
		for (int i = 0; i < NecromancerConfig.STAFF_RING_SPIKES; i++) {
			float a = i * Mth.TWO_PI / NecromancerConfig.STAFF_RING_SPIKES;
			NecromancerFangs.place(level, player.getX() + Mth.cos(a) * 2.0, player.getZ() + Mth.sin(a) * 2.0, minY, maxY, a, 2, player, variant);
		}
	}
}
