package net.ppekkungz.essentialUtils.listener;

import net.ppekkungz.essentialUtils.EssentialUtils;
import net.ppekkungz.essentialUtils.config.PluginConfig;
import net.ppekkungz.essentialUtils.features.chunkloader.ChunkLoaderFeature;
import net.ppekkungz.essentialUtils.features.farm.AutoFarmFeature;
import net.ppekkungz.essentialUtils.features.tree.TreeAssistFeature;
import net.ppekkungz.essentialUtils.features.vein.VeinMineFeature;
import net.ppekkungz.essentialUtils.indicator.ActionBarService;
import net.ppekkungz.essentialUtils.indicator.TabMenuService;
import net.ppekkungz.essentialUtils.state.PlayerState;
import net.ppekkungz.essentialUtils.state.StateManager;
import net.ppekkungz.essentialUtils.util.Materials;
import net.ppekkungz.essentialUtils.work.WorkItem;
import net.ppekkungz.essentialUtils.work.WorkService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Handles feature activation based on tool usage and sneaking.
 * 
 * Activation rules:
 * - Tree Feller: Crouch + break with axe
 * - VeinMiner: Break ore with pickaxe (always active)
 * - AutoFarm: Break crop with hoe (always active)
 * - Chunk Loader: Crouch + break crop with hoe to claim chunk
 */
public class ActivationListener implements Listener {
    private final EssentialUtils plugin;
    private final PluginConfig cfg;
    private final StateManager states;
    private final WorkService work;
    private final ActionBarService actionBar;
    private final ChunkLoaderFeature chunkLoader;
    private final TabMenuService tabMenu;

    private final TreeAssistFeature tree;
    private final VeinMineFeature vein;
    private final AutoFarmFeature farm;

    public ActivationListener(EssentialUtils plugin, PluginConfig cfg, StateManager states, 
                              WorkService work, ActionBarService actionBar,
                              ChunkLoaderFeature chunkLoader, TabMenuService tabMenu) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.states = states;
        this.work = work;
        this.actionBar = actionBar;
        this.chunkLoader = chunkLoader;
        this.tabMenu = tabMenu;

        this.tree = new TreeAssistFeature(cfg);
        this.vein = new VeinMineFeature(cfg);
        this.farm = new AutoFarmFeature(cfg);
    }

    // ==================== TOOL CHECKS ====================
    
    private boolean isAxe(ItemStack it) { 
        return it != null && it.getType().name().endsWith("_AXE"); 
    }
    
    private boolean isPick(ItemStack it) { 
        return it != null && it.getType().name().endsWith("_PICKAXE"); 
    }
    
    private boolean isHoe(ItemStack it) { 
        return it != null && it.getType().name().endsWith("_HOE"); 
    }

    // ==================== PLAYER JOIN/QUIT ====================

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        
        // Initialize tab menu for joining player
        if (tabMenu != null) {
            tabMenu.onPlayerJoin(p);
        }
    }

    // ==================== SNEAK HANDLING (Tree Feller Indicator) ====================
    
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        
        if (e.isSneaking()) {
            // Show Tree Feller indicator if holding axe and feature enabled
            if (cfg.treeFellerEnabled() && cfg.treeFellerShowIndicator() && isAxe(hand)) {
                actionBar.showPersistent(p, cfg.treeFellerActiveMessage());
            }
        } else {
            // Hide indicator when stopping sneaking
            if (actionBar.hasPersistent(p)) {
                actionBar.clearPersistent(p);
            }
        }
    }

    // ==================== BLOCK BREAK HANDLING ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        ItemStack hand = p.getInventory().getItemInMainHand();
        
        // Skip if player is already processing
        if (states.isActive(p)) {
            return;
        }

        // Try Tree Feller (requires crouching + axe)
        if (cfg.treeFellerEnabled() && p.isSneaking() && isAxe(hand)) {
            if (tree.canTrigger(p, b)) {
                handleTreeFeller(p, b, e);
                return;
            }
        }
        
        // Try VeinMiner (always active with pickaxe)
        if (cfg.veinMinerEnabled() && isPick(hand)) {
            if (vein.canTrigger(p, b)) {
                handleVeinMiner(p, b, e);
                return;
            }
        }
        
        // Try AutoFarm (always active with hoe)
        if (cfg.autoFarmEnabled() && isHoe(hand)) {
            if (farm.canTrigger(p, b)) {
                // Check for chunk claim while sneaking
                if (p.isSneaking() && cfg.chunkLoaderEnabled() && cfg.chunkLoaderClaimOnFarm()) {
                    handleChunkClaim(p, b);
                }
                
                handleAutoFarm(p, b, e);
                return;
            }
        }
    }

    // ==================== FEATURE HANDLERS ====================

    /**
     * Handle Tree Feller activation.
     */
    private void handleTreeFeller(Player p, Block origin, BlockBreakEvent e) {
        Set<Block> targets = tree.collectTargets(p, origin);
        
        if (targets.isEmpty()) {
            return; // Let normal break happen
        }
        
        // Cancel the original break event - we'll handle it
        e.setCancelled(true);
        
        // Find stump for replanting
        Block stump = findStump(targets);
        Material logType = origin.getType();
        Location stumpLocation = stump != null ? stump.getLocation() : origin.getLocation();
        
        // Start tracking
        states.startTreeFeller(p, logType, stumpLocation);
        states.set(p, PlayerState.ACTIVE);
        
        // Queue all blocks
        int idx = 0;
        for (Block tb : targets) {
            boolean isLeaf = tb.getType().name().endsWith("_LEAVES");
            int delay = idx; // Simple stagger
            
            if (isLeaf) {
                work.queue(p).add(WorkItem.breakLeaf(p, tb, delay));
            } else {
                work.queue(p).add(WorkItem.breakLog(p, tb, delay));
            }
            idx++;
        }
        
        // Queue sapling replant if enabled
        // Plant at stump position (where the bottom log was, now will be air)
        if (cfg.treeFellerReplant() && stump != null) {
            Material sapling = TreeAssistFeature.saplingForLog(logType);
            Block plantPos = stump; // Plant where the stump log was (on top of dirt/grass)
            
            // Delay replant to after tree is broken
            p.getScheduler().runDelayed(plugin, task -> {
                work.queue(p).add(WorkItem.plantSapling(p, plantPos, sapling, 10));
            }, null, 20L);
        }
        
        // Clear persistent indicator and start work loop
        actionBar.clearPersistent(p);
        work.ensureLoop(p);
    }

    /**
     * Handle VeinMiner activation.
     */
    private void handleVeinMiner(Player p, Block origin, BlockBreakEvent e) {
        Set<Block> targets = vein.collectTargets(p, origin);
        
        if (targets.isEmpty() || targets.size() == 1) {
            return; // Single ore, let normal break happen
        }
        
        // Cancel the original break event - we'll handle it
        e.setCancelled(true);
        
        // Start tracking
        states.startVeinMine(p, origin.getLocation(), origin.getType());
        states.set(p, PlayerState.ACTIVE);
        
        // Queue all ores
        int idx = 0;
        for (Block ore : targets) {
            int delay = idx / 4; // Break 4 per tick
            work.queue(p).add(WorkItem.breakOre(p, ore, delay));
            idx++;
        }
        
        work.ensureLoop(p);
    }

    /**
     * Handle AutoFarm activation.
     */
    private void handleAutoFarm(Player p, Block origin, BlockBreakEvent e) {
        Set<Block> targets = farm.collectTargets(p, origin);
        
        if (targets.isEmpty() || targets.size() == 1) {
            return; // Single crop, let normal break happen
        }
        
        // Cancel the original break event - we'll handle it
        e.setCancelled(true);
        
        states.set(p, PlayerState.ACTIVE);
        
        // Queue all crops
        for (Block crop : targets) {
            work.queue(p).add(WorkItem.breakCrop(p, crop));
        }
        
        work.ensureLoop(p);
    }

    /**
     * Handle chunk claim when sneaking + farming.
     */
    private void handleChunkClaim(Player p, Block origin) {
        if (chunkLoader == null) return;
        
        // Try to claim the chunk
        ChunkLoaderFeature.ClaimResult result = chunkLoader.claimChunk(p, origin.getChunk());
        
        // Show feedback
        if (cfg.chunkLoaderShowClaimMessage()) {
            if (result.isSuccess()) {
                int current = chunkLoader.getClaimedCount(p);
                int max = chunkLoader.getMaxChunks();
                String msg = cfg.chunkLoaderClaimMessage()
                    .replace("{current}", String.valueOf(current))
                    .replace("{max}", String.valueOf(max));
                actionBar.showTimed(p, msg, 60);
            } else if (result == ChunkLoaderFeature.ClaimResult.AT_LIMIT) {
                // Only notify if they hit the limit (don't spam for already claimed)
                actionBar.showTimed(p, result.getMessage(), 40);
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Find the lowest log block (stump) from a set of blocks.
     */
    private Block findStump(Set<Block> logs) {
        Block best = null;
        int bestY = Integer.MAX_VALUE;
        
        for (Block b : logs) {
            // Only consider logs, not leaves
            if (Materials.isLog(b.getType(), true)) {
                if (b.getY() < bestY) {
                    bestY = b.getY();
                    best = b;
                }
            }
        }
        return best;
    }

    // ==================== CLEANUP ====================

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        actionBar.cleanup(p);
        work.stopLoop(p);
        states.reset(p);
    }
}
