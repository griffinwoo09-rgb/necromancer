package com.example.necromancer.client;

import com.example.necromancer.NecromancerMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A cluster of jagged bone shards that burst out of the ground, rattle, and sink back: the Necromancer's take on
 * Evoker fangs. Model space: y = 24 is the ground, smaller y is up. 32x32 texture, see tools/make_textures.py.
 */
public class BoneSpikeModel extends EntityModel<NecromancerFangsRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "bone_spike"), "main");

	/** Base position and outward lean of each shard: x, z, xRot, zRot, height scale. */
	private static final float[][] SHARDS = {
			{0.0F, 0.0F, 0.0F, 0.0F, 1.0F},
			{-3.0F, 1.5F, 0.25F, 0.35F, 0.7F},
			{3.0F, 1.0F, 0.2F, -0.4F, 0.75F},
			{0.5F, -3.0F, -0.4F, 0.1F, 0.6F},
	};

	private final ModelPart[] shards = new ModelPart[SHARDS.length];

	public BoneSpikeModel(ModelPart root) {
		super(root);
		for (int i = 0; i < SHARDS.length; i++) shards[i] = root.getChild("shard" + i);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		for (int i = 0; i < SHARDS.length; i++) {
			float[] s = SHARDS[i];
			float h = 12.0F * s[4];
			root.addOrReplaceChild("shard" + i,
					CubeListBuilder.create()
							.texOffs(0, 0).addBox(-1.0F, -h, -1.0F, 2.0F, h, 2.0F)                // bone shaft, pivot at its base
							.texOffs(0, 16).addBox(-0.5F, -h - 3.0F, -0.5F, 1.0F, 3.0F, 1.0F),     // sharpened tip
					PartPose.offsetAndRotation(s[0], 24.0F, s[1], s[2], 0.0F, s[3]));
		}
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(NecromancerFangsRenderer.State state) {
		super.setupAnim(state);
		float p = state.biteProgress;                                    // vanilla fang timing: 0 -> 1 over the bite
		float rise = Math.min(1.0F, p * 3.0F);                           // erupt fast...
		float sink = p > 0.65F ? Math.max(0.0F, (1.0F - p) / 0.35F) : 1.0F; // ...hold, then withdraw
		float out = rise * sink;
		for (int i = 0; i < shards.length; i++) {
			ModelPart shard = shards[i];
			float stagger = Mth.clamp(rise * 1.3F - i * 0.1F, 0.0F, 1.0F) * sink; // the big one first, the rest a beat later
			shard.y += (1.0F - stagger) * 16.0F;                           // starts buried
			float rattle = Mth.sin(p * 45.0F + i * 1.7F) * 0.06F * out * (1.0F - p);
			shard.zRot += rattle;
			shard.xRot -= rattle * 0.5F;
		}
	}
}
