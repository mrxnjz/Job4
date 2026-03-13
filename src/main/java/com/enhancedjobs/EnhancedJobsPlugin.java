package com.enhancedjobs;

import com.enhancedjobs.command.JobAdminCommand;
import com.enhancedjobs.command.JobCommand;
import com.enhancedjobs.command.JobQuestsCommand;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.gui.JobMenuGui;
import com.enhancedjobs.gui.QuestMenuGui;
import com.enhancedjobs.listener.GuiListener;
import com.enhancedjobs.listener.PlayerListener;
import com.enhancedjobs.manager.JobManager;
import com.enhancedjobs.manager.QuestManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EnhancedJobSystem — main plugin class.
 *
 * HOW TO ADD A JOB FROM AN EXTERNAL PLUGIN:
 * ------------------------------------------
 * In your plugin's onEnable():
 *   EnhancedJobsPlugin jobs = (EnhancedJobsPlugin) Bukkit.getPluginManager().getPlugin("EnhancedJobSystem");
 *   if (jobs != null) {
 *       jobs.getJobManager().registerJob(new YourJob());
 *   }
 *
 * Or use Bukkit's ServicesManager:
 *   JobManager mgr = Bukkit.getServicesManager().load(JobManager.class);
 *   if (mgr != null) mgr.registerJob(new YourJob());
 */
public class EnhancedJobsPlugin extends JavaPlugin {

    private static EnhancedJobsPlugin instance;

    private DataManager   dataManager;
    private JobManager    jobManager;
    private QuestManager  questManager;
    private JobMenuGui    jobMenuGui;
    private QuestMenuGui  questMenuGui;
    private Economy       economy;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadConfig();

        // Core systems
        dataManager  = new DataManager(this);
        jobManager   = new JobManager(this);
        questManager = new QuestManager(this);

        // GUI builders
        jobMenuGui   = new JobMenuGui(this);
        questMenuGui = new QuestMenuGui(this);

        // Vault economy
        setupEconomy();

        // Commands
        getCommand("job").setExecutor(new JobCommand(this));
        getCommand("job").setTabCompleter(new JobCommand(this));

        getCommand("jobquests").setExecutor(new JobQuestsCommand(this));
        getCommand("jobquests").setTabCompleter(new JobQuestsCommand(this));

        getCommand("jobadmin").setExecutor(new JobAdminCommand(this));
        getCommand("jobadmin").setTabCompleter(new JobAdminCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this),    this);

        // Expose JobManager via ServicesManager for inter-plugin use
        getServer().getServicesManager().register(JobManager.class, jobManager, this,
                org.bukkit.plugin.ServicePriority.Normal);

        // Daily reset task — runs every minute to check if a player's 24h window has elapsed
        getServer().getScheduler().runTaskTimer(this,
                () -> dataManager.checkQuestResets(), 1200L, 1200L);

        getLogger().info("EnhancedJobSystem v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Registered 0 jobs. Add jobs via registerJob() or external plugins.");
    }

    @Override
    public void onDisable() {
        // Save all online players before shutdown
        getServer().getOnlinePlayers().forEach(p -> {
            jobManager.onPlayerLogout(p);
            dataManager.save(p.getUniqueId());
        });
        dataManager.saveAll();
        getLogger().info("EnhancedJobSystem disabled — all data saved.");
    }

    private void loadConfig() {
        // Validate config values
        double baseCost = getConfig().getDouble("economy.base-quest-cost", 30.0);
        if (baseCost < 0) {
            getLogger().warning("base-quest-cost cannot be negative, defaulting to 30.");
            getConfig().set("economy.base-quest-cost", 30.0);
        }
    }

    private void setupEconomy() {
        if (!getConfig().getBoolean("economy.enabled", true)) {
            getLogger().info("Economy disabled in config.");
            return;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found — economy features disabled. " +
                    "Players will receive free quests without charge.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy provider found via Vault — economy features disabled.");
            return;
        }
        economy = rsp.getProvider();
        getLogger().info("Economy hooked via Vault: " + economy.getName());
    }

    // ─── Static accessor ───────────────────────────────────────────────────────

    public static EnhancedJobsPlugin getInstance() { return instance; }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public DataManager   getDataManager()  { return dataManager; }
    public JobManager    getJobManager()   { return jobManager; }
    public QuestManager  getQuestManager() { return questManager; }
    public JobMenuGui    getJobMenuGui()   { return jobMenuGui; }
    public QuestMenuGui  getQuestMenuGui() { return questMenuGui; }
    public Economy       getEconomy()      { return economy; }
}
