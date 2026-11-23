package com.example.necromancer;

import com.example.necromancer.entity.NecromancerFangs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public class NecromancerMod implements ModInitializer {
	public static final String MOD_ID = "necromancer";

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0 && source.getDirectEntity() instanceof NecromancerFangs fangs) {
				fangs.getVariant().applyHit(entity, fangs, fangs.getOwner());
			}
		});
	}
}
