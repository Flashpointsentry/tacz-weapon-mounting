package net.taczbipod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.client.input.AimKey;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.taczbipod.BipodClientConfig;
import net.taczbipod.BraceType;
import net.taczbipod.TaczBipod;

/** Small prompt just above the hotbar: "Hold [Right Button] or press [B] to brace", "Bracing...", "Braced • press [B] to stand". */
@Mod.EventBusSubscriber(modid = TaczBipod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BraceHudOverlay {
    /** The prompt currently shown and when it first appeared, so it can fade after a while. */
    private static String shownText = "";
    private static long shownSince;

    private BraceHudOverlay() {}

    /** 255 = fully visible, 0 = gone. Resets whenever the prompt text changes. */
    private static int alphaFor(String text) {
        long now = Util.getMillis();
        if (!text.equals(shownText)) {
            shownText = text;
            shownSince = now;
        }
        double visible = BipodClientConfig.HINT_VISIBLE_SECONDS.get();
        if (visible <= 0) return 255;
        double fade = BipodClientConfig.HINT_FADE_SECONDS.get();
        double age = (now - shownSince) / 1000.0;
        if (age <= visible) return 255;
        if (fade <= 0 || age >= visible + fade) return 0;
        return (int) (255 * (1 - (age - visible) / fade));
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "brace_hint", (gui, graphics, partialTick, width, height) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || mc.player == null || mc.screen != null) {
                shownText = "";
                return;
            }

            Component aimKey = AimKey.AIM_KEY.getTranslatedKeyMessage();
            boolean toggleBound = BipodKeys.isBound();
            Component toggleKey = BipodKeys.TOGGLE_BRACE.getTranslatedKeyMessage();
            Component text;
            if (ClientBraceState.isBraced()) {
                String type = ClientBraceState.type().name().toLowerCase(java.util.Locale.ROOT);
                if (!ClientBraceState.isSettled()) {
                    text = Component.translatable("hud.taczbipod.settling." + type);
                } else {
                    Component stop = ClientBraceEvents.isToggleMode()
                            ? Component.translatable("hud.taczbipod.stop.toggle", toggleKey)
                            : Component.translatable("hud.taczbipod.stop.aim", aimKey);
                    text = Component.translatable("hud.taczbipod.braced." + type, stop);
                }
            } else {
                BraceType available = ClientBraceEvents.availableType();
                if (available == null) {
                    // Nothing to show: forget the last prompt so it appears fresh next time
                    shownText = "";
                    return;
                }
                String key = available == BraceType.PRONE ? "hud.taczbipod.can_deploy" : "hud.taczbipod.can_brace";
                text = toggleBound
                        ? Component.translatable(key + ".both", aimKey, toggleKey)
                        : Component.translatable(key, aimKey);
            }
            int alpha = alphaFor(text.getString());
            // Minecraft treats very low text alpha as fully opaque, so stop drawing just before that
            if (alpha < 8) return;
            Font font = mc.font;
            int x = (width - font.width(text)) / 2;
            int y = height - BipodClientConfig.HINT_HEIGHT.get();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.drawString(font, text, x, y, (alpha << 24) | 0xFFFFFF, true);
            RenderSystem.disableBlend();
        });
    }
}
