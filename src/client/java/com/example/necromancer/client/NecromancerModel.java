package com.example.necromancer.client;

import com.example.necromancer.NecromancerMod;
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
public class NecromancerModel extends EntityModel<NecromancerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NecromancerMod.MOD_ID, "necromancer"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart robe;
	private final ModelPart cape;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart staff;
	private final ModelPart staffBody;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public NecromancerModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.robe = body.getChild("robe");
		this.cape = body.getChild("cape");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.staff = rightArm.getChild("staff");
		this.staffBody = staff.getChild("staff_body");
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
		// staff: "staff" is the grip in the hand, held out past the robe so it never cuts through the body;
		// "staff_body" sits at the shaft's midpoint (8 px above the hand) so the flourish spins it about its centre.
		// The grip is 3/4 of the way down the 32 px shaft, so little of it hangs below the hand.
		PartDefinition staff = rightArm.addOrReplaceChild("staff", CubeListBuilder.create(),
				PartPose.offset(-2.5F, 9.0F, -1.0F));
		PartDefinition staffBody = staff.addOrReplaceChild("staff_body",
				CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -16.0F, -1.0F, 2.0F, 32.0F, 2.0F)   // shaft
						.texOffs(48, 64).addBox(-1.5F, -17.0F, -1.5F, 3.0F, 1.0F, 3.0F),                  // silver collar
				PartPose.offset(0.0F, -8.0F, 0.0F));
		staffBody.addOrReplaceChild("orb",
				CubeListBuilder.create()
						.texOffs(8, 64).addBox(-2.5F, -22.0F, -1.0F, 5.0F, 5.0F, 2.0F)
						.texOffs(24, 64).addBox(-3.5F, -23.0F, -0.5F, 7.0F, 7.0F, 1.0F),
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
	public void setupAnim(NecromancerRenderState state) {
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

		// idle life: slow breathing, a little skull sway, cape and robe rippling out of step with the rest of the group
		float breathe = Mth.sin(age * 0.09F + state.phase);
		body.y += breathe * 0.3F;
		head.y += breathe * 0.3F;
		head.zRot += Mth.sin(age * 0.05F + state.phase) * 0.04F * (1.0F - speed);
		cape.xRot += Mth.sin(age * 0.07F + state.phase) * 0.04F;
		robe.zRot += Mth.sin(age * 0.11F + state.phase) * 0.02F;

		// walking stick: the arm swings with each stride while the staff stays upright, so its foot never swings into the robe;
		// the free bony arm dangles and swings
		rightArm.xRot = -0.4F - stride * 0.25F * speed;
		rightArm.zRot = 0.05F;
		leftArm.xRot = stride * 0.55F * speed - 0.05F;
		leftArm.zRot = -0.06F - Mth.cos(age * 0.09F) * 0.04F;
		staffUpright(0.0F);

		if (state.flourish > 0.0F) flourish(state.flourish);
		if (state.castBlend > 0.0F) castPose(state, age);
		if (state.hasRedOverlay) {
			// flinch: skull snaps back, shoulders recoil
			head.xRot -= 0.25F;
			body.xRot -= 0.12F;
			rightArm.xRot += 0.2F;
			leftArm.xRot += 0.3F;
			staffUpright(0.0F);
		}
	}

	/** Holds the staff vertical whatever the arm is doing, tipped {@code lean} radians forward, top slightly out from the body. */
	private void staffUpright(float lean) {
		staff.xRot = -rightArm.xRot + lean;
		staff.zRot = -rightArm.zRot - 0.08F;
	}

	/** Rare idle: raise the arm out to the side, toss the staff above the head, spin it about its middle, catch it. */
	private void flourish(float f) {
		float arc = Mth.sin(f * Mth.PI);
		float raise = Math.min(1.0F, f * 4.0F) * (f < 0.85F ? 1.0F : (1.0F - f) / 0.15F);
		rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -2.7F);
		rightArm.zRot = 0.05F + 0.35F * raise; // out to the side, clear of the skull and crown
		staffUpright(0.0F);
		// offsets are in the raised arm's frame: +y runs along the arm toward (and past) the hand, i.e. upward here.
		// 1.1.0 subtracted this, pulling the spinning staff down into the skull.
		staff.y += arc * 14.0F;
		staff.x -= arc * 3.0F;
		float spin = Mth.clamp((f - 0.15F) / 0.7F, 0.0F, 1.0F); // the staff leaves the hand before it starts to turn
		staffBody.xRot = spin * spin * (3.0F - 2.0F * spin) * Mth.TWO_PI * 2.0F;
		leftArm.xRot = -0.6F * arc;
		leftArm.zRot = -1.1F * arc;
		head.xRot -= 0.5F * arc;
	}

	/** Blends from the current pose into the spell's own pose by {@code castBlend}. */
	private void castPose(NecromancerRenderState s, float age) {
		float b = s.castBlend;
		float pulse = Mth.sin(age * 0.45F);
		float rArm, rArmZ, lArm, lArmZ, headTilt, bodyX, robeX, capeX, staffLean, staffDrop;
		switch (s.castPose) {
			case SUMMON -> {
				// both arms flung skyward, staff held high overhead, skull thrown back
				rArm = -2.9F + pulse * 0.05F;
				rArmZ = 0.35F;
				lArm = -2.9F - pulse * 0.05F;
				lArmZ = -0.35F;
				headTilt = -0.55F;
				bodyX = -0.12F;
				robeX = 0.2F + Mth.sin(age * 0.9F) * 0.06F;
				capeX = 0.28F + Mth.sin(age * 0.6F) * 0.08F;
				staffLean = 0.0F;
				staffDrop = 0.0F;
			}
			case AURA -> {
				// staff driven down into the ground in front, head bowed, free hand spread, cape flaring outward
				rArm = -1.3F;
				rArmZ = 0.1F;
				lArm = -0.9F + Mth.cos(age * 0.5F) * 0.08F;
				lArmZ = -0.7F;
				headTilt = 0.35F;
				bodyX = 0.12F;
				robeX = 0.15F + Mth.sin(age * 1.1F) * 0.06F;
				capeX = 0.32F + Mth.sin(age * 0.8F) * 0.1F;
				staffLean = 0.0F;
				staffDrop = 2.0F;
			}
			default -> {
				// fangs: draw the staff back, then thrust it at the target with the orb tipped forward
				float back = Mth.clamp(s.castTime / 8.0F, 0.0F, 1.0F);
				float thrust = Mth.clamp((s.castTime - 8.0F) / 5.0F, 0.0F, 1.0F);
				rArm = Mth.lerp(thrust, Mth.lerp(back, -0.4F, 0.6F), -2.0F + pulse * 0.1F);
				rArmZ = 0.1F;
				lArm = -1.45F + Mth.cos(age * 0.6F) * 0.12F;
				lArmZ = -0.25F - pulse * 0.08F;
				headTilt = -0.25F;
				bodyX = Mth.lerp(thrust, 0.1F, -0.06F);
				robeX = 0.15F + Mth.sin(age * 0.9F) * 0.07F;
				capeX = 0.28F + Mth.sin(age * 0.6F) * 0.1F;
				staffLean = -0.3F * thrust;
				staffDrop = 0.0F;
			}
		}
		rightArm.xRot = Mth.lerp(b, rightArm.xRot, rArm);
		rightArm.zRot = Mth.lerp(b, rightArm.zRot, rArmZ);
		leftArm.xRot = Mth.lerp(b, leftArm.xRot, lArm);
		leftArm.zRot = Mth.lerp(b, leftArm.zRot, lArmZ);
		head.xRot += headTilt * b;
		body.xRot = Mth.lerp(b, body.xRot, bodyX);
		robe.xRot = Mth.lerp(b, robe.xRot, robeX);
		cape.xRot = Mth.lerp(b, cape.xRot, capeX);
		// hovering: legs dangle and drift under the robe
		rightLeg.xRot = Mth.lerp(b, rightLeg.xRot, 0.15F + Mth.sin(age * 0.2F) * 0.08F);
		leftLeg.xRot = Mth.lerp(b, leftLeg.xRot, 0.05F - Mth.sin(age * 0.2F) * 0.08F);
		staffUpright(staffLean * b);
		staff.y += staffDrop * b;
	}
}
