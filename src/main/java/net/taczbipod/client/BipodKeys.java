package net.taczbipod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.taczbipod.TaczBipod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TaczBipod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BipodKeys {
    /**
     * Optional alternative to hold-aim: press once to brace, press again to stand. Lets you brace
     * and then aim or hip-fire freely. Default B (TACZ uses C, G, H, O, R, T, V, Z). Rebind or
     * unbind in Controls; with it unbound, only hold-aim bracing is offered.
     */
    public static final KeyMapping TOGGLE_BRACE = new KeyMapping("key.taczbipod.toggle_brace",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.taczbipod");

    private BipodKeys() {}

    public static boolean isBound() {
        return !TOGGLE_BRACE.isUnbound();
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_BRACE);
    }
}
