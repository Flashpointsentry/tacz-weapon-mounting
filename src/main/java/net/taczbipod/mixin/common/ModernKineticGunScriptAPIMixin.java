package net.taczbipod.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.taczbipod.BipodConfig;
import net.taczbipod.BraceType;
import net.taczbipod.server.ServerBraceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * TACZ picks bullet spread on the server from the shooter's state: AIM, LIE (crawling),
 * SNEAK, MOVE or STAND. A settled brace hip-fires with the gun's LIE spread.
 * Aiming down sights (AIM) is never changed.
 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {
    @WrapOperation(
            method = "shootOnce",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/InaccuracyType;getInaccuracyType(Lnet/minecraft/world/entity/LivingEntity;)Lcom/tacz/guns/resource/pojo/data/gun/InaccuracyType;"))
    private InaccuracyType taczbipod$bracedHipfire(LivingEntity shooter, Operation<InaccuracyType> original) {
        InaccuracyType type = original.call(shooter);
        if (type == InaccuracyType.AIM || !(shooter instanceof ServerPlayer player)) return type;
        BraceType brace = ServerBraceManager.settledType(player);
        if (brace == null) return type;
        boolean steadier = switch (brace) {
            case WALL -> BipodConfig.WALL_HIPFIRE.get();
            case CORNER -> BipodConfig.CORNER_HIPFIRE.get();
            case PRONE -> false; // already lying down
        };
        return steadier ? InaccuracyType.LIE : type;
    }
}
