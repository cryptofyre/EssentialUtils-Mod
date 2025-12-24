package net.ppekkungz.essentialUtils;

import net.ppekkungz.essentialUtils.command.AdminCommands;
import net.ppekkungz.essentialUtils.config.PluginConfig;
import net.ppekkungz.essentialUtils.features.chunkloader.ChunkLoaderFeature;
import net.ppekkungz.essentialUtils.indicator.ActionBarService;
import net.ppekkungz.essentialUtils.indicator.TabMenuService;
import net.ppekkungz.essentialUtils.listener.ActivationListener;
import net.ppekkungz.essentialUtils.state.StateManager;
import net.ppekkungz.essentialUtils.work.WorkService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EssentialUtils - Folia-compatible survival utilities plugin.
 * 
 * Features:
 * - Tree Feller: Crouch + axe to fell entire trees
 * - Vein Miner: Always active with pickaxe on ores
 * - Auto Farm: Always active with hoe on mature crops
 * - Chunk Loader: Keep player-claimed farm chunks loaded
 * - Tab Menu: Animated logo and server info
 */
public final class EssentialUtils extends JavaPlugin {
    private static EssentialUtils instance;
    private PluginConfig cfg;
    private StateManager states;
    private WorkService work;
    private ActionBarService actionBar;
    private ChunkLoaderFeature chunkLoader;
    private TabMenuService tabMenu;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config if not exists
        saveDefaultConfig();
        loadPluginConfig();

        // Initialize services
        states = new StateManager();
        actionBar = new ActionBarService(this);
        work = new WorkService(this, cfg, states, actionBar);
        
        // Initialize new features
        chunkLoader = new ChunkLoaderFeature(this, cfg);
        tabMenu = new TabMenuService(this, cfg);

        // Register event listener
        getServer().getPluginManager().registerEvents(
            new ActivationListener(this, cfg, states, work, actionBar, chunkLoader, tabMenu), 
            this
        );

        // Register commands using Brigadier
        new AdminCommands(this).register();

        getLogger().info("EssentialUtils enabled (Folia-compatible)");
        getLogger().info("  Tree Feller: " + (cfg.treeFellerEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  Vein Miner: " + (cfg.veinMinerEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  Auto Farm: " + (cfg.autoFarmEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  Chunk Loader: " + (cfg.chunkLoaderEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("  Tab Menu: " + (cfg.tabMenuEnabled() ? "Enabled" : "Disabled"));
    }

    @Override
    public void onDisable() {
        if (work != null) work.shutdown();
        if (actionBar != null) actionBar.shutdown();
        if (chunkLoader != null) chunkLoader.shutdown();
        if (tabMenu != null) tabMenu.shutdown();
        if (states != null) states.clear();
        getLogger().info("EssentialUtils disabled.");
    }

    /**
     * Load or reload the plugin configuration.
     */
    public void loadPluginConfig() {
        this.cfg = new PluginConfig(getConfig());
    }

    // ==================== ACCESSORS ====================
    
    public static EssentialUtils get() { 
        return instance; 
    }
    
    public PluginConfig cfg() { 
        return cfg; 
    }
    
    public StateManager states() { 
        return states; 
    }
    
    public WorkService work() { 
        return work; 
    }
    
    public ActionBarService actionBar() { 
        return actionBar; 
    }
    
    public ChunkLoaderFeature chunkLoader() { 
        return chunkLoader; 
    }
    
    public TabMenuService tabMenu() { 
        return tabMenu; 
    }
}
