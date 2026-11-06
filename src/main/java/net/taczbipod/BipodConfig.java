package net.taczbipod;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-world server config: saves/<world>/serverconfig/taczbipod-server.toml */
public final class BipodConfig {
    public static final ForgeConfigSpec SPEC;

    // general
    public static final ForgeConfigSpec.IntValue DEPLOY_TICKS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_FENCES_AND_WALLS;
    public static final ForgeConfigSpec.DoubleValue MAX_FACING_ANGLE;
    public static final ForgeConfigSpec.DoubleValue KNOCK_OFF_DAMAGE;

    // resting on top of a block / fence / wall
    public static final ForgeConfigSpec.DoubleValue YAW_ARC;
    public static final ForgeConfigSpec.DoubleValue PITCH_UP;
    public static final ForgeConfigSpec.DoubleValue PITCH_DOWN;
    public static final ForgeConfigSpec.DoubleValue MAX_GAP;
    public static final ForgeConfigSpec.BooleanValue WALL_HIPFIRE;

    // leaning on a wall corner
    public static final ForgeConfigSpec.BooleanValue ALLOW_CORNERS;
    public static final ForgeConfigSpec.BooleanValue CORNER_ANY_GUN;
    public static final ForgeConfigSpec.DoubleValue CORNER_EDGE_REACH;
    public static final ForgeConfigSpec.DoubleValue CORNER_YAW_TOWARD_WALL;
    public static final ForgeConfigSpec.DoubleValue CORNER_RECOIL_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue CORNER_HIPFIRE;

    // prone bipod
    public static final ForgeConfigSpec.BooleanValue ALLOW_PRONE;
    public static final ForgeConfigSpec.DoubleValue PRONE_YAW_ARC;
    public static final ForgeConfigSpec.DoubleValue PRONE_PITCH_UP;
    public static final ForgeConfigSpec.DoubleValue PRONE_PITCH_DOWN;
    public static final ForgeConfigSpec.DoubleValue PRONE_RECOIL_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        DEPLOY_TICKS = b.comment("How long (ticks, 20 = 1 second) it takes to settle into a brace before the recoil bonus kicks in.")
                .defineInRange("deployTicks", 10, 0, 100);
        ALLOW_FENCES_AND_WALLS = b.comment("Whether fences, walls (cobblestone wall etc.) and closed fence gates count as brace surfaces.",
                        "Uses the vanilla fence/wall/fence gate tags, so most modded fences and walls work too.")
                .define("allowFencesAndWalls", true);
        MAX_FACING_ANGLE = b.comment("How far (degrees) the player's view can point away from the block (or, for corners, from straight ahead) and still brace.")
                .defineInRange("maxFacingAngle", 35.0, 5.0, 60.0);
        KNOCK_OFF_DAMAGE = b.comment("Damage from a single hit (after armor; 2 = one heart) that knocks a player off their brace. 0 = never.",
                        "Prone players stay crawling; only the bipod folds.")
                .defineInRange("knockOffDamage", 4.0, 0.0, 100.0);
        b.pop();

        b.push("wall");
        YAW_ARC = b.comment("How far (degrees) a braced player can turn left or right.")
                .defineInRange("yawArc", 45.0, 5.0, 90.0);
        PITCH_UP = b.comment("How far (degrees) a braced player can look up from level.")
                .defineInRange("pitchUp", 30.0, 0.0, 90.0);
        PITCH_DOWN = b.comment("How far (degrees) a braced player can look down from level.")
                .defineInRange("pitchDown", 20.0, 0.0, 90.0);
        MAX_GAP = b.comment("Largest gap (blocks) allowed between the player's body and the block they rest the gun on. 0 = must touch it.")
                .defineInRange("maxGap", 0.2, 0.0, 1.0);
        WALL_HIPFIRE = b.comment("Whether hip-firing while braced on top uses the gun's lying-down (prone) spread instead of its crouching spread.",
                        "Aiming down sights is unaffected; it's already the most accurate.")
                .define("hipfireAccuracy", true);
        b.pop();

        b.push("corner");
        ALLOW_CORNERS = b.comment("Whether players can lean the gun against the corner of a wall (the end of a wall, a pillar or a door frame).")
                .define("allowCorners", true);
        CORNER_ANY_GUN = b.comment("Whether every TACZ gun can lean on corners. If false, only guns with a bipod (the /bipod lists) can.",
                        "Mounting on top of cover and prone always needs a bipod.")
                .define("anyGun", true);
        CORNER_EDGE_REACH = b.comment("How close (blocks) the center of the player must be to the corner's edge. 0.3 = touching it.")
                .defineInRange("edgeReach", 0.35, 0.3, 1.5);
        CORNER_YAW_TOWARD_WALL = b.comment("How far (degrees) a corner-braced player can turn toward the wall they're leaning on.",
                        "Turning away from the wall uses wall.yawArc. Look up/down uses the wall limits.")
                .defineInRange("yawTowardWall", 12.0, 0.0, 90.0);
        CORNER_RECOIL_STRENGTH = b.comment("How much of the normal brace recoil reduction a corner gives. 1.0 = same as resting on top, 0.5 = half as much, 0 = none.",
                        "Example: a gun with crawl_recoil_multiplier 0.5 gets 0.5x recoil on top of a wall and 0.75x on a corner at 0.5.")
                .defineInRange("recoilStrength", 0.5, 0.0, 1.0);
        CORNER_HIPFIRE = b.comment("Whether hip-firing while braced on a corner uses the gun's lying-down spread. Off by default: corners keep normal crouching spread.")
                .define("hipfireAccuracy", false);
        b.pop();

        b.push("prone");
        ALLOW_PRONE = b.comment("Whether players can deploy the bipod while crawling (TACZ crawl) on flat ground.")
                .define("allowProne", true);
        PRONE_YAW_ARC = b.comment("How far (degrees) a prone player can turn left or right after deploying.")
                .defineInRange("yawArc", 30.0, 5.0, 90.0);
        PRONE_PITCH_UP = b.comment("How far (degrees) a prone player can look up from level.")
                .defineInRange("pitchUp", 15.0, 0.0, 90.0);
        PRONE_PITCH_DOWN = b.comment("How far (degrees) a prone player can look down from level.")
                .defineInRange("pitchDown", 15.0, 0.0, 90.0);
        PRONE_RECOIL_MULTIPLIER = b.comment("Extra recoil multiplier stacked on top of TACZ's crawl recoil when the bipod is deployed prone.",
                        "Example: crawl_recoil_multiplier 0.5 with this at 0.7 gives 0.35x recoil.")
                .defineInRange("recoilMultiplier", 0.7, 0.0, 1.0);
        b.pop();

        SPEC = b.build();
    }

    private BipodConfig() {}
}
