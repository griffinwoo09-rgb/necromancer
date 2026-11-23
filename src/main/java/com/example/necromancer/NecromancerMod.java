package com.example.necromancer;

import com.example.necromancer.entity.NecromancerFangs;
import com.example.necromancer.entity.SoulSkull;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public class NecromancerMod implements ModInitializer {
	public static final String MOD_ID = "necromancer";

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		// staff spikes and bolts never hurt the caster's own tamed pets (vanilla only skips the caster's team)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.getDirectEntity() instanceof NecromancerFangs || source.getDirectEntity() instanceof SoulSkull)
						|| !(source.getEntity() instanceof Player player)
						|| !(entity instanceof OwnableEntity pet && player.equals(pet.getOwner())));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0 && source.getDirectEntity() instanceof NecromancerFangs fangs) {
				fangs.getVariant().applyHit(entity, fangs, fangs.getOwner());
			}
		});
	}
}
