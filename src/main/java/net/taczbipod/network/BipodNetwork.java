package net.taczbipod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.taczbipod.BraceRules;
import net.taczbipod.TaczBipod;

public final class BipodNetwork {
    private static final String VERSION = "7";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TaczBipod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private BipodNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(C2SBraceRequest.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBraceRequest::encode).decoder(C2SBraceRequest::decode)
                .consumerMainThread(C2SBraceRequest::handle).add();
        CHANNEL.messageBuilder(S2CBraceState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBraceState::encode).decoder(S2CBraceState::decode)
                .consumerMainThread(S2CBraceState::handle).add();
        CHANNEL.messageBuilder(S2CRules.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRules::encode).decoder(S2CRules::decode)
                .consumerMainThread(S2CRules::handle).add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void syncRules(ServerPlayer player) {
        sendTo(player, new S2CRules(BraceRules.fromServerConfig()));
    }

    /** Call after the allowlist or server config changes. Safe to call when no server is running. */
    public static void syncRulesToAll() {
        BraceRules.invalidate();
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CRules(BraceRules.fromServerConfig()));
    }
}
