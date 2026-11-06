package net.taczbipod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Server-side lists of TACZ gun ids (always braceable) and attachment ids (make any gun braceable).
 * Stored in config/taczbipod/allowed_guns.json so it can be edited by hand or with /bipod.
 */
public final class BipodAllowlist {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<ResourceLocation> GUNS = new TreeSet<>();
    /** Attachments that let ANY gun they're installed on brace (e.g. a bipod grip). */
    private static final Set<ResourceLocation> ATTACHMENTS = new TreeSet<>();
    private static boolean loaded = false;

    private BipodAllowlist() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("taczbipod").resolve("allowed_guns.json");
    }

    public static synchronized Set<ResourceLocation> get() {
        if (!loaded) load();
        return Collections.unmodifiableSet(GUNS);
    }

    public static synchronized Set<ResourceLocation> getAttachments() {
        if (!loaded) load();
        return Collections.unmodifiableSet(ATTACHMENTS);
    }

    public static synchronized boolean addAttachment(ResourceLocation id) {
        if (!loaded) load();
        boolean changed = ATTACHMENTS.add(id);
        if (changed) save();
        return changed;
    }

    public static synchronized boolean removeAttachment(ResourceLocation id) {
        if (!loaded) load();
        boolean changed = ATTACHMENTS.remove(id);
        if (changed) save();
        return changed;
    }

    public static synchronized boolean add(ResourceLocation id) {
        if (!loaded) load();
        boolean changed = GUNS.add(id);
        if (changed) save();
        return changed;
    }

    public static synchronized boolean remove(ResourceLocation id) {
        if (!loaded) load();
        boolean changed = GUNS.remove(id);
        if (changed) save();
        return changed;
    }

    /** (Re)reads the file. Creates it with a few TACZ default machine guns if missing. */
    public static synchronized void load() {
        loaded = true;
        GUNS.clear();
        ATTACHMENTS.clear();
        Path path = file();
        if (!Files.exists(path)) {
            GUNS.add(new ResourceLocation("tacz", "m249"));
            GUNS.add(new ResourceLocation("tacz", "rpk"));
            GUNS.add(new ResourceLocation("tacz", "fn_evolys"));
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            readIds(root, "guns", GUNS);
            readIds(root, "attachments", ATTACHMENTS);
        } catch (Exception e) {
            LOGGER.error("[TACZ Bipod] Could not read {}; no guns can be braced until it's fixed", path, e);
        }
    }

    private static void readIds(JsonObject root, String key, Set<ResourceLocation> into) {
        if (root == null || !root.has(key)) return;
        for (var el : root.getAsJsonArray(key)) {
            ResourceLocation id = ResourceLocation.tryParse(el.getAsString().trim());
            if (id != null) into.add(id);
            else LOGGER.warn("[TACZ Bipod] Ignoring invalid id in allowed_guns.json \"{}\": {}", key, el);
        }
    }

    private static void save() {
        Path path = file();
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "guns: TACZ gun ids that can always brace. attachments: attachment ids that let any gun with them installed brace. Edit here then run /bipod reload, or use the /bipod commands.");
        JsonArray guns = new JsonArray();
        GUNS.forEach(id -> guns.add(id.toString()));
        root.add("guns", guns);
        JsonArray attachments = new JsonArray();
        ATTACHMENTS.forEach(id -> attachments.add(id.toString()));
        root.add("attachments", attachments);
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            LOGGER.error("[TACZ Bipod] Could not save {}", path, e);
        }
    }
}
