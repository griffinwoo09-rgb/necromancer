package com.example.necromancer.client;

import com.example.necromancer.NecromancerMod;
import com.example.necromancer.entity.NecromancerVariant;
import com.example.necromancer.entity.SoulSkull;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** A smaller copy of the caster's own skull (same skin and glowing eyes), tumbling slightly as it flies. */
public class SoulSkullRenderer extends EntityRenderer<SoulSkull, SoulSkullRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "soul_skull"), "main");
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final Map<NecromancerVariant, Identifier> SKIN = new EnumMap<>(NecromancerVariant.class);
	private static final Map<NecromancerVariant, Identifier> GLOW = new EnumMap<>(NecromancerVariant.class);

	static {
		for (NecromancerVariant v : NecromancerVariant.values()) {
			String base = "textures/entity/necromancer/" + v.getSerializedName();
			SKIN.put(v, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, base + ".png"));
			GLOW.put(v, Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, base + "_glow.png"));
		}
	}

	public static class State extends EntityRenderState {
		public NecromancerVariant variant = NecromancerVariant.WITHER;
		public float yRot, xRot;
	}

	/** The Necromancer's 8x8x8 skull box (texOffs 0,0 on the 128x128 skin), centred on the origin. */
	static class SkullModel extends Model<State> {
		SkullModel(ModelPart root) {
			super(root, RenderTypes::entityCutout);
		}

		static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			mesh.getRoot().addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
			return LayerDefinition.create(mesh, 128, 128);
		}
	}

	private final SkullModel model;

	public SoulSkullRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new SkullModel(context.bakeLayer(LAYER));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SoulSkull skull, State state, float partialTick) {
		super.extractRenderState(skull, state, partialTick);
		state.variant = skull.getVariant();
		state.yRot = skull.getYRot(partialTick);
		state.xRot = skull.getXRot(partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.0F, 0.15F, 0.0F);
		pose.rotateDegrees(Axis.YP, 180.0F - state.yRot);
		pose.rotateDegrees(Axis.XP, state.xRot + Mth.sin(state.ageInTicks * 0.6F) * 8.0F);
		pose.scale(-0.75F, -0.75F, 0.75F); // three quarters of the Necromancer's skull: readable at range
		collector.submitModel(model, state, pose, RenderTypes.entityCutout(SKIN.get(state.variant)), state.lightCoords,
				OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
		collector.submitModel(model, state, pose, RenderTypes.eyes(GLOW.get(state.variant)), FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
