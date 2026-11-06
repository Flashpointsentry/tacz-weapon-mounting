package net.taczbipod.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.taczbipod.BipodConfig;
import net.taczbipod.BraceLogic;
import net.taczbipod.BraceRules;
import net.taczbipod.BraceType;
import net.taczbipod.TaczBipod;
import net.taczbipod.network.BipodNetwork;
import net.taczbipod.network.S2CBraceState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Authoritative brace state. The client only asks; the server validates every tick. */
@Mod.EventBusSubscriber(modid = TaczBipod.MODID)
public final class ServerBraceManager {
    /** How far (blocks) a braced player can drift (knockback, pistons...) before they're knocked off. */
    private static final double MAX_DRIFT_SQR = 0.35 * 0.35;

    private static final class Brace {
        final BraceLogic.Spot spot;
        final Vec3 startPos;
        final int slot;
        final ResourceLocation gunId;
        final long settleAt;
        boolean settled;

        Brace(BraceLogic.Spot spot, Vec3 startPos, int slot, ResourceLocation gunId, long settleAt) {
            this.spot = spot;
            this.startPos = startPos;
            this.slot = slot;
            this.gunId = gunId;
            this.settleAt = settleAt;
        }
    }

    private static final Map<UUID, Brace> BRACED = new HashMap<>();

    private ServerBraceManager() {}

    public static boolean isBraced(ServerPlayer player) {
        return BRACED.containsKey(player.getUUID());
    }

    /** The brace type if the player is braced AND settled (deploy delay finished), otherwise null. */
    @org.jetbrains.annotations.Nullable
    public static BraceType settledType(ServerPlayer player) {
        Brace brace = BRACED.get(player.getUUID());
        return brace != null && brace.settled ? brace.spot.type() : null;
    }

    public static void tryBrace(ServerPlayer player) {
        if (isBraced(player)) return;
        BraceRules rules = BraceRules.fromServerConfig();
        ResourceLocation gun = BraceLogic.heldGunId(player);
        BraceLogic.Spot spot = BraceLogic.findSpot(player, rules, 0);
        if (spot == null) {
            // Tell the client "no" so it doesn't wait forever
            BipodNetwork.sendTo(player, S2CBraceState.notBraced());
            return;
        }
        long settleAt = player.level().getGameTime() + BipodConfig.DEPLOY_TICKS.get();
        Brace brace = new Brace(spot, player.position(), player.getInventory().selected, gun, settleAt);
        BRACED.put(player.getUUID(), brace);
        if (BipodConfig.DEPLOY_TICKS.get() <= 0) settle(player, brace);
        else sendState(player, brace);
    }

    public static void unbrace(ServerPlayer player) {
        if (BRACED.remove(player.getUUID()) != null) BipodNetwork.sendTo(player, S2CBraceState.notBraced());
    }

    private static void settle(ServerPlayer player, Brace brace) {
        brace.settled = true;
        // Heard by everyone nearby, including the player
        float pitch = brace.spot.type() == BraceType.PRONE ? 0.7f : 0.9f;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.6f, pitch);
        sendState(player, brace);
    }

    private static void sendState(ServerPlayer player, Brace brace) {
        BraceLogic.Spot spot = brace.spot;
        float yawMin, yawMax, up, down, recoil;
        switch (spot.type()) {
            case PRONE -> {
                float arc = BipodConfig.PRONE_YAW_ARC.get().floatValue();
                yawMin = -arc;
                yawMax = arc;
                up = BipodConfig.PRONE_PITCH_UP.get().floatValue();
                down = BipodConfig.PRONE_PITCH_DOWN.get().floatValue();
                recoil = BipodConfig.PRONE_RECOIL_MULTIPLIER.get().floatValue();
            }
            case CORNER -> {
                float away = BipodConfig.YAW_ARC.get().floatValue();
                float toward = BipodConfig.CORNER_YAW_TOWARD_WALL.get().floatValue();
                // Turning left lowers yaw. Wall on the left limits the left side, and vice versa.
                yawMin = spot.wallSide() < 0 ? -toward : -away;
                yawMax = spot.wallSide() < 0 ? away : toward;
                up = BipodConfig.PITCH_UP.get().floatValue();
                down = BipodConfig.PITCH_DOWN.get().floatValue();
                recoil = BipodConfig.CORNER_RECOIL_STRENGTH.get().floatValue();
            }
            default -> {
                float arc = BipodConfig.YAW_ARC.get().floatValue();
                yawMin = -arc;
                yawMax = arc;
                up = BipodConfig.PITCH_UP.get().floatValue();
                down = BipodConfig.PITCH_DOWN.get().floatValue();
                recoil = 1f;
            }
        }
        BipodNetwork.sendTo(player, new S2CBraceState(true, brace.settled, spot.type(), spot.centerYaw(),
                yawMin, yawMax, up, down, recoil, spot.wallSide()));
    }

    /** A hard enough hit knocks you off. Only the brace ends; a prone player keeps crawling. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isBraced(player)) return;
        double threshold = BipodConfig.KNOCK_OFF_DAMAGE.get();
        if (threshold > 0 && event.getAmount() >= threshold) unbrace(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Brace brace = BRACED.get(player.getUUID());
        if (brace == null) return;
        if (!stillValid(player, brace)) {
            unbrace(player);
            return;
        }
        if (!brace.settled && player.level().getGameTime() >= brace.settleAt) settle(player, brace);
    }

    private static boolean stillValid(ServerPlayer player, Brace brace) {
        BraceRules rules = BraceRules.fromServerConfig();
        if (!BraceLogic.stillValid(player, brace.spot, rules)) return false;
        if (player.getInventory().selected != brace.slot) return false;
        // Same gun as when they braced, and it's still allowed this kind of brace (bipod not removed etc.)
        if (!brace.gunId.equals(BraceLogic.heldGunId(player))) return false;
        if (!BraceLogic.gunAllows(brace.spot.type(), BraceLogic.hasBipod(player, rules), rules)) return false;
        Vec3 now = player.position();
        double dx = now.x - brace.startPos.x, dz = now.z - brace.startPos.z;
        return dx * dx + dz * dz <= MAX_DRIFT_SQR && Math.abs(now.y - brace.startPos.y) < 0.5;
    }

    // ---- cleanup + rules sync ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BipodNetwork.syncRules(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BRACED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) unbrace(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) unbrace(player);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        BRACED.clear();
        net.taczbipod.BipodAllowlist.load();
        BraceRules.invalidate();
    }
}
