package net.taczbipod.client;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.taczbipod.BipodClientConfig;
import net.taczbipod.BraceLogic;
import net.taczbipod.BraceRules;
import net.taczbipod.BraceType;
import net.taczbipod.TaczBipod;
import net.taczbipod.network.BipodNetwork;
import net.taczbipod.network.C2SBraceRequest;
import org.jetbrains.annotations.Nullable;

/**
 * Bracing follows TACZ's aim (right click by default): press aim while in position to brace,
 * stop aiming to stand up. Already aiming when you reach a spot just shows the prompt.
 * Or press the optional toggle key to brace and stay braced whether or not you aim. Uses TACZ's own aim state, so rebinding aim or toggle-aim just works.
 */
@Mod.EventBusSubscriber(modid = TaczBipod.MODID, value = Dist.CLIENT)
public final class ClientBraceEvents {
    /** Extra reach on the client so the hint doesn't flicker at the edge; the server has the final say. */
    private static final double CLIENT_SLACK = 0.02;

    @Nullable
    private static BraceType availableType;
    private static boolean requestPending;
    /** Aim state last tick: bracing only starts on a fresh aim press, never by walking into position while aiming. */
    private static boolean wasAiming;
    /** How the current brace was started: by the toggle key (stays until pressed again) or by holding aim. */
    private static boolean toggleMode;
    /** Movement keys held when mounting don't count until they've been released once. */
    private static boolean movementArmed;
    /** For a short moment after mounting, the view eases into the look limits instead of snapping. */
    private static final long EASE_MS = 300;
    private static final float EASE_TIME_CONSTANT = 0.04f;
    private static long easeUntilMs;
    private static long lastFrameNs;

    private ClientBraceEvents() {}

    /** What the HUD prompt should offer, or null for no prompt. */
    @Nullable
    public static BraceType availableType() {
        return ClientBraceState.isBraced() ? null : availableType;
    }

    /** True if braced with the toggle key (release aim doesn't stand you up). */
    public static boolean isToggleMode() {
        return toggleMode;
    }

    private static boolean isAiming(LocalPlayer player) {
        return IClientPlayerGunOperator.fromLocalPlayer(player).isAim();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean aiming = isAiming(player);
        boolean aimPressed = aiming && !wasAiming;
        wasAiming = aiming;

        BraceRules rules = ClientBraceState.rules();
        BraceLogic.Spot spot = BraceLogic.findSpot(player, rules, CLIENT_SLACK);
        availableType = spot == null ? null : spot.type();

        // Toggle key: press to brace, press again to stand
        while (BipodKeys.TOGGLE_BRACE.consumeClick()) {
            if (requestPending) continue;
            if (ClientBraceState.isBraced()) {
                request(false);
            } else if (spot != null) {
                toggleMode = true;
                request(true);
            }
        }

        // Hold aim: press aim in position to brace, release to stand (ignored for toggle braces)
        if (!requestPending) {
            if (ClientBraceState.isBraced()) {
                if (!toggleMode && !aiming) request(false);
            } else if (aimPressed && spot != null) {
                toggleMode = false;
        movementArmed = false;
        easeUntilMs = 0;
                request(true);
            }
        }
        if (ClientBraceState.isBraced()) clampLook(player, false);
    }

    private static void request(boolean brace) {
        requestPending = true;
        BipodNetwork.sendToServer(new C2SBraceRequest(brace));
    }

    /** Called once when a brace begins. */
    public static void onBraceStarted() {
        easeUntilMs = Util.getMillis() + EASE_MS;
        movementArmed = false;
    }

    /** Called when the server answers (or force-unbraces us). */
    public static void onServerAnswer() {
        requestPending = false;
    }

    /** Freezes movement while braced (and holds sneak, except prone). Jump or moving stands you up. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientBraceState.isBraced()) return;
        Input input = event.getInput();
        if (input.jumping && !requestPending) request(false);
        if (BipodClientConfig.MOVE_TO_DISMOUNT.get()) {
            boolean moving = input.up || input.down || input.left || input.right
                    || Minecraft.getInstance().options.keySprint.isDown();
            if (!moving) movementArmed = true;
            else if (movementArmed && !requestPending) request(false);
        }
        if (ClientBraceState.type() != BraceType.PRONE) input.shiftKeyDown = true;
        input.jumping = false;
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
    }

    /** Mouse look is applied every frame, so clamp every frame too (prevents jitter past the edges). */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        float dt = lastFrameNs == 0 ? 0.016f : Math.min(0.1f, (now - lastFrameNs) / 1e9f);
        lastFrameNs = now;
        if (!ClientBraceState.isBraced()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (Util.getMillis() < easeUntilMs) easeLook(player, dt);
        else clampLook(player, true);
    }

    /** Right after mounting: glide the view into the limits over ~0.15s instead of snapping. */
    private static void easeLook(LocalPlayer player, float dt) {
        float center = ClientBraceState.centerYaw();
        float yawTarget = center + Mth.clamp(Mth.wrapDegrees(player.getYRot() - center),
                ClientBraceState.yawMin(), ClientBraceState.yawMax());
        float yawDelta = Mth.wrapDegrees(yawTarget - player.getYRot());
        float pitchTarget = Mth.clamp(player.getXRot(), -ClientBraceState.pitchUp(), ClientBraceState.pitchDown());
        float k = 1f - (float) Math.exp(-dt / EASE_TIME_CONSTANT);

        float yaw = player.getYRot() + yawDelta * k;
        float pitch = player.getXRot() + (pitchTarget - player.getXRot()) * k;
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
        player.xRotO = pitch;
    }

    /** Hard look limits. During the ease window the tick-rate clamp is skipped so it doesn't snap. */
    private static void clampLook(LocalPlayer player, boolean fromFrame) {
        if (!fromFrame && Util.getMillis() < easeUntilMs) return;
        float center = ClientBraceState.centerYaw();
        float min = ClientBraceState.yawMin();
        float max = ClientBraceState.yawMax();
        float clamped = center + Mth.clamp(Mth.wrapDegrees(player.getYRot() - center), min, max);
        float clampedO = center + Mth.clamp(Mth.wrapDegrees(player.yRotO - center), min, max);
        if (clamped != player.getYRot()) {
            // Keep the same winding as the current yaw so the camera doesn't spin 360°
            float fixed = player.getYRot() + Mth.wrapDegrees(clamped - player.getYRot());
            player.setYRot(fixed);
            player.setYHeadRot(fixed);
        }
        player.yRotO = player.yRotO + Mth.wrapDegrees(clampedO - player.yRotO);

        // Pitch: Minecraft uses negative = up, positive = down, 0 = level
        float minPitch = -ClientBraceState.pitchUp();
        float maxPitch = ClientBraceState.pitchDown();
        player.setXRot(Mth.clamp(player.getXRot(), minPitch, maxPitch));
        player.xRotO = Mth.clamp(player.xRotO, minPitch, maxPitch);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBraceState.reset();
        availableType = null;
        requestPending = false;
        wasAiming = false;
        toggleMode = false;
    }
}
