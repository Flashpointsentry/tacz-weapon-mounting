package net.taczbipod;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** Shared (client + server) rules for when and how a player can brace. */
public final class BraceLogic {
    private BraceLogic() {}

    /**
     * Where and how a player would brace.
     * @param anchor    the block the gun rests on (ground block for prone)
     * @param centerYaw the yaw the look limits are centered on
     * @param wallSide  for corners: -1 = wall on the left, +1 = wall on the right; 0 otherwise
     */
    public record Spot(BraceType type, BlockPos anchor, float centerYaw, int wallSide) {}

    // ------------------------------------------------------------------ guns

    /** The TACZ gun id of the main-hand item, or null if it isn't a TACZ gun. */
    @Nullable
    public static ResourceLocation heldGunId(Player player) {
        ItemStack stack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(stack);
        return gun == null ? null : gun.getGunId(stack);
    }

    /**
     * Whether the main-hand gun has a bipod: it's on the gun list, or it has an allowlisted
     * attachment installed (or built in). Bipods are needed for mounting on cover and prone.
     */
    public static boolean hasBipod(Player player, BraceRules rules) {
        ItemStack stack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return false;
        if (rules.guns().contains(gun.getGunId(stack))) return true;
        Set<ResourceLocation> attachments = rules.attachments();
        if (attachments.isEmpty()) return false;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            if (attachments.contains(gun.getAttachmentId(stack, type))
                    || attachments.contains(gun.getBuiltInAttachmentId(stack, type))) {
                return true;
            }
        }
        return false;
    }

    /** Whether the held gun may use this kind of brace. */
    public static boolean gunAllows(BraceType type, boolean hasBipod, BraceRules rules) {
        return type == BraceType.CORNER ? hasBipod || rules.cornerAnyGun() : hasBipod;
    }

    // ------------------------------------------------------------------ player state

    /** TACZ's crawl uses the SWIMMING pose while not actually swimming. */
    public static boolean isCrawling(Player player) {
        return player.getPose() == Pose.SWIMMING && !player.isSwimming();
    }

    public static boolean basicChecks(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.getAbilities().flying
                && !player.isInWater()
                && player.onGround();
    }

    // ------------------------------------------------------------------ finding a spot

    /**
     * Finds the best brace spot for the player and their held gun right now, or null.
     * Order: prone (if crawling), then resting on top of the block in front, then a corner.
     * Prone and on-top need a bipod; corners work for any TACZ gun unless corner.anyGun is off.
     * @param slack extra reach allowed (client uses a little so the hint doesn't flicker)
     */
    @Nullable
    public static Spot findSpot(Player player, BraceRules rules, double slack) {
        if (heldGunId(player) == null || !basicChecks(player)) return null;
        Level level = player.level();
        boolean bipod = hasBipod(player, rules);

        if (isCrawling(player)) {
            return rules.allowProne() && bipod
                    ? new Spot(BraceType.PRONE, player.blockPosition().below(), player.getYRot(), 0) : null;
        }
        if (player.getPose() == Pose.SWIMMING) return null;

        // 1) Rest on top of the block you're looking at (full block, fence or wall) with open space above
        if (bipod) {
            Spot top = findTop(player, level, rules, slack);
            if (top != null) return top;
        }

        // 2) Lean on a corner at head height, with a clear line of fire straight ahead
        if (!rules.allowCorners() || !gunAllows(BraceType.CORNER, bipod, rules)) return null;
        Direction facing = player.getDirection();
        if (angleOff(player, facing) > rules.maxFacingAngle()) return null;
        BlockPos feet = player.blockPosition();
        BlockPos front = feet.relative(facing);
        BlockPos headFront = front.above();
        if (!isEmpty(level, headFront)) return null;
        BlockPos head = feet.above();

        Spot best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction side : new Direction[]{facing.getCounterClockWise(), facing.getClockWise()}) {
            BlockPos beside = head.relative(side);
            BlockPos diagonal = headFront.relative(side);
            boolean besideSolid = isSurface(level, beside, rules);
            boolean diagonalSolid = isSurface(level, diagonal, rules);
            // Exactly one of them: the end of a wall beside you, or a pillar / door frame just ahead.
            // Both = a continuous wall (no corner); neither = nothing to lean on.
            if (besideSolid == diagonalSolid) continue;
            BlockPos anchor = besideSolid ? beside : diagonal;

            // The corner edge is at the corner of the player's own block, toward front + side
            double edgeX = feet.getX() + ((facing.getStepX() + side.getStepX()) > 0 ? 1 : 0);
            double edgeZ = feet.getZ() + ((facing.getStepZ() + side.getStepZ()) > 0 ? 1 : 0);
            double dist = distanceToShape(player, level, anchor, edgeX, edgeZ);
            if (dist <= rules.cornerReach() + slack && dist < bestDist) {
                bestDist = dist;
                int wallSide = side == facing.getCounterClockWise() ? -1 : 1;
                best = new Spot(BraceType.CORNER, anchor, facing.toYRot(), wallSide);
            }
        }
        return best;
    }

    /**
     * Casts a short horizontal ray along where the player is looking, at waist height (the middle
     * of the block at their feet). This finds thin blocks like fences and walls even when the
     * player is standing partly inside their block space.
     */
    @Nullable
    private static Spot findTop(Player player, Level level, BraceRules rules, double slack) {
        double halfWidth = player.getBbWidth() / 2;
        double yawRad = Math.toRadians(player.getYRot());
        Vec3 dir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 from = new Vec3(player.getX(), player.getY() + 0.4, player.getZ());
        // Long enough to reach the block at the widest allowed angle; the exact gap is checked below
        double length = halfWidth / Math.cos(Math.toRadians(rules.maxFacingAngle())) + rules.wallGap() + slack + 0.1;
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(dir.scale(length)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection().getAxis().isVertical()) return null;

        BlockPos pos = hit.getBlockPos();
        if (!isTopSurface(level, pos, rules)) return null;

        // Must actually face the block's side, not just glance at it
        Direction toward = hit.getDirection().getOpposite();
        if (angleOff(player, toward) > rules.maxFacingAngle()) return null;

        // Gap between the player's body and the face (perpendicular, so angle doesn't matter)
        Vec3 at = hit.getLocation();
        double centerToFace = toward.getAxis() == Direction.Axis.X
                ? Math.abs(at.x - player.getX()) : Math.abs(at.z - player.getZ());
        if (centerToFace - halfWidth > rules.wallGap() + slack) return null;

        return new Spot(BraceType.WALL, pos, toward.toYRot(), 0);
    }

    /** Degrees between where the player is looking (horizontally) and the given direction. */
    private static double angleOff(Player player, Direction dir) {
        return Math.abs(Mth.wrapDegrees(player.getYRot() - dir.toYRot()));
    }

    /** Re-checks a brace that's already active (server, every tick). */
    public static boolean stillValid(Player player, Spot spot, BraceRules rules) {
        if (!basicChecks(player)) return false;
        Level level = player.level();
        return switch (spot.type()) {
            case PRONE -> rules.allowProne() && isCrawling(player);
            case WALL -> !isCrawling(player) && isTopSurface(level, spot.anchor(), rules);
            case CORNER -> !isCrawling(player) && rules.allowCorners() && isSurface(level, spot.anchor(), rules);
        };
    }

    // ------------------------------------------------------------------ blocks

    /** Full blocks, plus fences, walls and closed fence gates if enabled. No slabs, signs, lanterns... */
    public static boolean isSurface(Level level, BlockPos pos, BraceRules rules) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.isCollisionShapeFullBlock(level, pos)) return true;
        if (!rules.allowFencesAndWalls()) return false;
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) return true;
        return state.is(BlockTags.FENCE_GATES) && state.hasProperty(FenceGateBlock.OPEN) && !state.getValue(FenceGateBlock.OPEN);
    }

    /** A surface with nothing solid on top for the gun to rest on. */
    public static boolean isTopSurface(Level level, BlockPos pos, BraceRules rules) {
        return isSurface(level, pos, rules) && isEmpty(level, pos.above());
    }

    private static boolean isEmpty(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    // ------------------------------------------------------------------ distances

    /** Horizontal distance from the player's center to the point of the block's shape nearest the given edge. */
    private static double distanceToShape(Player player, Level level, BlockPos pos, double edgeX, double edgeZ) {
        AABB box = worldBox(level, pos);
        if (box == null) return Double.MAX_VALUE;
        double x = Mth.clamp(edgeX, box.minX, box.maxX);
        double z = Mth.clamp(edgeZ, box.minZ, box.maxZ);
        double dx = x - player.getX(), dz = z - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Nullable
    private static AABB worldBox(Level level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        return shape.isEmpty() ? null : shape.bounds().move(pos.getX(), pos.getY(), pos.getZ());
    }
}
