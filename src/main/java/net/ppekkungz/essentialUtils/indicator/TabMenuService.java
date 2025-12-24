package net.ppekkungz.essentialUtils.indicator;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.ppekkungz.essentialUtils.EssentialUtils;
import net.ppekkungz.essentialUtils.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab Menu Service - Displays a stylized player list header and footer.
 * Features an animated logo and server info. Highly configurable.
 * Folia-compatible using global region scheduler.
 */
public class TabMenuService {
    private final EssentialUtils plugin;
    private final PluginConfig cfg;
    
    private ScheduledTask updateTask;
    private int animationFrame = 0;
    
    // Animation color gradient frames
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
    
    // Theme colors
    private static final TextColor ACCENT_GOLD = TextColor.color(0xF4A460);
    private static final TextColor ACCENT_GREEN = TextColor.color(0x28A745);
    private static final TextColor ACCENT_LIGHT = TextColor.color(0xDFE6E9);
    private static final TextColor ACCENT_DARK = TextColor.color(0x636E72);
    
    // Status colors
    private static final TextColor STATUS_GOOD = TextColor.color(0x00FF7F);
    private static final TextColor STATUS_MED = TextColor.color(0xFFD700);
    private static final TextColor STATUS_BAD = TextColor.color(0xFF4500);
    
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
            20L,
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
     * Build the header with animated logo.
     */
    private Component buildHeader() {
        Component header = Component.empty();
        
        // Top decoration
        if (cfg.tabMenuShowDecorations()) {
            header = header.append(Component.newline());
            header = header.append(buildDecorativeLine());
        }
        header = header.append(Component.newline());
        
        // Animated logo
        header = header.append(buildAnimatedLogo());
        header = header.append(Component.newline());
        
        // Optional tagline
        String tagline = cfg.tabMenuHeaderTagline();
        if (!tagline.isEmpty()) {
            header = header.append(
                Component.text(tagline, ACCENT_DARK).decorate(TextDecoration.ITALIC)
            );
            header = header.append(Component.newline());
        }
        
        // Server IP
        String serverIp = cfg.tabMenuServerIp();
        if (!serverIp.isEmpty()) {
            if (cfg.tabMenuShowDecorations()) {
                header = header.append(
                    Component.text("✦ ", ACCENT_GOLD)
                        .append(Component.text(serverIp, ACCENT_LIGHT).decorate(TextDecoration.BOLD))
                        .append(Component.text(" ✦", ACCENT_GOLD))
                );
            } else {
                header = header.append(
                    Component.text(serverIp, ACCENT_LIGHT).decorate(TextDecoration.BOLD)
                );
            }
            header = header.append(Component.newline());
        }
        
        // Bottom decoration
        if (cfg.tabMenuShowDecorations()) {
            header = header.append(buildDecorativeLine());
            header = header.append(Component.newline());
        }
        
        return header;
    }
    
    /**
     * Build the animated logo with flowing gradient.
     */
    private Component buildAnimatedLogo() {
        String logoText = cfg.tabMenuLogoText();
        if (logoText.isEmpty()) return Component.empty();
        
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
     * Build decorative line separator.
     */
    private Component buildDecorativeLine() {
        String style = cfg.tabMenuDecorationStyle();
        int length = cfg.tabMenuDecorationLength();
        
        Component line = Component.empty();
        
        for (int i = 0; i < length; i++) {
            float phase = (animationFrame / 16.0f + i / (float) length) % 1.0f;
            int brightness = (int) (60 + Math.sin(phase * Math.PI * 2) * 20);
            TextColor color = TextColor.color(brightness + 40, brightness + 50, brightness + 60);
            
            line = line.append(Component.text(style, color));
        }
        
        return line;
    }
    
    /**
     * Build the footer with player stats.
     */
    private Component buildFooter(Player player) {
        Component footer = Component.empty();
        boolean compact = cfg.tabMenuCompactMode();
        
        // Top decoration
        if (cfg.tabMenuShowDecorations()) {
            footer = footer.append(Component.newline());
            footer = footer.append(buildDecorativeLine());
        }
        footer = footer.append(Component.newline());
        
        if (compact) {
            footer = footer.append(buildCompactStats(player));
        } else {
            footer = footer.append(buildExpandedStats(player));
        }
        
        // Chunk info
        if (cfg.chunkLoaderEnabled() && cfg.tabMenuShowChunkInfo()) {
            var chunkLoader = plugin.chunkLoader();
            if (chunkLoader != null) {
                int claimed = chunkLoader.getClaimedCount(player);
                int maxChunks = chunkLoader.getMaxChunks();
                footer = footer.append(
                    Component.text("📦 ", ACCENT_GOLD)
                        .append(Component.text(claimed, ACCENT_GREEN).decorate(TextDecoration.BOLD))
                        .append(Component.text("/" + maxChunks, ACCENT_DARK))
                );
                footer = footer.append(Component.newline());
            }
        }
        
        // Footer tagline
        String tagline = cfg.tabMenuFooterTagline();
        if (!tagline.isEmpty()) {
            footer = footer.append(
                Component.text(tagline, ACCENT_DARK).decorate(TextDecoration.ITALIC)
            );
            footer = footer.append(Component.newline());
        }
        
        // Bottom decoration
        if (cfg.tabMenuShowDecorations()) {
            footer = footer.append(buildDecorativeLine());
            footer = footer.append(Component.newline());
        }
        
        return footer;
    }
    
    /**
     * Build compact stats (single line).
     */
    private Component buildCompactStats(Player player) {
        List<Component> parts = new ArrayList<>();
        
        // Players
        if (cfg.tabMenuShowPlayers()) {
            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            parts.add(
                Component.text("👥 ", ACCENT_GREEN)
                    .append(Component.text(online, ACCENT_GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text("/" + max, ACCENT_DARK))
            );
        }
        
        // Ping
        if (cfg.tabMenuShowPing()) {
            int ping = player.getPing();
            TextColor pingColor = ping < 50 ? STATUS_GOOD : (ping < 150 ? STATUS_MED : STATUS_BAD);
            parts.add(
                Component.text("📶 ", pingColor)
                    .append(Component.text(ping + "ms", pingColor).decorate(TextDecoration.BOLD))
            );
        }
        
        // TPS
        if (cfg.tabMenuShowTps()) {
            double tps = getTPS();
            TextColor tpsColor = tps >= 19.0 ? STATUS_GOOD : (tps >= 15.0 ? STATUS_MED : STATUS_BAD);
            parts.add(
                Component.text("⚡ ", tpsColor)
                    .append(Component.text(String.format("%.0f", tps), tpsColor).decorate(TextDecoration.BOLD))
            );
        }
        
        // Memory
        if (cfg.tabMenuShowMemory()) {
            Runtime runtime = Runtime.getRuntime();
            long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMB = runtime.maxMemory() / (1024 * 1024);
            double memPercent = (double) usedMB / maxMB;
            TextColor memColor = memPercent < 0.7 ? STATUS_GOOD : (memPercent < 0.9 ? STATUS_MED : STATUS_BAD);
            parts.add(
                Component.text("💾 ", memColor)
                    .append(Component.text(usedMB + "MB", memColor).decorate(TextDecoration.BOLD))
            );
        }
        
        // Join with separators
        Component result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result = result.append(Component.text("  ", ACCENT_DARK));
            }
            result = result.append(parts.get(i));
        }
        result = result.append(Component.newline());
        
        return result;
    }
    
    /**
     * Build expanded stats (multiple lines).
     */
    private Component buildExpandedStats(Player player) {
        Component stats = Component.empty();
        
        // Players
        if (cfg.tabMenuShowPlayers()) {
            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            stats = stats.append(
                Component.text("👥 ", ACCENT_GREEN)
                    .append(Component.text("Players: ", ACCENT_LIGHT))
                    .append(Component.text(online, ACCENT_GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text("/" + max, ACCENT_DARK))
            );
            stats = stats.append(Component.newline());
        }
        
        // Ping
        if (cfg.tabMenuShowPing()) {
            int ping = player.getPing();
            TextColor pingColor = ping < 50 ? STATUS_GOOD : (ping < 150 ? STATUS_MED : STATUS_BAD);
            stats = stats.append(
                Component.text("📶 ", pingColor)
                    .append(Component.text("Ping: ", ACCENT_LIGHT))
                    .append(Component.text(ping + "ms", pingColor).decorate(TextDecoration.BOLD))
            );
            stats = stats.append(Component.newline());
        }
        
        // TPS
        if (cfg.tabMenuShowTps()) {
            double tps = getTPS();
            TextColor tpsColor = tps >= 19.0 ? STATUS_GOOD : (tps >= 15.0 ? STATUS_MED : STATUS_BAD);
            stats = stats.append(
                Component.text("⚡ ", tpsColor)
                    .append(Component.text("TPS: ", ACCENT_LIGHT))
                    .append(Component.text(String.format("%.1f", tps), tpsColor).decorate(TextDecoration.BOLD))
            );
            stats = stats.append(Component.newline());
        }
        
        // Memory
        if (cfg.tabMenuShowMemory()) {
            Runtime runtime = Runtime.getRuntime();
            long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMB = runtime.maxMemory() / (1024 * 1024);
            double memPercent = (double) usedMB / maxMB;
            TextColor memColor = memPercent < 0.7 ? STATUS_GOOD : (memPercent < 0.9 ? STATUS_MED : STATUS_BAD);
            stats = stats.append(
                Component.text("💾 ", memColor)
                    .append(Component.text("Memory: ", ACCENT_LIGHT))
                    .append(Component.text(usedMB + "MB", memColor).decorate(TextDecoration.BOLD))
                    .append(Component.text("/" + maxMB + "MB", ACCENT_DARK))
            );
            stats = stats.append(Component.newline());
        }
        
        return stats;
    }
    
    /**
     * Get current server TPS.
     */
    private double getTPS() {
        try {
            double[] tps = Bukkit.getTPS();
            return Math.min(20.0, tps[0]);
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    /**
     * Called when a player joins.
     */
    public void onPlayerJoin(Player player) {
        if (cfg.tabMenuEnabled()) {
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
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
    }
}
