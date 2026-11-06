package net.taczbipod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Everything both sides need to agree on "can I brace here?". Built from config on the server, synced to clients. */
public record BraceRules(Set<ResourceLocation> guns, Set<ResourceLocation> attachments,
                         double wallGap, double cornerReach, double maxFacingAngle,
                         boolean allowCorners, boolean cornerAnyGun, boolean allowProne, boolean allowFencesAndWalls) {

    public static final BraceRules NONE = new BraceRules(Collections.emptySet(), Collections.emptySet(),
            0.2, 0.35, 35, false, false, false, false);

    private static volatile BraceRules cached;

    /** Server-side rules, built once and reused until the allowlist or config changes. */
    public static BraceRules fromServerConfig() {
        BraceRules rules = cached;
        if (rules == null) cached = rules = build();
        return rules;
    }

    /** Call when the allowlist or server config changes. */
    public static void invalidate() {
        cached = null;
    }

    private static BraceRules build() {
        return new BraceRules(new HashSet<>(BipodAllowlist.get()), new HashSet<>(BipodAllowlist.getAttachments()),
                BipodConfig.MAX_GAP.get(), BipodConfig.CORNER_EDGE_REACH.get(), BipodConfig.MAX_FACING_ANGLE.get(),
                BipodConfig.ALLOW_CORNERS.get(), BipodConfig.CORNER_ANY_GUN.get(), BipodConfig.ALLOW_PRONE.get(), BipodConfig.ALLOW_FENCES_AND_WALLS.get());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(guns, FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(attachments, FriendlyByteBuf::writeResourceLocation);
        buf.writeDouble(wallGap);
        buf.writeDouble(cornerReach);
        buf.writeDouble(maxFacingAngle);
        buf.writeBoolean(allowCorners);
        buf.writeBoolean(cornerAnyGun);
        buf.writeBoolean(allowProne);
        buf.writeBoolean(allowFencesAndWalls);
    }

    public static BraceRules read(FriendlyByteBuf buf) {
        return new BraceRules(buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation),
                buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }
}
