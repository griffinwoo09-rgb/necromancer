package com.example.necromancer.mixin;

import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla only sets the fang warmup through its own constructor, which hard-codes the vanilla entity type. */
@Mixin(EvokerFangs.class)
public interface EvokerFangsAccessor {
	@Accessor("warmupDelayTicks")
	void necromancer$setWarmupDelayTicks(int ticks);
}
