package com.example.necromancer.client;

import com.example.necromancer.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SkeletonRenderer;

public class NecromancerModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(NecromancerModel.LAYER, NecromancerModel::createBodyLayer);
		EntityRenderers.register(ModEntities.NECROMANCER, NecromancerRenderer::new);
		EntityRenderers.register(ModEntities.NECROMANCER_FANGS, NecromancerFangsRenderer::new);
		EntityRenderers.register(ModEntities.NECROMANCER_SKELETON, SkeletonRenderer::new);
	}
}
