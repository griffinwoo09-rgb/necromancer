package com.example.sorcerer.client;

import com.example.sorcerer.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SkeletonRenderer;

public class SorcererModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(SorcererModel.LAYER, SorcererModel::createBodyLayer);
		EntityRenderers.register(ModEntities.SORCERER, SorcererRenderer::new);
		EntityRenderers.register(ModEntities.SORCERER_FANGS, SorcererFangsRenderer::new);
		EntityRenderers.register(ModEntities.SORCERER_SKELETON, SkeletonRenderer::new);
	}
}
