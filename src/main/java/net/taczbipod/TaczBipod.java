package net.taczbipod;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.taczbipod.network.BipodNetwork;

@Mod(TaczBipod.MODID)
public class TaczBipod {
    public static final String MODID = "taczbipod";

    public TaczBipod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BipodConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BipodClientConfig.SPEC);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onConfigReload);
        // Game events (commands, ticking, login sync, client input) are registered via @EventBusSubscriber.
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BipodNetwork::register);
    }

    /** Editing taczbipod-server.toml while the server runs: push the new settings to everyone. */
    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != BipodConfig.SPEC) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        // The file watcher runs on its own thread; hop onto the server thread before sending
        if (server != null) server.execute(BipodNetwork::syncRulesToAll);
    }
}
