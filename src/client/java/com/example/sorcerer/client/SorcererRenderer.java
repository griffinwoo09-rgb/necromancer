package com.example.sorcerer.client;

import com.example.sorcerer.SorcererMod;
import com.example.sorcerer.entity.Sorcerer;
import com.example.sorcerer.entity.SorcererVariant;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class SorcererRenderer extends MobRenderer<Sorcerer, SorcererRenderState, SorcererModel> {
	private static final Map<SorcererVariant, Identifier> TEXTURES = new EnumMap<>(SorcererVariant.class);

	static {
		for (SorcererVariant v : SorcererVariant.values()) {
			TEXTURES.put(v, Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, "textures/entity/sorcerer/" + v.getSerializedName() + ".png"));
		}
	}

	public SorcererRenderer(EntityRendererProvider.Context context) {
		super(context, new SorcererModel(context.bakeLayer(SorcererModel.LAYER)), 0.5F);
		addLayer(new SorcererGlowLayer(this));
	}

	@Override
	public SorcererRenderState createRenderState() {
		return new SorcererRenderState();
	}

	@Override
	public void extractRenderState(Sorcerer entity, SorcererRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.variant = entity.getVariant();
		state.casting = entity.isCastingSpell();
		state.flourish = flourishProgress(entity, state);
	}

	/** Client-only flourish: roughly every 30 s while standing idle, offset per entity so a group doesn't sync up. */
	private static float flourishProgress(Sorcerer entity, SorcererRenderState state) {
		final int period = 600, length = 40;
		if (state.casting || state.walkAnimationSpeed > 0.05F || entity.isAggressive()) return 0.0F;
		float t = (state.ageInTicks + entity.getId() * 97) % period;
		return t < length ? t / length : 0.0F;
	}

	@Override
	public Identifier getTextureLocation(SorcererRenderState state) {
		return TEXTURES.get(state.variant);
	}
}
