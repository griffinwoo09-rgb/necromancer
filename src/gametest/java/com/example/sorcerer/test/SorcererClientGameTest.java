package com.example.sorcerer.test;

import com.example.sorcerer.ModEntities;
import com.example.sorcerer.entity.Sorcerer;
import com.example.sorcerer.entity.SorcererFangs;
import com.example.sorcerer.entity.SorcererSkeleton;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.example.sorcerer.entity.SorcererVariant;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Screenshots the four variants, their tinted fangs, and the casting pose (runClientGameTest). */
public class SorcererClientGameTest implements FabricClientGameTest {
	private static final String[] VARIANTS = {"wind", "fire", "wither", "poison"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			var server = world.getServer();
			server.runCommand("difficulty normal");
			server.runCommand("time set noon");
			server.runCommand("tp @a 0 -60 0 0 10");
			for (int i = 0; i < 4; i++) {
				int x = -3 + i * 2;
				server.runCommand("summon sorcerer:sorcerer " + x + " -60 7 {NoAI:1b,Rotation:[180f,0f],Variant:\"" + VARIANTS[i] + "\"}");
			}
			context.waitTicks(40); // let chunks render
			for (int i = 0; i < 4; i++) {
				server.runCommand("summon sorcerer:sorcerer_fangs " + (-3 + i * 2) + " -60 4 {Warmup:0,Variant:\"" + VARIANTS[i] + "\"}");
			}
			context.waitTicks(6);
			context.takeScreenshot("sorcerer_variants_and_fangs");
			server.runCommand("tp @a 0 -60 4 0 15");
			context.waitTicks(5);
			context.takeScreenshot("sorcerer_closeup");
			server.runCommand("tp @a 0 -60 11 180 15");
			context.waitTicks(5);
			context.takeScreenshot("sorcerer_back");
			server.runOnServer(s -> s.overworld().getEntities(ModEntities.SORCERER, Sorcerer::isAlive).forEach(Sorcerer::startAura));
			server.runCommand("tp @a 0 -53 -4 0 35");
			context.waitTicks(25);
			context.takeScreenshot("sorcerer_auras");
			server.runCommand("tp @a 0 -60 0 0 10");

			// casting pose: a live Sorcerer facing a survival player
			server.runCommand("kill @e[type=sorcerer:sorcerer]");
			server.runCommand("gamemode survival @a");
			server.runCommand("effect give @a resistance 60 4 true");
			server.runCommand("summon sorcerer:sorcerer 0 -60 9 {Variant:\"fire\"}");
			server.waitFor(s -> s.overworld().getEntities(ModEntities.SORCERER, e -> true).stream().anyMatch(Sorcerer::isCastingSpell), 400);
			context.waitTicks(15);
			double hover = server.computeOnServer(s -> s.overworld().getEntities(ModEntities.SORCERER, Sorcerer::isAlive).getFirst().getY()) - (-60.0);
			if (hover < 0.8) throw new AssertionError("Sorcerer should hover ~1 block while casting, but is " + hover + " above ground");
			context.takeScreenshot("sorcerer_casting");
			context.waitTicks(16);
			context.takeScreenshot("sorcerer_fang_line");

			// wind fangs must launch a real player (the server has to push the new speed to the client)
			server.runCommand("kill @e[type=sorcerer:sorcerer]");
			server.runCommand("effect clear @a");
			server.runCommand("effect give @a resistance 30 3 true"); // 80% less damage, still > 0 so the hit counts
			server.runCommand("tp @a 20 -60 20");
			context.waitTicks(30);
			double before = context.computeOnClient(mc -> mc.player.getY());
			// a /summon'ed fang has no caster and bites as plain magic, so give it a (frozen) wind Sorcerer as owner
			server.runCommand("summon sorcerer:sorcerer 26 -60 20 {NoAI:1b,Variant:\"wind\"}");
			server.runOnServer(s -> {
				var level = s.overworld();
				Sorcerer caster = level.getEntities(ModEntities.SORCERER, Sorcerer::isAlive).getFirst();
				level.addFreshEntity(new SorcererFangs(level, 20.0, -60.0, 20.0, 0.0F, 0, caster, SorcererVariant.WIND));
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
					SorcererSkeleton sk = ModEntities.SORCERER_SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
					sk.snapTo(38.5 + i * 2, -60, 44.5, 180.0F, 0.0F);
					sk.setNoAi(true);
					sk.equipForArmy(i == 2);
					if (i == 0) sk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
					if (i == 1) sk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
					if (i == 2) sk.startUsingItem(InteractionHand.OFF_HAND);
					level.addFreshEntity(sk);
				}
			});
			context.waitTicks(20);
			context.takeScreenshot("skeleton_army_gear");
		}
	}
}
