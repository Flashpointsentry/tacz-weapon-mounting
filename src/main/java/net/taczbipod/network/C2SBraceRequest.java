package net.taczbipod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.taczbipod.server.ServerBraceManager;

import java.util.function.Supplier;

/** Client asks to brace (true) or stand up (false). The server decides. */
public record C2SBraceRequest(boolean brace) {
    public static void encode(C2SBraceRequest msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.brace);
    }

    public static C2SBraceRequest decode(FriendlyByteBuf buf) {
        return new C2SBraceRequest(buf.readBoolean());
    }

    public static void handle(C2SBraceRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (msg.brace) ServerBraceManager.tryBrace(player);
        else ServerBraceManager.unbrace(player);
    }
}
