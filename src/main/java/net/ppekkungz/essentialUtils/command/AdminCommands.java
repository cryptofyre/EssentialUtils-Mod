package net.ppekkungz.essentialUtils.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.ppekkungz.essentialUtils.EssentialUtils;
import net.ppekkungz.essentialUtils.config.PluginConfig;
import net.ppekkungz.essentialUtils.features.chunkloader.ChunkLoaderFeature;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Admin commands for EssentialUtils using Brigadier.
 * Supports enable/disable modules, status check, chunk management, and config reload.
 */
@SuppressWarnings("UnstableApiUsage")
public class AdminCommands {
    private final EssentialUtils plugin;
    
    private static final List<String> MODULES = Arrays.asList(
        "treefeller", "veinminer", "autofarm", "chunkloader", "tabmenu"
    );

    public AdminCommands(EssentialUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Register commands using Paper's Brigadier API.
     */
    public void register() {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            
            // Register /eutils command
            commands.register(
                buildCommand("eutils").build(),
                "EssentialUtils admin commands",
                List.of("eu", "essentialutils")
            );
            
            // Register /chunk command for chunk loader
            commands.register(
                buildChunkCommand("chunk").build(),
                "Manage your loaded chunks",
                List.of("chunks", "farmchunk")
            );
        });
    }

    /**
     * Build the main command tree.
     */
    private LiteralArgumentBuilder<CommandSourceStack> buildCommand(String name) {
        return Commands.literal(name)
            // /eutils (no args - show help)
            .executes(this::showHelp)
            
            // /eutils status
            .then(Commands.literal("status")
                .requires(source -> source.getSender().hasPermission("essentialutils.admin"))
                .executes(this::showStatus))
            
            // /eutils reload
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("essentialutils.admin"))
                .executes(this::reloadConfig))
            
            // /eutils enable <module>
            .then(Commands.literal("enable")
                .requires(source -> source.getSender().hasPermission("essentialutils.admin"))
                .then(Commands.argument("module", StringArgumentType.word())
                    .suggests(this::suggestModules)
                    .executes(this::enableModule)))
            
            // /eutils disable <module>
            .then(Commands.literal("disable")
                .requires(source -> source.getSender().hasPermission("essentialutils.admin"))
                .then(Commands.argument("module", StringArgumentType.word())
                    .suggests(this::suggestModules)
                    .executes(this::disableModule)));
    }

    /**
     * Build the chunk loader command tree.
     */
    private LiteralArgumentBuilder<CommandSourceStack> buildChunkCommand(String name) {
        return Commands.literal(name)
            // /chunk (no args - show help)
            .executes(this::showChunkHelp)
            
            // /chunk claim
            .then(Commands.literal("claim")
                .requires(source -> source.getSender() instanceof Player 
                    && source.getSender().hasPermission("essentialutils.chunkloader"))
                .executes(this::claimChunk))
            
            // /chunk unclaim
            .then(Commands.literal("unclaim")
                .requires(source -> source.getSender() instanceof Player 
                    && source.getSender().hasPermission("essentialutils.chunkloader"))
                .executes(this::unclaimChunk))
            
            // /chunk list
            .then(Commands.literal("list")
                .requires(source -> source.getSender() instanceof Player)
                .executes(this::listChunks))
            
            // /chunk info
            .then(Commands.literal("info")
                .executes(this::chunkInfo));
    }

    /**
     * Suggest module names for tab completion.
     */
    private CompletableFuture<Suggestions> suggestModules(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        MODULES.stream()
            .filter(m -> m.startsWith(input))
            .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage("§6§l[EssentialUtils] §fCommands:");
        sender.sendMessage("");
        sender.sendMessage("  §e/eutils enable <module> §7- Enable a module");
        sender.sendMessage("  §e/eutils disable <module> §7- Disable a module");
        sender.sendMessage("  §e/eutils status §7- View module status");
        sender.sendMessage("  §e/eutils reload §7- Reload configuration");
        sender.sendMessage("");
        sender.sendMessage("  §e/chunk claim §7- Claim current chunk");
        sender.sendMessage("  §e/chunk unclaim §7- Unclaim current chunk");
        sender.sendMessage("  §e/chunk list §7- List your claimed chunks");
        sender.sendMessage("");
        sender.sendMessage("§7Modules: treefeller, veinminer, autofarm, chunkloader, tabmenu");
        return Command.SINGLE_SUCCESS;
    }

    private int showChunkHelp(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        PluginConfig cfg = plugin.cfg();
        
        sender.sendMessage("§6§l[Chunk Loader] §fCommands:");
        sender.sendMessage("");
        sender.sendMessage("  §e/chunk claim §7- Claim your current chunk");
        sender.sendMessage("  §e/chunk unclaim §7- Unclaim your current chunk");
        sender.sendMessage("  §e/chunk list §7- List your claimed chunks");
        sender.sendMessage("  §e/chunk info §7- View chunk info");
        sender.sendMessage("");
        sender.sendMessage("§7Max chunks per player: §f" + cfg.chunkLoaderMaxChunksPerPlayer());
        sender.sendMessage("§7Tip: Sneak + harvest crops to auto-claim!");
        return Command.SINGLE_SUCCESS;
    }

    private int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        PluginConfig cfg = plugin.cfg();
        
        sender.sendMessage("§6§l[EssentialUtils] §fModule Status:");
        sender.sendMessage("");
        
        // Tree Feller
        String treeStatus = cfg.treeFellerEnabled() ? "§a✓ Enabled" : "§c✗ Disabled";
        sender.sendMessage("  §f⚒ Tree Feller: " + treeStatus);
        if (cfg.treeFellerEnabled()) {
            sender.sendMessage("    §7├ Max blocks: §f" + cfg.treeFellerMaxBlocks());
            sender.sendMessage("    §7├ Replant: " + (cfg.treeFellerReplant() ? "§aYes" : "§cNo"));
            sender.sendMessage("    §7└ Particles: " + (cfg.treeFellerParticles() ? "§aYes" : "§cNo"));
        }
        
        // Vein Miner
        String veinStatus = cfg.veinMinerEnabled() ? "§a✓ Enabled" : "§c✗ Disabled";
        sender.sendMessage("  §b⛏ Vein Miner: " + veinStatus);
        if (cfg.veinMinerEnabled()) {
            sender.sendMessage("    §7├ Max ores: §f" + cfg.veinMinerMaxOres());
            sender.sendMessage("    §7├ Fortune: " + (cfg.veinMinerFortuneEnabled() ? "§aYes" : "§cNo"));
            sender.sendMessage("    §7└ Silk Touch: " + (cfg.veinMinerSilkTouchDropsOre() ? "§aDrops ore" : "§cDrops resources"));
        }
        
        // Auto Farm
        String farmStatus = cfg.autoFarmEnabled() ? "§a✓ Enabled" : "§c✗ Disabled";
        sender.sendMessage("  §e🌾 Auto Farm: " + farmStatus);
        if (cfg.autoFarmEnabled()) {
            sender.sendMessage("    §7├ Radius: §f" + cfg.autoFarmRadius());
            sender.sendMessage("    §7└ Replant: " + (cfg.autoFarmReplant() ? "§aYes" : "§cNo"));
        }
        
        // Chunk Loader
        String chunkStatus = cfg.chunkLoaderEnabled() ? "§a✓ Enabled" : "§c✗ Disabled";
        sender.sendMessage("  §d📦 Chunk Loader: " + chunkStatus);
        if (cfg.chunkLoaderEnabled()) {
            var chunkLoader = plugin.chunkLoader();
            sender.sendMessage("    §7├ Max chunks/player: §f" + cfg.chunkLoaderMaxChunksPerPlayer());
            sender.sendMessage("    §7├ Auto-claim on farm: " + (cfg.chunkLoaderClaimOnFarm() ? "§aYes" : "§cNo"));
            if (chunkLoader != null) {
                sender.sendMessage("    §7└ Total loaded: §f" + chunkLoader.getTotalLoadedChunks());
            }
        }
        
        // Tab Menu
        String tabStatus = cfg.tabMenuEnabled() ? "§a✓ Enabled" : "§c✗ Disabled";
        sender.sendMessage("  §6📋 Tab Menu: " + tabStatus);
        if (cfg.tabMenuEnabled()) {
            sender.sendMessage("    §7├ Server IP: §f" + cfg.tabMenuServerIp());
            sender.sendMessage("    §7└ Update interval: §f" + cfg.tabMenuUpdateInterval() + " ticks");
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7Performance: §f" + cfg.blocksPerTick() + " blocks/tick");
        
        return Command.SINGLE_SUCCESS;
    }

    private int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        plugin.reloadConfig();
        plugin.loadPluginConfig();
        sender.sendMessage("§a[EssentialUtils] §fConfiguration reloaded!");
        return Command.SINGLE_SUCCESS;
    }

    private int enableModule(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String module = StringArgumentType.getString(context, "module").toLowerCase();
        PluginConfig cfg = plugin.cfg();
        
        switch (module) {
            case "treefeller", "tree" -> {
                cfg.setTreeFellerEnabled(true);
                sender.sendMessage("§a[EssentialUtils] §fTree Feller §aenabled!");
            }
            case "veinminer", "vein" -> {
                cfg.setVeinMinerEnabled(true);
                sender.sendMessage("§a[EssentialUtils] §fVein Miner §aenabled!");
            }
            case "autofarm", "farm" -> {
                cfg.setAutoFarmEnabled(true);
                sender.sendMessage("§a[EssentialUtils] §fAuto Farm §aenabled!");
            }
            case "chunkloader", "chunk" -> {
                cfg.setChunkLoaderEnabled(true);
                sender.sendMessage("§a[EssentialUtils] §fChunk Loader §aenabled!");
                sender.sendMessage("§7Note: Reload plugin for full effect.");
            }
            case "tabmenu", "tab" -> {
                cfg.setTabMenuEnabled(true);
                sender.sendMessage("§a[EssentialUtils] §fTab Menu §aenabled!");
                sender.sendMessage("§7Note: Reload plugin for full effect.");
            }
            default -> {
                sender.sendMessage("§c[EssentialUtils] Unknown module: §f" + module);
                sender.sendMessage("§7Valid modules: treefeller, veinminer, autofarm, chunkloader, tabmenu");
                return Command.SINGLE_SUCCESS;
            }
        }
        
        plugin.saveConfig();
        return Command.SINGLE_SUCCESS;
    }

    private int disableModule(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String module = StringArgumentType.getString(context, "module").toLowerCase();
        PluginConfig cfg = plugin.cfg();
        
        switch (module) {
            case "treefeller", "tree" -> {
                cfg.setTreeFellerEnabled(false);
                sender.sendMessage("§c[EssentialUtils] §fTree Feller §cdisabled!");
            }
            case "veinminer", "vein" -> {
                cfg.setVeinMinerEnabled(false);
                sender.sendMessage("§c[EssentialUtils] §fVein Miner §cdisabled!");
            }
            case "autofarm", "farm" -> {
                cfg.setAutoFarmEnabled(false);
                sender.sendMessage("§c[EssentialUtils] §fAuto Farm §cdisabled!");
            }
            case "chunkloader", "chunk" -> {
                cfg.setChunkLoaderEnabled(false);
                sender.sendMessage("§c[EssentialUtils] §fChunk Loader §cdisabled!");
                sender.sendMessage("§7Note: Reload plugin for full effect.");
            }
            case "tabmenu", "tab" -> {
                cfg.setTabMenuEnabled(false);
                sender.sendMessage("§c[EssentialUtils] §fTab Menu §cdisabled!");
                sender.sendMessage("§7Note: Reload plugin for full effect.");
            }
            default -> {
                sender.sendMessage("§c[EssentialUtils] Unknown module: §f" + module);
                sender.sendMessage("§7Valid modules: treefeller, veinminer, autofarm, chunkloader, tabmenu");
                return Command.SINGLE_SUCCESS;
            }
        }
        
        plugin.saveConfig();
        return Command.SINGLE_SUCCESS;
    }

    // ==================== CHUNK COMMANDS ====================

    private int claimChunk(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        var chunkLoader = plugin.chunkLoader();
        
        if (chunkLoader == null || !plugin.cfg().chunkLoaderEnabled()) {
            player.sendMessage("§c[Chunk Loader] §fThis feature is disabled.");
            return Command.SINGLE_SUCCESS;
        }
        
        ChunkLoaderFeature.ClaimResult result = chunkLoader.claimChunk(player, player.getChunk());
        
        if (result.isSuccess()) {
            int current = chunkLoader.getClaimedCount(player);
            int max = chunkLoader.getMaxChunks();
            player.sendMessage("§a[Chunk Loader] §fChunk claimed! §7(" + current + "/" + max + ")");
            player.sendMessage("§7Chunk at §f" + player.getChunk().getX() + ", " + player.getChunk().getZ() + 
                              " §7will stay loaded.");
        } else {
            player.sendMessage(result.getMessage().replace("&", "§"));
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private int unclaimChunk(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        var chunkLoader = plugin.chunkLoader();
        
        if (chunkLoader == null || !plugin.cfg().chunkLoaderEnabled()) {
            player.sendMessage("§c[Chunk Loader] §fThis feature is disabled.");
            return Command.SINGLE_SUCCESS;
        }
        
        boolean success = chunkLoader.unclaimChunk(player, player.getChunk());
        
        if (success) {
            player.sendMessage("§e[Chunk Loader] §fChunk unclaimed.");
        } else {
            player.sendMessage("§c[Chunk Loader] §fYou don't own this chunk.");
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private int listChunks(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getSender();
        var chunkLoader = plugin.chunkLoader();
        
        if (chunkLoader == null || !plugin.cfg().chunkLoaderEnabled()) {
            player.sendMessage("§c[Chunk Loader] §fThis feature is disabled.");
            return Command.SINGLE_SUCCESS;
        }
        
        var chunks = chunkLoader.getPlayerChunks(player.getUniqueId());
        int max = chunkLoader.getMaxChunks();
        
        player.sendMessage("§6[Chunk Loader] §fYour Claimed Chunks §7(" + chunks.size() + "/" + max + "):");
        
        if (chunks.isEmpty()) {
            player.sendMessage("  §7No chunks claimed yet!");
            player.sendMessage("  §7Use §e/chunk claim §7or sneak + farm to claim.");
        } else {
            int i = 1;
            for (var key : chunks) {
                player.sendMessage("  §7" + i + ". §f" + key.worldName() + " §7@ §f" + key.x() + ", " + key.z());
                i++;
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private int chunkInfo(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[Chunk Loader] §fYou must be a player!");
            return Command.SINGLE_SUCCESS;
        }
        
        var chunkLoader = plugin.chunkLoader();
        var chunk = player.getChunk();
        
        player.sendMessage("§6[Chunk Loader] §fChunk Info:");
        player.sendMessage("  §7World: §f" + chunk.getWorld().getName());
        player.sendMessage("  §7Coordinates: §f" + chunk.getX() + ", " + chunk.getZ());
        player.sendMessage("  §7Block range: §f" + (chunk.getX() * 16) + " to " + (chunk.getX() * 16 + 15) + 
                          ", " + (chunk.getZ() * 16) + " to " + (chunk.getZ() * 16 + 15));
        
        if (chunkLoader != null && plugin.cfg().chunkLoaderEnabled()) {
            boolean claimed = chunkLoader.isClaimed(chunk);
            boolean ownedByYou = chunkLoader.isClaimedBy(player, chunk);
            
            if (ownedByYou) {
                player.sendMessage("  §7Status: §aOwned by you");
            } else if (claimed) {
                player.sendMessage("  §7Status: §eOwned by another player");
            } else {
                player.sendMessage("  §7Status: §7Unclaimed");
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
}
