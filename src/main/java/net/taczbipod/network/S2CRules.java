package net.taczbipod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.taczbipod.BraceRules;
import net.taczbipod.client.ClientBraceState;

import java.util.function.Supplier;

/** Sends the allowlists and brace settings to clients so they know when to show the prompt. */
public record S2CRules(BraceRules rules) {
    public static void encode(S2CRules msg, FriendlyByteBuf buf) {
        msg.rules.write(buf);
    }

    public static S2CRules decode(FriendlyByteBuf buf) {
        return new S2CRules(BraceRules.read(buf));
    }

    public static void handle(S2CRules msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBraceState.setRules(msg.rules));
    }
}
