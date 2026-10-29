package com.toancao.loadouttc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.toancao.loadouttc.LoadoutTcClient;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import com.toancao.loadouttc.platform.PlatformHolder;

/** Loads and saves {@code config/loadouttc.json}. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Debounce window for {@link #save()}; long enough to merge a burst of clicks. */
    private static final long FLUSH_NANOS = 400_000_000L;
    private static HudConfig config = new HudConfig();
    private static boolean dirty;
    private static long dirtySince;
    /** Non-null while a screen edits one panel; see {@link #beginEdit(String)}. */
    private static HudConfig edit;
    private static String editId;

    private ConfigStore() {
    }

    public static HudConfig get() {
        return edit != null ? edit : config;
    }

    // ---- Per-panel editing ----
    /**
     * Starts an editing session that writes to one cluster. {@link #get()} returns the session
     * copy, so every design control keeps working unchanged; {@link #endEdit(boolean)} folds the
     * look back into that cluster.
     */
    public static boolean beginEdit(String clusterId) {
        HudCluster cluster = config.cluster(clusterId);
        if (cluster == null) {
            return false;
        }
        HudConfig view = cluster.resolve(config);
        if (view == config) {
            // The main panel has no look of its own yet; edit a copy so the shared settings do not
            // change while the panel is being edited.
            view = config.copy();
        }
        view.clusters = config.clusters;
        view.slotOverrides = config.slotOverrides;
        view.activeCluster = clusterId;
        view.enabled = true;
        edit = view;
        editId = clusterId;
        return true;
    }

    public static boolean editingCluster() {
        return edit != null;
    }

    public static String editingClusterId() {
        return editId;
    }

    /** Ends the session, optionally folding the look into the cluster it belonged to. */
    public static void endEdit(boolean apply) {
        HudConfig view = edit;
        HudCluster cluster = config.cluster(editId);
        edit = null;
        String id = editId;
        editId = null;
        if (view == null || !apply || cluster == null) {
            return;
        }
        cluster.look = view.copy();
        cluster.look.clusters = new java.util.ArrayList<>();
        cluster.look.activeCluster = "";
        // The snapshot is a look, not a second copy of the slot data.
        cluster.look.slotOverrides = new java.util.LinkedHashMap<>();
        cluster.look.hiddenSlots = new java.util.ArrayList<>();
        cluster.look.visibleSlots = new java.util.ArrayList<>();
        cluster.look.slotOrder = new java.util.ArrayList<>();
        cluster.look.unlockedSlots = new java.util.ArrayList<>();
        cluster.sanitize();
        LoadoutTcClient.LOG.debug("Saved look for cluster {}", id);
        save();
    }

    public static void replace(HudConfig next) {
        edit = null;
        editId = null;
        config = next == null ? new HudConfig() : next;
        config.sanitize();
    }

    public static void load() {
        Path path = path();
        if (!Files.exists(path)) {
            migrateLegacy();
        }
        if (!Files.exists(path)) {
            save();
            flush();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            HudConfig loaded = GSON.fromJson(reader, HudConfig.class);
            config = loaded == null ? new HudConfig() : loaded;
            config.sanitize();
            upgrade(path);
        } catch (Exception exception) {
            LoadoutTcClient.LOG.warn("Could not read {}, using defaults", path, exception);
            config = new HudConfig();
        }
    }

    /**
     * Marks the config dirty; the write happens on the next tick or when a menu closes, so a row
     * of quick clicks costs one disk write instead of one per click.
     */
    public static void save() {
        dirty = true;
        dirtySince = System.nanoTime();
    }

    /** Writes right away. Used when a menu closes and when the game is shutting down. */
    public static void flush() {
        if (!dirty) {
            return;
        }
        dirty = false;
        config.sanitize();
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            LoadoutTcClient.LOG.warn("Could not write {}", path, exception);
        }
    }

    /** Called once per client tick from the mod initialiser. */
    public static void tick() {
        if (dirty && System.nanoTime() - dirtySince >= FLUSH_NANOS) {
            flush();
        }
    }

    /**
     * Raises the slot cap for configs written before the slot menu existed. Only the old default
     * (32) is touched, so a player who deliberately picked a number keeps it.
     */
    private static void upgrade(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("sortMode") && !root.has("slotFilter") && config.maxSlots == 32) {
                config.maxSlots = 64;
                LoadoutTcClient.LOG.info("Raised maxSlots to 64 for the new slot menu");
            }
        } catch (Exception ignored) {
            // A hand-written or partial file is not worth failing a launch over.
        }
    }

    private static Path path() {
        return PlatformHolder.get().configDir().resolve("loadouttc.json");
    }

    /** Carries settings over from the old Accessory HUD name so nothing is lost on rename. */
    private static void migrateLegacy() {
        Path legacy = PlatformHolder.get().configDir().resolve("accessoryhud.json");
        if (!Files.exists(legacy)) {
            return;
        }
        try {
            Files.copy(legacy, path());
            LoadoutTcClient.LOG.info("Migrated accessoryhud.json to loadouttc.json");
        } catch (IOException exception) {
            LoadoutTcClient.LOG.warn("Could not migrate the old config", exception);
        }
    }
}
