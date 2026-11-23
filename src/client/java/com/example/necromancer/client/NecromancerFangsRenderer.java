package com.example.necromancer.client;

import com.example.necromancer.NecromancerMod;
import com.example.necromancer.entity.NecromancerFangs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Bone spikes: bone-coloured shards with full-bright tips glowing in the variant's colour. */
public class NecromancerFangsRenderer extends EntityRenderer<NecromancerFangs, NecromancerFangsRenderer.State> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "textures/entity/bone_spike.png");
	private static final Identifier GLOW = Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "textures/entity/bone_spike_glow.png");
	private static final int FULL_BRIGHT = 0xF000F0;
	private final BoneSpikeModel model;

	public static class State extends EvokerFangsRenderState {
		public int tint = 0xFFFFFFFF;
	}

	public NecromancerFangsRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new BoneSpikeModel(context.bakeLayer(BoneSpikeModel.LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(NecromancerFangs fangs, State state, float partialTick) {
		super.extractRenderState(fangs, state, partialTick);
		state.yRot = fangs.getYRot();
		state.biteProgress = fangs.getAnimationProgress(partialTick);
		state.tint = fangs.getVariant().fangTint();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.biteProgress == 0.0F) return;
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 90.0F - state.yRot);
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(model, state, pose, model.renderType(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
		collector.submitModel(model, state, pose, RenderTypes.eyes(GLOW), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.tint, null, state.outlineColor);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
