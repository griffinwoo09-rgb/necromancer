package com.example.necromancer;

/** Every tweakable number for the Necromancer lives here. Drop chances live in the loot table JSON. */
public final class NecromancerConfig {
	private NecromancerConfig() {}

	// Stats: a mini-boss (Evoker: 24 health, no armor, 0.5 speed, 12 follow range)
	public static final double MAX_HEALTH = 60.0;
	public static final double ARMOR = 6.0;
	public static final double KNOCKBACK_RESISTANCE = 0.3;
	public static final double MOVEMENT_SPEED = 0.45;
	public static final double FOLLOW_RANGE = 16.0;
	public static final int XP_REWARD = 25;
	/** Backs away from players closer than this, after trying its staff sweep. */
	public static final float FLEE_DISTANCE = 5.0F;

	// Soul-skull bolt: ranged attack for targets 7-16 blocks away
	public static final double SOUL_BOLT_MIN_RANGE = 7.0;
	public static final double SOUL_BOLT_MAX_RANGE = 16.0;
	public static final int SOUL_BOLT_CAST_TIME = 25;
	public static final int SOUL_BOLT_COOLDOWN = 80;
	public static final double SOUL_SKULL_SPEED = 0.6;          // blocks per tick
	public static final double SOUL_SKULL_TURN_DEGREES = 6.0;   // max homing turn per tick
	public static final int SOUL_SKULL_LIFETIME = 100;
	public static final float SOUL_SKULL_DAMAGE = 4.0F;

	// Staff sweep: melee arc for targets within 3 blocks
	public static final double SWEEP_TRIGGER_RANGE = 3.0;
	public static final double SWEEP_REACH = 3.5;
	public static final double SWEEP_ARC_DEGREES = 100.0;
	public static final int SWEEP_WINDUP = 10;
	public static final int SWEEP_LENGTH = 16;
	public static final int SWEEP_COOLDOWN = 60;
	public static final float SWEEP_DAMAGE = 6.0F;
	public static final double SWEEP_KNOCKBACK = 1.2;

	// Necromancer's Staff (player item)
	public static final int STAFF_DURABILITY = 250;
	public static final int STAFF_COOLDOWN = 40;
	public static final int STAFF_LINE_SPIKES = 8;
	public static final int STAFF_RING_SPIKES = 6;

	// Fang spell timing (ticks)
	public static final int CAST_WARMUP = 20;
	public static final int CAST_DURATION = 40;
	public static final int CAST_COOLDOWN = 100;

	/** Blocks the Necromancer rises off the ground while casting, and how fast it gets there (0-1). */
	public static final double HOVER_HEIGHT = 1.0;
	public static final double HOVER_RISE_SPEED = 0.15;

	// Fang pattern
	public static final double CLOSE_RANGE = 3.0;
	public static final int LINE_FANG_COUNT = 16;
	public static final double LINE_FANG_SPACING = 1.25;

	// Circle aura: a ring of the variant's effect around the Necromancer, cast when a target gets close
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
	/** Summoned skeletons crumble this many seconds after their Necromancer is gone (instantly if it dies nearby). */
	public static final int ORPHAN_CRUMBLE_SECONDS = 5;

	// Per-variant hit effects (fangs and aura pulses)
	public static final double WIND_KNOCKBACK_HORIZONTAL = 1.2;
	public static final double WIND_KNOCKBACK_VERTICAL = 0.6;
	public static final float FIRE_SECONDS = 5.0F;
	public static final int WITHER_TICKS = 100;
	public static final int WITHER_AMPLIFIER = 0;
	public static final int POISON_TICKS = 100;
	public static final int POISON_AMPLIFIER = 0;
	/** Lightning: extra damage on top of the fang bite or aura pulse, then a short Slowness "stun". No real bolt: no fire or mob conversions. */
	public static final float LIGHTNING_SHOCK_DAMAGE = 3.0F;
	public static final int LIGHTNING_STUN_TICKS = 30;
	public static final int LIGHTNING_STUN_AMPLIFIER = 2;        // Slowness III
	public static final int LIGHTNING_BOLT_COOLDOWN_TICKS = 10;  // at most one cosmetic bolt per target this often

	// Spawning (desert biomes, monster category)
	public static final int SPAWN_WEIGHT = 20;
	public static final int SPAWN_MIN_GROUP = 1;
	public static final int SPAWN_MAX_GROUP = 1;
}
