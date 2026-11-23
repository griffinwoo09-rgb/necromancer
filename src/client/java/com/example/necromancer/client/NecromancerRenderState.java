package com.example.necromancer.client;

import com.example.necromancer.entity.Necromancer;
import com.example.necromancer.entity.NecromancerVariant;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class NecromancerRenderState extends LivingEntityRenderState {
	public NecromancerVariant variant = NecromancerVariant.WIND;
	public boolean casting;
	/** 0 when idle normally; 0..1 progress through the rare staff-toss idle. */
	public float flourish;
	/** 0..1 eased blend from the walk/idle pose into the cast pose. */
	public float castBlend;
	public Necromancer.CastPose castPose = Necromancer.CastPose.NONE;
	/** Ticks into the current cast, with partial tick, for the fang wind-up. */
	public float castTime;
	/** Per-entity phase so a group's idle sway doesn't move in lockstep. */
	public float phase;
}
