package net.taczbipod.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.taczbipod.BipodClientConfig;
import net.taczbipod.BraceType;
import net.taczbipod.TaczBipod;

/**
 * Corner mounts tilt the first-person view out toward the open side, like leaning out of cover.
 * Purely visual: the player, their hitbox and where bullets come from don't move.
 */
@Mod.EventBusSubscriber(modid = TaczBipod.MODID, value = Dist.CLIENT)
public final class CornerLeanCamera {
    private static final float TIME_CONSTANT = 0.08f;
    private static float roll;
    private static long lastNs;

    private CornerLeanCamera() {}

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;

        float target = 0f;
        Minecraft mc = Minecraft.getInstance();
        if (ClientBraceState.isBraced() && ClientBraceState.type() == BraceType.CORNER
                && mc.options.getCameraType().isFirstPerson()) {
            // Positive roll leans right. Wall on the left (-1) -> lean right, and vice versa.
            target = -ClientBraceState.wallSide() * BipodClientConfig.CORNER_LEAN.get().floatValue();
        }
        roll += (target - roll) * (1f - (float) Math.exp(-dt / TIME_CONSTANT));
        if (Math.abs(roll) > 0.01f) event.setRoll(event.getRoll() + roll);
    }
}
