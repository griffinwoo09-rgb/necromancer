package com.example.necromancer.test;

import com.example.necromancer.ModEntities;
import com.example.necromancer.entity.Necromancer;
import com.example.necromancer.entity.NecromancerFangs;
import com.example.necromancer.entity.NecromancerSkeleton;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.example.necromancer.entity.NecromancerVariant;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Screenshots the four variants, their tinted fangs, and the casting pose (runClientGameTest). */
public class NecromancerClientGameTest implements FabricClientGameTest {
	private static final String[] VARIANTS = {"wind", "fire", "wither", "poison", "lightning"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			var server = world.getServer();
			server.runCommand("difficulty normal");
			server.runCommand("time set noon");
			server.runCommand("tp @a 0 -60 -1 0 10");
			for (int i = 0; i < VARIANTS.length; i++) {
				int x = -4 + i * 2;
				server.runCommand("summon necromancer:necromancer " + x + " -60 7 {NoAI:1b,Rotation:[180f,0f],Variant:\"" + VARIANTS[i] + "\"}");
			}
			context.waitTicks(40); // let chunks render
			for (int i = 0; i < VARIANTS.length; i++) {
				server.runCommand("summon necromancer:necromancer_fangs " + (-4 + i * 2) + " -60 4 {Warmup:0,Variant:\"" + VARIANTS[i] + "\"}");
			}
			context.waitTicks(6);
			context.takeScreenshot("necromancer_variants_and_fangs");
			server.runCommand("tp @a 0 -60 4 0 15");
			context.waitTicks(5);
			context.takeScreenshot("necromancer_closeup");
			server.runCommand("tp @a 0 -60 11 180 15");
			context.waitTicks(5);
			context.takeScreenshot("necromancer_back");
			server.runOnServer(s -> s.overworld().getEntities(ModEntities.NECROMANCER, Necromancer::isAlive).forEach(Necromancer::startAura));
			server.runCommand("tp @a 0 -53 -4 0 35");
			context.waitTicks(25);
			context.takeScreenshot("necromancer_auras");
			server.runCommand("tp @a 0 -60 0 0 10");

			// casting pose: a live Necromancer facing a survival player
			server.runCommand("kill @e[type=necromancer:necromancer]");
			server.runCommand("gamemode survival @a");
			server.runCommand("effect give @a resistance 60 4 true");
			server.runCommand("summon necromancer:necromancer 0 -60 9 {Variant:\"fire\"}");
			server.waitFor(s -> s.overworld().getEntities(ModEntities.NECROMANCER, e -> true).stream().anyMatch(Necromancer::isCastingSpell), 400);
			context.waitTicks(15);
			double hover = server.computeOnServer(s -> s.overworld().getEntities(ModEntities.NECROMANCER, Necromancer::isAlive).getFirst().getY()) - (-60.0);
			if (hover < 0.8) throw new AssertionError("Necromancer should hover ~1 block while casting, but is " + hover + " above ground");
			context.takeScreenshot("necromancer_casting");
			context.waitTicks(16);
			context.takeScreenshot("necromancer_fang_line");

			// wind fangs must launch a real player (the server has to push the new speed to the client)
			server.runCommand("kill @e[type=necromancer:necromancer]");
			server.runCommand("effect clear @a");
			server.runCommand("effect give @a resistance 30 3 true"); // 80% less damage, still > 0 so the hit counts
			server.runCommand("tp @a 20 -60 20");
			context.waitTicks(30);
			double before = context.computeOnClient(mc -> mc.player.getY());
			// a /summon'ed fang has no caster and bites as plain magic, so give it a (frozen) wind Necromancer as owner
			server.runCommand("summon necromancer:necromancer 26 -60 20 {NoAI:1b,Variant:\"wind\"}");
			server.runOnServer(s -> {
				var level = s.overworld();
				Necromancer caster = level.getEntities(ModEntities.NECROMANCER, Necromancer::isAlive).getFirst();
				level.addFreshEntity(new NecromancerFangs(level, 20.0, -60.0, 20.0, 0.0F, 0, caster, NecromancerVariant.WIND));
			});
			context.waitTicks(12);
			double peak = context.computeOnClient(mc -> mc.player.getY());
			if (peak - before < 0.5) throw new AssertionError("wind fangs did not launch the player (rose " + (peak - before) + ")");

			// the skeleton army's gear: sword, spear, and a spear + raised shield
			server.runCommand("kill @e[type=!player]");
			server.runCommand("gamemode creative @a");
			server.runCommand("tp @a 40 -60 40 0 5");
			server.runOnServer(s -> {
				var level = s.overworld();
				for (int i = 0; i < 3; i++) {
					NecromancerSkeleton sk = ModEntities.NECROMANCER_SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
					sk.snapTo(38.5 + i * 2, -60, 44.5, 180.0F, 0.0F);
					sk.setNoAi(true);
					sk.equipForArmy(i == 2, NecromancerVariant.FIRE);
					if (i == 0) sk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
					if (i == 1) sk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
					if (i == 2) sk.startUsingItem(InteractionHand.OFF_HAND);
					level.addFreshEntity(sk);
				}
			});
			context.waitTicks(20);
			context.takeScreenshot("skeleton_army_gear");

			// every variant's banner shield, face-on in item frames on a wall
			server.runCommand("kill @e[type=!player]");
			server.runCommand("fill 56 -60 66 64 -57 66 minecraft:stone");
			server.runCommand("tp @a 60 -59 61 0 5");
			server.runOnServer(s -> {
				var level = s.overworld();
				NecromancerVariant[] all = NecromancerVariant.values();
				for (int i = 0; i < all.length; i++) {
					var frame = new net.minecraft.world.entity.decoration.ItemFrame(level,
							new net.minecraft.core.BlockPos(56 + i * 2, -59, 65), net.minecraft.core.Direction.NORTH);
					frame.setItem(all[i].makeShield(level.registryAccess()));
					level.addFreshEntity(frame);
				}
			});
			context.waitTicks(20);
			context.takeScreenshot("variant_banner_shields");

			// staff clearance: one frozen Necromancer per pose, seen side-on from its staff hand, then from the other side
			server.runCommand("kill @e[type=!player]");
			Necromancer.CastPose[] poses = Necromancer.CastPose.values();
			server.runOnServer(s -> {
				var level = s.overworld();
				for (int i = 0; i < poses.length; i++) {
					Necromancer n = ModEntities.NECROMANCER.create(level, EntitySpawnReason.COMMAND);
					n.snapTo(77.5 + i * 3, -60, 80.5, 270.0F, 0.0F); // facing +x, so its right (staff) hand faces -z
					n.setYHeadRot(270.0F);
					n.setYBodyRot(270.0F);
					n.setNoAi(true);
					n.setVariant(NecromancerVariant.values()[i % NecromancerVariant.values().length]);
					n.holdCastPose(poses[i]);
					level.addFreshEntity(n);
				}
			});
			server.runCommand("tp @a 82 -59.5 74.5 0 5");
			context.waitTicks(30); // poses ease in over ~5 ticks; the wind-up finishes by ~13
			context.takeScreenshot("staff_poses_staff_side");
			server.runCommand("tp @a 82 -59.5 86.5 180 5");
			context.waitTicks(10);
			context.takeScreenshot("staff_poses_other_side");

			// lightning shock on a pig: cosmetic bolt, sparks
			server.runCommand("kill @e[type=!player]");
			server.runCommand("summon necromancer:necromancer 106 -60 100 {NoAI:1b,Variant:\"lightning\"}");
			server.runCommand("summon minecraft:pig 100 -60 104 {NoAI:1b}");
			server.runCommand("tp @a 100 -59 97 0 5");
			context.waitTicks(20);
			server.runOnServer(s -> {
				var level = s.overworld();
				Necromancer caster = level.getEntities(ModEntities.NECROMANCER, Necromancer::isAlive).getFirst();
				level.addFreshEntity(new NecromancerFangs(level, 100.5, -60.0, 104.5, 0.0F, 0, caster, NecromancerVariant.LIGHTNING));
			});
			context.waitTicks(5);
			context.takeScreenshot("lightning_shock");
		}
	}
}
