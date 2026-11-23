package com.example.necromancer.client;

import com.example.necromancer.entity.NecromancerVariant;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class NecromancerRenderState extends LivingEntityRenderState {
	public NecromancerVariant variant = NecromancerVariant.WIND;
	public boolean casting;
	/** 0 when idle normally; 0..1 progress through the rare staff-toss idle. */
	public float flourish;
}
