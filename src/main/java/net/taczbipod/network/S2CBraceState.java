package net.taczbipod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.taczbipod.BraceType;
import net.taczbipod.client.ClientBraceState;

import java.util.function.Supplier;

/**
 * Server tells the client its brace state.
 * Yaw limits are relative to centerYaw (negative = left). Pitch limits are degrees up/down from level.
 * recoilParam: corner = share of the crawl reduction, prone = extra multiplier, wall = unused.
 * wallSide: corners only, -1 = wall on the left, +1 = wall on the right.
 */
public record S2CBraceState(boolean braced, boolean settled, BraceType type, float centerYaw,
                            float yawMin, float yawMax, float pitchUp, float pitchDown, float recoilParam,
                            int wallSide) {

    public static S2CBraceState notBraced() {
        return new S2CBraceState(false, false, BraceType.WALL, 0, 0, 0, 0, 0, 1, 0);
    }

    public static void encode(S2CBraceState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.braced);
        buf.writeBoolean(msg.settled);
        buf.writeEnum(msg.type);
        buf.writeFloat(msg.centerYaw);
        buf.writeFloat(msg.yawMin);
        buf.writeFloat(msg.yawMax);
        buf.writeFloat(msg.pitchUp);
        buf.writeFloat(msg.pitchDown);
        buf.writeFloat(msg.recoilParam);
        buf.writeByte(msg.wallSide);
    }

    public static S2CBraceState decode(FriendlyByteBuf buf) {
        return new S2CBraceState(buf.readBoolean(), buf.readBoolean(), buf.readEnum(BraceType.class), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readByte());
    }

    public static void handle(S2CBraceState msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBraceState.apply(msg));
    }
}
