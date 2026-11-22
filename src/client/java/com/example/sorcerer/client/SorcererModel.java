package com.example.sorcerer.client;

import com.example.sorcerer.SorcererMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Crowned skeleton necromancer: skull under a hood, open ribcage around a spine, bone limbs under sleeves and
 * pauldrons, a belted tattered robe, a cape and a glowing staff.
 * 128x128 texture; UV offsets match tools/make_textures.py (keep the two in sync).
 */
public class SorcererModel extends EntityModel<SorcererRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SorcererMod.MOD_ID, "sorcerer"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart robe;
	private final ModelPart cape;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart staff;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public SorcererModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.robe = body.getChild("robe");
		this.cape = body.getChild("cape");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.staff = rightArm.getChild("staff");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// skull with a dark hood over it (front of the hood is open) and a spiked crown
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("hood",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.6F)),
				PartPose.ZERO);
		PartDefinition crown = head.addOrReplaceChild("crown",
				CubeListBuilder.create().texOffs(64, 0).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 2.0F, 10.0F),
				PartPose.ZERO);
		float[][] spikes = {{-5, -5}, {4, -5}, {-5, 4}, {4, 4}, {-5, -0.5F}, {4, -0.5F}, {-0.5F, 4}};
		CubeListBuilder spikeCubes = CubeListBuilder.create();
		for (float[] p : spikes) spikeCubes.texOffs(104, 0).addBox(p[0], -13.0F, p[1], 1.0F, 3.0F, 1.0F);
		spikeCubes.texOffs(110, 0).addBox(-1.0F, -14.0F, -5.5F, 2.0F, 4.0F, 1.0F);
		crown.addOrReplaceChild("spikes", spikeCubes, PartPose.ZERO);

		// torso: open ribcage (gaps are see-through) around a spine, silver belt as the pelvis
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.5F, -2.5F, 8.0F, 8.0F, 5.0F),
				PartPose.ZERO);
		body.addOrReplaceChild("spine",
				CubeListBuilder.create().texOffs(28, 16).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 12.0F, 2.0F),
				PartPose.ZERO);
		body.addOrReplaceChild("belt",
				CubeListBuilder.create().texOffs(44, 16).addBox(-4.5F, 9.5F, -3.0F, 9.0F, 3.0F, 6.0F),
				PartPose.ZERO);
		body.addOrReplaceChild("robe",
				CubeListBuilder.create().texOffs(0, 34).addBox(-4.5F, 0.0F, -3.0F, 9.0F, 13.0F, 6.0F, new CubeDeformation(0.3F)),
				PartPose.offset(0.0F, 12.0F, 0.0F));
		body.addOrReplaceChild("cape",
				CubeListBuilder.create().texOffs(32, 34).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 22.0F, 1.0F),
				PartPose.offset(0.0F, 0.5F, 3.0F));

		// arms: thin bones under a short sleeve and a big pauldron; the staff hangs off the right hand
		PartDefinition rightArm = root.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(56, 34).addBox(-2.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
						.texOffs(98, 34).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F),
				PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("right_pauldron",
				CubeListBuilder.create().texOffs(72, 34).addBox(-4.5F, -3.5F, -3.5F, 6.0F, 5.0F, 7.0F),
				PartPose.ZERO);
		PartDefinition staff = rightArm.addOrReplaceChild("staff",
				CubeListBuilder.create().texOffs(0, 64).addBox(-0.5F, -18.0F, -0.5F, 1.0F, 32.0F, 1.0F),
				PartPose.offset(-1.0F, 9.0F, -1.0F));
		staff.addOrReplaceChild("orb",
				CubeListBuilder.create()
						.texOffs(8, 64).addBox(-2.5F, -23.0F, -1.0F, 5.0F, 5.0F, 2.0F)
						.texOffs(24, 64).addBox(-3.5F, -24.0F, -0.5F, 7.0F, 7.0F, 1.0F),
				PartPose.ZERO);
		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(56, 34).mirror().addBox(0.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
						.texOffs(98, 34).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F),
				PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("left_pauldron",
				CubeListBuilder.create().texOffs(72, 34).mirror().addBox(-1.5F, -3.5F, -3.5F, 6.0F, 5.0F, 7.0F),
				PartPose.ZERO);

		// bone legs, mostly hidden by the robe
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(56, 50).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(56, 50).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				PartPose.offset(2.0F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(SorcererRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0F);
		float stride = Mth.cos(pos * 0.6662F);
		float step = Mth.sin(pos * 0.6662F);

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// skeletal walk: stiff short steps, hunched forward, a hitch at each footfall, skull nodding with the gait
		rightLeg.xRot = stride * 0.9F * speed;
		leftLeg.xRot = -stride * 0.9F * speed;
		body.xRot = 0.12F * speed;
		head.xRot += 0.1F * speed + Mth.abs(step) * 0.08F * speed;
		head.zRot = step * 0.06F * speed;
		body.zRot = -step * 0.03F * speed;
		robe.xRot = -0.04F * speed + Mth.abs(stride) * 0.08F * speed;
		robe.zRot = step * 0.05F * speed;
		cape.xRot = 0.08F + speed * 0.5F + Mth.sin(age * 0.12F) * 0.03F;
		float hitch = Mth.abs(step) * 0.7F * speed + Mth.sin(age * 0.08F) * 0.2F;
		head.y -= hitch;
		body.y -= hitch;
		rightArm.y -= hitch;
		leftArm.y -= hitch;

		if (state.casting) {
			// staff thrust up and forward, bony hand reaching at the target, cape and robe whipping in the spell wind
			float pulse = Mth.sin(age * 0.45F);
			rightArm.xRot = -2.0F + pulse * 0.1F;
			rightArm.zRot = 0.1F;
			staff.xRot = 1.7F - pulse * 0.1F;
			leftArm.xRot = -1.45F + Mth.cos(age * 0.6F) * 0.12F;
			leftArm.zRot = -0.25F - pulse * 0.08F;
			head.xRot -= 0.25F;
			body.xRot = -0.06F;
			robe.xRot = 0.15F + Mth.sin(age * 0.9F) * 0.07F;
			cape.xRot = 0.35F + Mth.sin(age * 0.6F) * 0.15F;
			rightLeg.xRot = 0.15F + Mth.sin(age * 0.2F) * 0.05F;
			leftLeg.xRot = 0.05F - Mth.sin(age * 0.2F) * 0.05F;
		} else if (state.flourish > 0.0F) {
			// rare idle: toss the staff up, let it spin, catch it and strike a pose
			float f = state.flourish;              // 0 -> 1 over the flourish
			float arc = Mth.sin(f * Mth.PI);       // up then back down
			rightArm.xRot = -2.6F * Math.min(1.0F, f * 4.0F) * (f < 0.85F ? 1.0F : (1.0F - f) / 0.15F);
			staff.y -= arc * 14.0F;
			staff.xRot = f * Mth.TWO_PI * 2.0F;
			leftArm.xRot = -0.6F * arc;
			leftArm.zRot = -1.1F * arc;
			head.xRot -= 0.5F * arc;
		} else {
			// staff planted like a walking stick, swung forward with each stride; free bony arm dangles and swings
			rightArm.xRot = -0.4F - stride * 0.25F * speed;
			rightArm.zRot = 0.05F;
			staff.xRot = 0.4F + stride * 0.25F * speed;
			leftArm.xRot = stride * 0.55F * speed - 0.05F;
			leftArm.zRot = -0.06F - Mth.cos(age * 0.09F) * 0.04F;
		}
	}
}
