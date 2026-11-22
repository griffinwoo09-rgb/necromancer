package com.example.sorcerer.client;

import com.example.sorcerer.SorcererMod;
import com.example.sorcerer.entity.SorcererVariant;
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
public class SorcererGlowLayer extends RenderLayer<SorcererRenderState, SorcererModel> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final Map<SorcererVariant, Identifier> GLOW = new EnumMap<>(SorcererVariant.class);

	static {
		for (SorcererVariant v : SorcererVariant.values()) {
			GLOW.put(v, Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, "textures/entity/sorcerer/" + v.getSerializedName() + "_glow.png"));
		}
	}

	public SorcererGlowLayer(RenderLayerParent<SorcererRenderState, SorcererModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SorcererRenderState state, float yRot, float xRot) {
		if (state.isInvisibleToPlayer) return;
		collector.submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW.get(state.variant)),
				FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
	}
}
