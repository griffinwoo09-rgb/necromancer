package com.example.necromancer.client;

import com.example.necromancer.NecromancerMod;
import com.example.necromancer.entity.Necromancer;
import com.example.necromancer.entity.NecromancerVariant;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class NecromancerRenderer extends MobRenderer<Necromancer, NecromancerRenderState, NecromancerModel> {
	private static final Map<NecromancerVariant, Identifier> TEXTURES = new EnumMap<>(NecromancerVariant.class);

	static {
		for (NecromancerVariant v : NecromancerVariant.values()) {
			TEXTURES.put(v, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "textures/entity/necromancer/" + v.getSerializedName() + ".png"));
		}
	}

	public NecromancerRenderer(EntityRendererProvider.Context context) {
		super(context, new NecromancerModel(context.bakeLayer(NecromancerModel.LAYER)), 0.5F);
		addLayer(new NecromancerGlowLayer(this));
	}

	@Override
	public NecromancerRenderState createRenderState() {
		return new NecromancerRenderState();
	}

	@Override
	public void extractRenderState(Necromancer entity, NecromancerRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.variant = entity.getVariant();
		state.casting = entity.isCastingSpell();
		state.flourish = flourishProgress(entity, state);
	}

	/** Client-only flourish: roughly every 30 s while standing idle, offset per entity so a group doesn't sync up. */
	private static float flourishProgress(Necromancer entity, NecromancerRenderState state) {
		final int period = 600, length = 40;
		if (state.casting || state.walkAnimationSpeed > 0.05F || entity.isAggressive()) return 0.0F;
		float t = (state.ageInTicks + entity.getId() * 97) % period;
		return t < length ? t / length : 0.0F;
	}

	@Override
	public Identifier getTextureLocation(NecromancerRenderState state) {
		return TEXTURES.get(state.variant);
	}
}
