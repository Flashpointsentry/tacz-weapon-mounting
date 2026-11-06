package net.taczbipod;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player client config: config/taczbipod-client.toml */
public final class BipodClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HINT_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue HINT_VISIBLE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HINT_FADE_SECONDS;
    public static final ForgeConfigSpec.BooleanValue MOVE_TO_DISMOUNT;
    public static final ForgeConfigSpec.DoubleValue CORNER_LEAN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        HINT_HEIGHT = b.comment("How many pixels above the bottom of the screen the brace prompt is drawn.",
                        "Default sits just above the hearts/armor and the vanilla action-bar text.")
                .defineInRange("hintHeight", 82, 20, 400);
        HINT_VISIBLE_SECONDS = b.comment("How long (seconds) each prompt stays fully visible before fading. It shows again whenever the prompt changes.",
                        "0 = never fade.")
                .defineInRange("hintVisibleSeconds", 5.0, 0.0, 60.0);
        HINT_FADE_SECONDS = b.comment("How long (seconds) the fade-out takes.")
                .defineInRange("hintFadeSeconds", 1.5, 0.0, 10.0);
        MOVE_TO_DISMOUNT = b.comment("Whether pressing a movement key (or sprint) stands you up from a mount. Keys already held when you mount are ignored until released.")
                .define("moveToDismount", true);
        CORNER_LEAN = b.comment("How far (degrees) the camera tilts out toward the open side when mounted on a corner (first person only). 0 = no tilt.")
                .defineInRange("cornerLeanDegrees", 8.0, 0.0, 20.0);
        SPEC = b.build();
    }

    private BipodClientConfig() {}
}
