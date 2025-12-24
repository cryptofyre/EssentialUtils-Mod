package net.ppekkungz.essentialUtils.indicator;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.ppekkungz.essentialUtils.EssentialUtils;
import net.ppekkungz.essentialUtils.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Tab Menu Service - Displays a stylized player list header and footer.
 * Features an animated logo and server info.
 * Folia-compatible using global region scheduler.
 */
public class TabMenuService {
    private final EssentialUtils plugin;
    private final PluginConfig cfg;
    
    private ScheduledTask updateTask;
    private int animationFrame = 0;
    
    // Animation color gradient frames for logo
    private static final TextColor[] GRADIENT_COLORS = {
        TextColor.color(0xFF6B6B), // Coral red
        TextColor.color(0xFFA07A), // Light salmon
        TextColor.color(0xFFD93D), // Golden yellow
        TextColor.color(0x6BCB77), // Soft green
        TextColor.color(0x4D96FF), // Sky blue
        TextColor.color(0x9B59B6), // Purple
        TextColor.color(0xE056FD), // Magenta pink
        TextColor.color(0xFF6B6B), // Back to coral
    };
    
    // Secondary accent colors for the theme
    private static final TextColor CIDER_GOLD = TextColor.color(0xF4A460);
    private static final TextColor LEAF_GREEN = TextColor.color(0x28A745);
    
    // UI accent colors
    private static final TextColor ACCENT_LIGHT = TextColor.color(0xDFE6E9);
    private static final TextColor PING_GOOD = TextColor.color(0x00FF7F);
    private static final TextColor PING_MED = TextColor.color(0xFFD700);
    private static final TextColor PING_BAD = TextColor.color(0xFF4500);
    private static final TextColor TPS_GOOD = TextColor.color(0x00FF7F);
    private static final TextColor TPS_MED = TextColor.color(0xFFD700);
    private static final TextColor TPS_BAD = TextColor.color(0xFF4500);
    
    public TabMenuService(EssentialUtils plugin, PluginConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        
        if (cfg.tabMenuEnabled()) {
            startUpdateLoop();
        }
    }
    
    /**
     * Start the tab menu update loop.
     */
    private void startUpdateLoop() {
        int updateInterval = cfg.tabMenuUpdateInterval();
        
        updateTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
            plugin,
            task -> updateAllPlayers(),
            20L, // Initial delay
            updateInterval
        );
    }
    
    /**
     * Update tab menu for all online players.
     */
    private void updateAllPlayers() {
        animationFrame = (animationFrame + 1) % (GRADIENT_COLORS.length * 8);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }
    
    /**
     * Update tab menu for a specific player.
     */
    public void updatePlayer(Player player) {
        if (!cfg.tabMenuEnabled()) return;
        
        Component header = buildHeader();
        Component footer = buildFooter(player);
        
        player.sendPlayerListHeaderAndFooter(header, footer);
    }
    
    /**
     * Build the animated header with Cider Collective logo.
     */
    private Component buildHeader() {
        Component header = Component.empty();
        
        // Top decorative line
        header = header.append(Component.newline());
        header = header.append(buildDecorativeLine());
        header = header.append(Component.newline());
        header = header.append(Component.newline());
        
        // Animated "Cider Collective" logo
        header = header.append(buildAnimatedLogo());
        header = header.append(Component.newline());
        
        // Apple/cider decoration
        header = header.append(buildAppleDecoration());
        header = header.append(Component.newline());
        header = header.append(Component.newline());
        
        // Server IP
        String serverIp = cfg.tabMenuServerIp();
        header = header.append(
            Component.text("✦ ", CIDER_GOLD)
                .append(Component.text(serverIp, ACCENT_LIGHT).decorate(TextDecoration.BOLD))
                .append(Component.text(" ✦", CIDER_GOLD))
        );
        header = header.append(Component.newline());
        
        // Bottom decorative line
        header = header.append(buildDecorativeLine());
        header = header.append(Component.newline());
        
        return header;
    }
    
    /**
     * Build the animated logo with flowing gradient.
     */
    private Component buildAnimatedLogo() {
        String logoText = "CIDER COLLECTIVE";
        Component logo = Component.empty();
        
        for (int i = 0; i < logoText.length(); i++) {
            char c = logoText.charAt(i);
            
            // Calculate color based on position and animation frame
            float phase = (animationFrame / 8.0f + i / (float) logoText.length()) % 1.0f;
            TextColor color = interpolateGradient(phase);
            
            logo = logo.append(
                Component.text(String.valueOf(c), color)
                    .decorate(TextDecoration.BOLD)
            );
        }
        
        return logo;
    }
    
    /**
     * Interpolate between gradient colors for smooth animation.
     */
    private TextColor interpolateGradient(float phase) {
        float scaledPhase = phase * (GRADIENT_COLORS.length - 1);
        int index1 = (int) scaledPhase;
        int index2 = (index1 + 1) % GRADIENT_COLORS.length;
        float t = scaledPhase - index1;
        
        TextColor c1 = GRADIENT_COLORS[index1];
        TextColor c2 = GRADIENT_COLORS[index2];
        
        int r = (int) (c1.red() + (c2.red() - c1.red()) * t);
        int g = (int) (c1.green() + (c2.green() - c1.green()) * t);
        int b = (int) (c1.blue() + (c2.blue() - c1.blue()) * t);
        
        return TextColor.color(r, g, b);
    }
    
    /**
     * Build apple/cider themed decoration.
     */
    private Component buildAppleDecoration() {
        // Animated apple that "glows"
        float pulse = (float) Math.sin(animationFrame / 4.0) * 0.5f + 0.5f;
        int redValue = (int) (180 + pulse * 75);
        TextColor appleColor = TextColor.color(redValue, 50, 60);
        
        return Component.text("🍎", appleColor)
            .append(Component.text(" ━━━ ", TextColor.color(0x636E72)))
            .append(Component.text("☕", CIDER_GOLD))
            .append(Component.text(" ━━━ ", TextColor.color(0x636E72)))
            .append(Component.text("🍎", appleColor));
    }
    
    /**
     * Build decorative line separator.
     */
    private Component buildDecorativeLine() {
        Component line = Component.empty();
        String pattern = "═══════════════════════════";
        
        for (int i = 0; i < pattern.length(); i++) {
            float phase = (animationFrame / 16.0f + i / (float) pattern.length()) % 1.0f;
            // Subtle shimmer effect
            int brightness = (int) (60 + Math.sin(phase * Math.PI * 2) * 20);
            TextColor color = TextColor.color(brightness + 40, brightness + 50, brightness + 60);
            
            line = line.append(Component.text(String.valueOf(pattern.charAt(i)), color));
        }
        
        return line;
    }
    
    /**
     * Build the footer with player stats.
     */
    private Component buildFooter(Player player) {
        Component footer = Component.empty();
        
        footer = footer.append(Component.newline());
        footer = footer.append(buildDecorativeLine());
        footer = footer.append(Component.newline());
        footer = footer.append(Component.newline());
        
        // Player count
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        footer = footer.append(
            Component.text("👥 ", LEAF_GREEN)
                .append(Component.text("Players: ", ACCENT_LIGHT))
                .append(Component.text(online, CIDER_GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text("/", TextColor.color(0x636E72)))
                .append(Component.text(max, ACCENT_LIGHT))
        );
        footer = footer.append(Component.newline());
        
        // Ping
        int ping = player.getPing();
        TextColor pingColor = ping < 50 ? PING_GOOD : (ping < 150 ? PING_MED : PING_BAD);
        String pingIcon = ping < 50 ? "📶" : (ping < 150 ? "📶" : "📶");
        footer = footer.append(
            Component.text(pingIcon + " ", pingColor)
                .append(Component.text("Ping: ", ACCENT_LIGHT))
                .append(Component.text(ping + "ms", pingColor).decorate(TextDecoration.BOLD))
        );
        footer = footer.append(Component.newline());
        
        // TPS
        double tps = getTPS();
        TextColor tpsColor = tps >= 19.0 ? TPS_GOOD : (tps >= 15.0 ? TPS_MED : TPS_BAD);
        String tpsIcon = tps >= 19.0 ? "⚡" : (tps >= 15.0 ? "⚡" : "⚡");
        footer = footer.append(
            Component.text(tpsIcon + " ", tpsColor)
                .append(Component.text("TPS: ", ACCENT_LIGHT))
                .append(Component.text(String.format("%.1f", tps), tpsColor).decorate(TextDecoration.BOLD))
        );
        footer = footer.append(Component.newline());
        
        // Memory usage (optional, nice to have)
        if (cfg.tabMenuShowMemory()) {
            Runtime runtime = Runtime.getRuntime();
            long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMB = runtime.maxMemory() / (1024 * 1024);
            double memPercent = (double) usedMB / maxMB;
            TextColor memColor = memPercent < 0.7 ? TPS_GOOD : (memPercent < 0.9 ? TPS_MED : TPS_BAD);
            
            footer = footer.append(
                Component.text("💾 ", memColor)
                    .append(Component.text("Memory: ", ACCENT_LIGHT))
                    .append(Component.text(usedMB + "MB", memColor).decorate(TextDecoration.BOLD))
                    .append(Component.text("/", TextColor.color(0x636E72)))
                    .append(Component.text(maxMB + "MB", ACCENT_LIGHT))
            );
            footer = footer.append(Component.newline());
        }
        
        footer = footer.append(Component.newline());
        
        // Chunk loader info (if enabled)
        if (cfg.chunkLoaderEnabled() && cfg.tabMenuShowChunkInfo()) {
            var chunkLoader = plugin.chunkLoader();
            if (chunkLoader != null) {
                int claimed = chunkLoader.getClaimedCount(player);
                int maxChunks = chunkLoader.getMaxChunks();
                footer = footer.append(
                    Component.text("📦 ", CIDER_GOLD)
                        .append(Component.text("Chunks: ", ACCENT_LIGHT))
                        .append(Component.text(claimed, LEAF_GREEN).decorate(TextDecoration.BOLD))
                        .append(Component.text("/", TextColor.color(0x636E72)))
                        .append(Component.text(maxChunks, ACCENT_LIGHT))
                );
                footer = footer.append(Component.newline());
                footer = footer.append(Component.newline());
            }
        }
        
        // Footer tagline
        footer = footer.append(
            Component.text("☕ ", CIDER_GOLD)
                .append(Component.text("Fresh from the orchard", TextColor.color(0xB2BEC3)).decorate(TextDecoration.ITALIC))
                .append(Component.text(" ☕", CIDER_GOLD))
        );
        footer = footer.append(Component.newline());
        footer = footer.append(buildDecorativeLine());
        footer = footer.append(Component.newline());
        
        return footer;
    }
    
    /**
     * Get current server TPS.
     * Folia-compatible - uses Paper's TPS API.
     */
    private double getTPS() {
        try {
            // Paper API for TPS
            double[] tps = Bukkit.getTPS();
            return Math.min(20.0, tps[0]); // 1-minute average, capped at 20
        } catch (Exception e) {
            return 20.0; // Default if unavailable
        }
    }
    
    /**
     * Called when a player joins - set their initial tab.
     */
    public void onPlayerJoin(Player player) {
        if (cfg.tabMenuEnabled()) {
            // Delay slightly to ensure player is fully loaded
            player.getScheduler().runDelayed(plugin, task -> {
                updatePlayer(player);
            }, null, 5L);
        }
    }
    
    /**
     * Shutdown the service.
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        // Clear tab menus for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
    }
}

