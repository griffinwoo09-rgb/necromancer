package com.example.sorcerer;

import com.example.sorcerer.entity.SorcererFangs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public class SorcererMod implements ModInitializer {
	public static final String MOD_ID = "sorcerer";

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!blocked && damageTaken > 0 && source.getDirectEntity() instanceof SorcererFangs fangs) {
				fangs.getVariant().applyHit(entity, fangs, fangs.getOwner());
			}
		});
	}
}
