package com.example.sorcerer;

/** Every tweakable number for the Sorcerer lives here. Drop chances live in the loot table JSON. */
public final class SorcererConfig {
	private SorcererConfig() {}

	// Stats (Evoker: 24 health, 0.5 speed, 12 follow range)
	public static final double MAX_HEALTH = 24.0;
	public static final double MOVEMENT_SPEED = 0.45;
	public static final double FOLLOW_RANGE = 16.0;
	public static final int XP_REWARD = 10;
	/** Flees players closer than this many blocks, like the Evoker. */
	public static final float FLEE_DISTANCE = 8.0F;

	// Fang spell timing (ticks)
	public static final int CAST_WARMUP = 20;
	public static final int CAST_DURATION = 40;
	public static final int CAST_COOLDOWN = 100;

	/** Blocks the Sorcerer rises off the ground while casting, and how fast it gets there (0-1). */
	public static final double HOVER_HEIGHT = 1.0;
	public static final double HOVER_RISE_SPEED = 0.15;

	// Fang pattern
	public static final double CLOSE_RANGE = 3.0;
	public static final int LINE_FANG_COUNT = 16;
	public static final double LINE_FANG_SPACING = 1.25;

	// Circle aura: a ring of the variant's effect around the Sorcerer, cast when a target gets close
	public static final double AURA_TRIGGER_RANGE = 5.0;
	public static final double AURA_RADIUS = 4.0;
	public static final int AURA_DURATION = 100;        // ticks the ring stays up
	public static final int AURA_PULSE_INTERVAL = 20;   // ticks between effect pulses inside the ring
	public static final int AURA_COOLDOWN = 240;

	// Skeleton army summon
	public static final float SKELETON_SUMMON_CHANCE = 0.3F; // rolled once per cooldown window
	public static final int SKELETON_SUMMON_COOLDOWN = 300;
	public static final int SKELETON_ARMY_SIZE = 3;
	public static final int MAX_SKELETONS_NEARBY = 6;         // counted within 16 blocks
	/** Chance each summon includes one spear + shield skeleton; the rest get a sword or a spear. */
	public static final float SHIELD_BEARER_CHANCE = 0.35F;
	public static final double SHIELD_RAISE_RANGE = 8.0;
	public static final int SHIELD_UP_TICKS = 50;   // shield held up...
	public static final int SHIELD_DOWN_TICKS = 20; // ...then lowered this long to attack

	// Per-variant hit effects (fangs and aura pulses)
	public static final double WIND_KNOCKBACK_HORIZONTAL = 1.2;
	public static final double WIND_KNOCKBACK_VERTICAL = 0.6;
	public static final float FIRE_SECONDS = 5.0F;
	public static final int WITHER_TICKS = 100;
	public static final int WITHER_AMPLIFIER = 0;
	public static final int POISON_TICKS = 100;
	public static final int POISON_AMPLIFIER = 0;

	// Spawning (desert biomes, monster category)
	public static final int SPAWN_WEIGHT = 20;
	public static final int SPAWN_MIN_GROUP = 1;
	public static final int SPAWN_MAX_GROUP = 1;
}
