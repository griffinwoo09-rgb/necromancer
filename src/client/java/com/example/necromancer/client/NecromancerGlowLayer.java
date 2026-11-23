package com.example.necromancer.client;

import com.example.necromancer.NecromancerMod;
import com.example.necromancer.entity.NecromancerVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Full-bright eyes, gems, robe drips and staff orb (like spider/enderman eyes), one glow map per variant. */
public class NecromancerGlowLayer extends RenderLayer<NecromancerRenderState, NecromancerModel> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final Map<NecromancerVariant, Identifier> GLOW = new EnumMap<>(NecromancerVariant.class);

	static {
		for (NecromancerVariant v : NecromancerVariant.values()) {
			GLOW.put(v, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "textures/entity/necromancer/" + v.getSerializedName() + "_glow.png"));
		}
	}

	public NecromancerGlowLayer(RenderLayerParent<NecromancerRenderState, NecromancerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, NecromancerRenderState state, float yRot, float xRot) {
		if (state.isInvisibleToPlayer) return;
		collector.submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW.get(state.variant)),
				FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
	}
}
