package net.taczbipod.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.world.entity.Pose;
import net.taczbipod.BraceType;
import net.taczbipod.client.ClientBraceState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TACZ multiplies camera recoil by each gun's "crawl_recoil_multiplier" (default 0.5) when
 * player.getPose() == SWIMMING (its crawl pose). Once a brace has settled:
 *  - wall:   report SWIMMING to that one check, so recoil uses the gun's crawl multiplier
 *  - corner: same, but only part of the reduction (1 - (1 - m) * strength)
 *  - prone:  the player is already crawling; stack an extra multiplier on top
 */
@Mixin(value = CameraSetupEvent.class, remap = false)
public abstract class CameraSetupEventMixin {
    @ModifyExpressionValue(
            method = "initialCameraRecoil",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getPose()Lnet/minecraft/world/entity/Pose;",
                    remap = true))
    private static Pose taczbipod$bracedCountsAsCrawl(Pose original) {
        if (ClientBraceState.isSettled() && ClientBraceState.type() != BraceType.PRONE) return Pose.SWIMMING;
        return original;
    }

    @ModifyExpressionValue(
            method = "initialCameraRecoil",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getCrawlRecoilMultiplier()F"))
    private static float taczbipod$braceRecoilMultiplier(float crawlMultiplier) {
        if (!ClientBraceState.isSettled()) return crawlMultiplier;
        float param = ClientBraceState.recoilParam();
        return switch (ClientBraceState.type()) {
            case CORNER -> 1f - (1f - crawlMultiplier) * param;
            case PRONE -> crawlMultiplier * param;
            default -> crawlMultiplier;
        };
    }
}
