package net.ppekkungz.essentialUtils.config;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Simplified configuration for EssentialUtils.
 * Handles module settings, actionbar customization, and performance tuning.
 */
public class PluginConfig {
    private final FileConfiguration c;
    
    public PluginConfig(FileConfiguration c) { 
        this.c = c; 
    }

    // ==================== MODULES ====================
    
    // Tree Feller
    public boolean treeFellerEnabled() { 
        return c.getBoolean("modules.treeFeller.enabled", true); 
    }
    public void setTreeFellerEnabled(boolean enabled) {
        c.set("modules.treeFeller.enabled", enabled);
    }
    public int treeFellerMaxBlocks() { 
        return c.getInt("modules.treeFeller.maxBlocks", 200); 
    }
    public boolean treeFellerReplant() { 
        return c.getBoolean("modules.treeFeller.replantSaplings", true); 
    }
    public boolean treeFellerParticles() { 
        return c.getBoolean("modules.treeFeller.particleEffects", true); 
    }

    // Vein Miner
    public boolean veinMinerEnabled() { 
        return c.getBoolean("modules.veinMiner.enabled", true); 
    }
    public void setVeinMinerEnabled(boolean enabled) {
        c.set("modules.veinMiner.enabled", enabled);
    }
    public int veinMinerMaxOres() { 
        return c.getInt("modules.veinMiner.maxOres", 64); 
    }
    public boolean veinMinerFortuneEnabled() { 
        return c.getBoolean("modules.veinMiner.fortuneEnabled", true); 
    }
    public boolean veinMinerSilkTouchDropsOre() { 
        return c.getBoolean("modules.veinMiner.silkTouchDropsOre", true); 
    }

    // Auto Farm
    public boolean autoFarmEnabled() { 
        return c.getBoolean("modules.autoFarm.enabled", true); 
    }
    public void setAutoFarmEnabled(boolean enabled) {
        c.set("modules.autoFarm.enabled", enabled);
    }
    public int autoFarmRadius() { 
        return c.getInt("modules.autoFarm.radius", 4); 
    }
    public boolean autoFarmReplant() { 
        return c.getBoolean("modules.autoFarm.autoReplant", true); 
    }

    // Chunk Loader
    public boolean chunkLoaderEnabled() { 
        return c.getBoolean("modules.chunkLoader.enabled", true); 
    }
    public void setChunkLoaderEnabled(boolean enabled) {
        c.set("modules.chunkLoader.enabled", enabled);
    }
    public int chunkLoaderMaxChunksPerPlayer() { 
        return c.getInt("modules.chunkLoader.maxChunksPerPlayer", 9); 
    }
    public int chunkLoaderValidationInterval() { 
        return c.getInt("modules.chunkLoader.validationInterval", 300); 
    }
    public boolean chunkLoaderClaimOnFarm() { 
        return c.getBoolean("modules.chunkLoader.claimOnFarm", true); 
    }

    // ==================== ACTIONBAR ====================
    
    // Tree Feller ActionBar
    public boolean treeFellerShowIndicator() { 
        return c.getBoolean("actionbar.treeFeller.showActiveIndicator", true); 
    }
    public String treeFellerActiveMessage() { 
        return colorize(c.getString("actionbar.treeFeller.activeMessage", "&a⚒ Tree Feller Active")); 
    }
    public boolean treeFellerShowSummary() { 
        return c.getBoolean("actionbar.treeFeller.showSummary", true); 
    }
    public String treeFellerSummaryFormat() { 
        return colorize(c.getString("actionbar.treeFeller.summaryFormat", 
            "&a🌳 &f{logs} logs &7| &f{saplings} saplings &7| &f{apples} apples")); 
    }

    // Vein Miner ActionBar
    public boolean veinMinerShowSummary() { 
        return c.getBoolean("actionbar.veinMiner.showSummary", true); 
    }
    public int veinMinerSummaryDuration() { 
        return c.getInt("actionbar.veinMiner.summaryDuration", 40); 
    }
    public String veinMinerSummaryFormat() { 
        return colorize(c.getString("actionbar.veinMiner.summaryFormat", 
            "&b⛏ &ex{count} {ore} &7| &f{drops} &7({mult}) &7| &a{xp} XP")); 
    }

    // Chunk Loader ActionBar
    public boolean chunkLoaderShowClaimMessage() { 
        return c.getBoolean("actionbar.chunkLoader.showClaimMessage", true); 
    }
    public String chunkLoaderClaimMessage() { 
        return colorize(c.getString("actionbar.chunkLoader.claimMessage", 
            "&a📦 Chunk claimed! &7({current}/{max})")); 
    }
    public String chunkLoaderUnclaimMessage() { 
        return colorize(c.getString("actionbar.chunkLoader.unclaimMessage", 
            "&e📦 Chunk unclaimed.")); 
    }

    // ==================== TAB MENU ====================
    
    public boolean tabMenuEnabled() { 
        return c.getBoolean("tabMenu.enabled", true); 
    }
    public void setTabMenuEnabled(boolean enabled) {
        c.set("tabMenu.enabled", enabled);
    }
    public int tabMenuUpdateInterval() { 
        return c.getInt("tabMenu.updateInterval", 4); 
    }
    
    // Header
    public String tabMenuLogoText() { 
        return c.getString("tabMenu.header.logoText", "CIDER COLLECTIVE"); 
    }
    public String tabMenuServerIp() { 
        return c.getString("tabMenu.header.serverIp", "play.cidercollective.net"); 
    }
    public String tabMenuHeaderTagline() { 
        return c.getString("tabMenu.header.tagline", ""); 
    }
    public boolean tabMenuShowDecorations() { 
        return c.getBoolean("tabMenu.header.showDecorations", true); 
    }
    public String tabMenuDecorationStyle() { 
        return c.getString("tabMenu.header.decorationStyle", "═"); 
    }
    public int tabMenuDecorationLength() { 
        return c.getInt("tabMenu.header.decorationLength", 20); 
    }
    
    // Footer
    public boolean tabMenuShowPlayers() { 
        return c.getBoolean("tabMenu.footer.showPlayers", true); 
    }
    public boolean tabMenuShowPing() { 
        return c.getBoolean("tabMenu.footer.showPing", true); 
    }
    public boolean tabMenuShowTps() { 
        return c.getBoolean("tabMenu.footer.showTps", true); 
    }
    public boolean tabMenuShowMemory() { 
        return c.getBoolean("tabMenu.footer.showMemory", false); 
    }
    public boolean tabMenuShowChunkInfo() { 
        return c.getBoolean("tabMenu.footer.showChunkInfo", true); 
    }
    public String tabMenuFooterTagline() { 
        return c.getString("tabMenu.footer.tagline", ""); 
    }
    public boolean tabMenuCompactMode() { 
        return c.getBoolean("tabMenu.footer.compactMode", true); 
    }

    // ==================== PERFORMANCE ====================
    
    public int blocksPerTick() { 
        return c.getInt("performance.blocksPerTick", 32); 
    }
    public boolean requireChunkLoaded() { 
        return c.getBoolean("performance.requireChunkLoaded", true); 
    }

    // ==================== UTILITIES ====================
    
    /**
     * Convert & color codes to legacy format for actionbar display.
     * Uses Adventure's legacy serializer for modern compatibility.
     */
    private String colorize(String text) {
        if (text == null) return "";
        // Convert & codes to section symbol for legacy compatibility
        return LegacyComponentSerializer.legacyAmpersand()
            .serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }
    
    /**
     * Get the underlying FileConfiguration for saving
     */
    public FileConfiguration getConfig() {
        return c;
    }
}
