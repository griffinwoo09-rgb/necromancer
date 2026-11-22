package com.example.sorcerer.client;

import com.example.sorcerer.entity.SorcererVariant;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class SorcererRenderState extends LivingEntityRenderState {
	public SorcererVariant variant = SorcererVariant.WIND;
	public boolean casting;
	/** 0 when idle normally; 0..1 progress through the rare staff-toss idle. */
	public float flourish;
}
