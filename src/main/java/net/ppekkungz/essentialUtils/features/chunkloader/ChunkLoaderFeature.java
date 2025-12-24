package net.ppekkungz.essentialUtils.features.chunkloader;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.ppekkungz.essentialUtils.EssentialUtils;
import net.ppekkungz.essentialUtils.config.PluginConfig;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk Loader Feature - Keeps player-claimed farm chunks loaded.
 * Folia-compatible using plugin chunk tickets.
 * 
 * Players can claim chunks by crouching + breaking a crop, or via command.
 * Chunks stay loaded even when players are offline (configurable).
 */
public class ChunkLoaderFeature {
    private final EssentialUtils plugin;
    private final PluginConfig cfg;
    
    // Tracks claimed chunks per player (UUID -> Set of ChunkKey)
    private final Map<UUID, Set<ChunkKey>> playerChunks = new ConcurrentHashMap<>();
    
    // All currently loaded chunks (for quick lookup)
    private final Set<ChunkKey> loadedChunks = ConcurrentHashMap.newKeySet();
    
    // Scheduled task for periodic chunk validation
    private ScheduledTask validationTask;
    
    public ChunkLoaderFeature(EssentialUtils plugin, PluginConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        
        // Load saved chunk claims from config
        loadChunkClaims();
        
        // Start validation loop
        startValidationLoop();
    }
    
    /**
     * Claim a chunk for a player (keeps it loaded).
     * Returns true if claimed successfully, false if at limit or already claimed.
     */
    public ClaimResult claimChunk(Player player, Chunk chunk) {
        if (!cfg.chunkLoaderEnabled()) {
            return ClaimResult.FEATURE_DISABLED;
        }
        
        UUID playerId = player.getUniqueId();
        ChunkKey key = new ChunkKey(chunk);
        
        // Check if already claimed by this player
        Set<ChunkKey> owned = playerChunks.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (owned.contains(key)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        
        // Check if claimed by another player
        for (Map.Entry<UUID, Set<ChunkKey>> entry : playerChunks.entrySet()) {
            if (!entry.getKey().equals(playerId) && entry.getValue().contains(key)) {
                return ClaimResult.CLAIMED_BY_OTHER;
            }
        }
        
        // Check player's chunk limit
        int maxChunks = cfg.chunkLoaderMaxChunksPerPlayer();
        if (owned.size() >= maxChunks) {
            return ClaimResult.AT_LIMIT;
        }
        
        // Claim the chunk
        owned.add(key);
        loadedChunks.add(key);
        
        // Add chunk ticket to keep it loaded
        addChunkTicket(key);
        
        // Save to config
        saveChunkClaims();
        
        return ClaimResult.SUCCESS;
    }
    
    /**
     * Unclaim a chunk for a player.
     */
    public boolean unclaimChunk(Player player, Chunk chunk) {
        UUID playerId = player.getUniqueId();
        ChunkKey key = new ChunkKey(chunk);
        
        Set<ChunkKey> owned = playerChunks.get(playerId);
        if (owned == null || !owned.contains(key)) {
            return false;
        }
        
        // Remove from tracking
        owned.remove(key);
        if (owned.isEmpty()) {
            playerChunks.remove(playerId);
        }
        loadedChunks.remove(key);
        
        // Remove chunk ticket
        removeChunkTicket(key);
        
        // Save to config
        saveChunkClaims();
        
        return true;
    }
    
    /**
     * Check if a chunk is claimed by a player.
     */
    public boolean isClaimedBy(Player player, Chunk chunk) {
        Set<ChunkKey> owned = playerChunks.get(player.getUniqueId());
        return owned != null && owned.contains(new ChunkKey(chunk));
    }
    
    /**
     * Check if a chunk is claimed by anyone.
     */
    public boolean isClaimed(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk);
        return loadedChunks.contains(key);
    }
    
    /**
     * Get the number of chunks claimed by a player.
     */
    public int getClaimedCount(Player player) {
        Set<ChunkKey> owned = playerChunks.get(player.getUniqueId());
        return owned != null ? owned.size() : 0;
    }
    
    /**
     * Get the maximum chunks a player can claim.
     */
    public int getMaxChunks() {
        return cfg.chunkLoaderMaxChunksPerPlayer();
    }
    
    /**
     * Get all chunks claimed by a player.
     */
    public Set<ChunkKey> getPlayerChunks(UUID playerId) {
        Set<ChunkKey> owned = playerChunks.get(playerId);
        return owned != null ? Collections.unmodifiableSet(owned) : Collections.emptySet();
    }
    
    /**
     * Get total number of loaded chunks.
     */
    public int getTotalLoadedChunks() {
        return loadedChunks.size();
    }
    
    /**
     * Add a plugin chunk ticket to keep the chunk loaded.
     * Folia-compatible - uses region scheduler for chunk operations.
     */
    private void addChunkTicket(ChunkKey key) {
        World world = plugin.getServer().getWorld(key.worldName());
        if (world == null) return;
        
        // Use global region scheduler for chunk ticket operations
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            world.addPluginChunkTicket(key.x(), key.z(), plugin);
        });
    }
    
    /**
     * Remove a plugin chunk ticket.
     */
    private void removeChunkTicket(ChunkKey key) {
        World world = plugin.getServer().getWorld(key.worldName());
        if (world == null) return;
        
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            world.removePluginChunkTicket(key.x(), key.z(), plugin);
        });
    }
    
    /**
     * Re-apply all chunk tickets (used on startup and validation).
     */
    public void reapplyAllTickets() {
        for (ChunkKey key : loadedChunks) {
            addChunkTicket(key);
        }
    }
    
    /**
     * Start a periodic validation loop to ensure chunks stay loaded.
     */
    private void startValidationLoop() {
        if (!cfg.chunkLoaderEnabled()) return;
        
        int intervalTicks = cfg.chunkLoaderValidationInterval() * 20; // Convert seconds to ticks
        
        validationTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
            plugin,
            task -> validateChunks(),
            intervalTicks,
            intervalTicks
        );
    }
    
    /**
     * Validate all chunks are still loaded.
     */
    private void validateChunks() {
        for (ChunkKey key : loadedChunks) {
            World world = plugin.getServer().getWorld(key.worldName());
            if (world == null) continue;
            
            // Re-add ticket if chunk isn't loaded
            if (!world.isChunkLoaded(key.x(), key.z())) {
                world.addPluginChunkTicket(key.x(), key.z(), plugin);
            }
        }
    }
    
    /**
     * Load chunk claims from config file.
     */
    private void loadChunkClaims() {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection("chunkloader.claims");
        
        if (section == null) return;
        
        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                List<String> chunkStrings = section.getStringList(uuidStr);
                
                Set<ChunkKey> chunks = ConcurrentHashMap.newKeySet();
                for (String chunkStr : chunkStrings) {
                    ChunkKey key = ChunkKey.fromString(chunkStr);
                    if (key != null) {
                        chunks.add(key);
                        loadedChunks.add(key);
                    }
                }
                
                if (!chunks.isEmpty()) {
                    playerChunks.put(playerId, chunks);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in chunk claims: " + uuidStr);
            }
        }
        
        // Apply tickets for all loaded chunks
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> {
            reapplyAllTickets();
            plugin.getLogger().info("Loaded " + loadedChunks.size() + " chunk claims from config.");
        }, 20L); // Delay to ensure worlds are loaded
    }
    
    /**
     * Save chunk claims to config file.
     */
    private void saveChunkClaims() {
        var config = plugin.getConfig();
        
        // Clear existing claims
        config.set("chunkloader.claims", null);
        
        // Save each player's chunks
        for (Map.Entry<UUID, Set<ChunkKey>> entry : playerChunks.entrySet()) {
            List<String> chunkStrings = new ArrayList<>();
            for (ChunkKey key : entry.getValue()) {
                chunkStrings.add(key.toString());
            }
            config.set("chunkloader.claims." + entry.getKey().toString(), chunkStrings);
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Shutdown the feature.
     */
    public void shutdown() {
        if (validationTask != null) {
            validationTask.cancel();
        }
        
        // Remove all chunk tickets
        for (ChunkKey key : loadedChunks) {
            World world = plugin.getServer().getWorld(key.worldName());
            if (world != null) {
                world.removePluginChunkTicket(key.x(), key.z(), plugin);
            }
        }
        
        // Save any pending changes
        saveChunkClaims();
    }
    
    // ==================== RESULT ENUM ====================
    
    public enum ClaimResult {
        SUCCESS("&aChunk claimed successfully!"),
        ALREADY_CLAIMED("&eYou already own this chunk."),
        CLAIMED_BY_OTHER("&cThis chunk is owned by another player."),
        AT_LIMIT("&cYou've reached your chunk limit!"),
        FEATURE_DISABLED("&cChunk loader is disabled.");
        
        private final String message;
        
        ClaimResult(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
    
    // ==================== CHUNK KEY RECORD ====================
    
    /**
     * Immutable key for identifying a chunk across server restarts.
     */
    public record ChunkKey(String worldName, int x, int z) {
        
        public ChunkKey(Chunk chunk) {
            this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        }
        
        public ChunkKey(Location loc) {
            this(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        }
        
        @Override
        public String toString() {
            return worldName + ":" + x + ":" + z;
        }
        
        public static ChunkKey fromString(String str) {
            String[] parts = str.split(":");
            if (parts.length != 3) return null;
            try {
                return new ChunkKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}

