package net.taczbipod.client;

import net.taczbipod.BraceRules;
import net.taczbipod.BraceType;
import net.taczbipod.network.S2CBraceState;

/** Client mirror of what the server told us. Only touched on the client main thread. */
public final class ClientBraceState {
    private static S2CBraceState state = S2CBraceState.notBraced();
    private static BraceRules rules = BraceRules.NONE;

    private ClientBraceState() {}

    public static boolean isBraced() { return state.braced(); }
    /** Settled = deploy delay finished; the recoil bonus only applies once settled. */
    public static boolean isSettled() { return state.braced() && state.settled(); }
    public static BraceType type() { return state.type(); }
    public static float centerYaw() { return state.centerYaw(); }
    public static float yawMin() { return state.yawMin(); }
    public static float yawMax() { return state.yawMax(); }
    public static float pitchUp() { return state.pitchUp(); }
    public static float pitchDown() { return state.pitchDown(); }
    public static float recoilParam() { return state.recoilParam(); }
    public static int wallSide() { return state.wallSide(); }
    public static BraceRules rules() { return rules; }

    public static void apply(S2CBraceState newState) {
        boolean wasBraced = state.braced();
        state = newState;
        if (!wasBraced && newState.braced()) ClientBraceEvents.onBraceStarted();
        ClientBraceEvents.onServerAnswer();
    }

    public static void setRules(BraceRules newRules) {
        rules = newRules;
    }

    public static void reset() {
        state = S2CBraceState.notBraced();
        rules = BraceRules.NONE;
    }
}
