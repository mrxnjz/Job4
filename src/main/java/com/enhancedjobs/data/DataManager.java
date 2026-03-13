package com.enhancedjobs.data;

import com.enhancedjobs.EnhancedJobsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles loading and saving PlayerData to per-player YAML files.
 */
public class DataManager {

    private final EnhancedJobsPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
        String folderName = plugin.getConfig().getString("data.player-data-folder", "playerdata");
        this.dataFolder = new File(plugin.getDataFolder(), folderName);
        if (!dataFolder.exists()) dataFolder.mkdirs();
        scheduleAutoSave();
    }

    // ─── Load / Get ────────────────────────────────────────────────────────────

    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    private PlayerData loadFromDisk(UUID uuid) {
        File file = getFile(uuid);
        if (!file.exists()) return new PlayerData(uuid);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid);

        // Load reset timestamp
        data.setLastQuestResetMs(cfg.getLong("lastQuestReset", System.currentTimeMillis()));

        // Load quest counts today
        ConfigurationSection qToday = cfg.getConfigurationSection("questsToday");
        if (qToday != null) {
            for (String jobId : qToday.getKeys(false)) {
                data.getRawQuestsAcquiredToday().put(jobId, qToday.getInt(jobId));
            }
        }

        // Load job states
        ConfigurationSection jobsSec = cfg.getConfigurationSection("jobs");
        if (jobsSec != null) {
            for (String jobId : jobsSec.getKeys(false)) {
                ConfigurationSection js = jobsSec.getConfigurationSection(jobId);
                if (js == null) continue;
                int totalXp = js.getInt("totalXp", 0);
                boolean perksActive = js.getBoolean("perksActive", true);
                List<String> perkList = js.getStringList("enabledPerks");
                Set<String> perks = new HashSet<>(perkList);
                data.getRawJobStates().put(jobId, new PlayerJobState(jobId, totalXp, perksActive, perks));
            }
        }

        // Load active quests
        ConfigurationSection questsSec = cfg.getConfigurationSection("activeQuests");
        if (questsSec != null) {
            for (String jobId : questsSec.getKeys(false)) {
                ConfigurationSection qs = questsSec.getConfigurationSection(jobId);
                if (qs == null) continue;
                String questId = qs.getString("questId", "");
                long acceptedAt = qs.getLong("acceptedAt", System.currentTimeMillis());
                Map<String, Integer> progress = new HashMap<>();
                ConfigurationSection progSec = qs.getConfigurationSection("progress");
                if (progSec != null) {
                    for (String objId : progSec.getKeys(false)) {
                        progress.put(objId, progSec.getInt(objId));
                    }
                }
                data.getRawActiveQuests().put(jobId, new ActiveQuestData(questId, jobId, acceptedAt, progress));
            }
        }

        return data;
    }

    // ─── Save ──────────────────────────────────────────────────────────────────

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        saveToDisk(data);
    }

    private void saveToDisk(PlayerData data) {
        File file = getFile(data.getPlayerId());
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("lastQuestReset", data.getLastQuestResetMs());

        // Save quest counts today
        for (Map.Entry<String, Integer> e : data.getRawQuestsAcquiredToday().entrySet()) {
            cfg.set("questsToday." + e.getKey(), e.getValue());
        }

        // Save job states
        for (Map.Entry<String, PlayerJobState> e : data.getRawJobStates().entrySet()) {
            String path = "jobs." + e.getKey();
            PlayerJobState js = e.getValue();
            cfg.set(path + ".totalXp", js.getTotalXp());
            cfg.set(path + ".perksActive", js.isPerksActive());
            cfg.set(path + ".enabledPerks", new ArrayList<>(js.getEnabledPerkIds()));
        }

        // Save active quests
        for (Map.Entry<String, ActiveQuestData> e : data.getRawActiveQuests().entrySet()) {
            String path = "activeQuests." + e.getKey();
            ActiveQuestData aq = e.getValue();
            cfg.set(path + ".questId", aq.getQuestId());
            cfg.set(path + ".acceptedAt", aq.getAcceptedAt());
            for (Map.Entry<String, Integer> prog : aq.getAllProgress().entrySet()) {
                cfg.set(path + ".progress." + prog.getKey(), prog.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + data.getPlayerId(), ex);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            saveToDisk(data);
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) saveToDisk(data);
    }

    /** Called periodically to check for and perform daily quest resets. */
    public void checkQuestResets() {
        for (PlayerData data : cache.values()) {
            data.checkAndReset();
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private File getFile(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }

    private void scheduleAutoSave() {
        int minutes = plugin.getConfig().getInt("data.auto-save-minutes", 5);
        long ticks = minutes * 60L * 20L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, ticks, ticks);
    }
}
