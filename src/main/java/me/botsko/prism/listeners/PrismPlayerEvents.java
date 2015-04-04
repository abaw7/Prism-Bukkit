package me.botsko.prism.listeners;

import java.lang.NoSuchFieldError;
import me.botsko.prism.wands.ProfileWand;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.block.Jukebox;
import org.bukkit.Location;
import me.botsko.prism.actions.BlockAction;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import me.botsko.prism.wands.Wand;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import me.botsko.prism.actions.Handler;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import java.lang.Integer;
import org.bukkit.enchantments.Enchantment;
import java.util.Map;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.botsko.prism.players.PlayerIdentification;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.entity.Player;
import java.lang.StringBuilder;
import java.lang.Object;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.lang.String;
import java.util.List;
import me.botsko.prism.Prism;
import org.bukkit.event.Listener;

public class PrismPlayerEvents implements Listener
{
    private final Prism plugin;
    private final List<String> illegalCommands;
    private final List<String> ignoreCommands;
    
    public PrismPlayerEvents(final Prism plugin) {
        super();
        this.plugin = plugin;
        this.illegalCommands = (List<String>)plugin.getConfig().getList("prism.alerts.illegal-commands.commands");
        this.ignoreCommands = (List<String>)plugin.getConfig().getList("prism.do-not-track.commands");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String cmd = event.getMessage().toLowerCase();
        final String[] cmdArgs = cmd.split(" ");
        final String primaryCmd = cmdArgs[0].substring(1);
        if (this.plugin.getConfig().getBoolean("prism.alerts.illegal-commands.enabled") && this.illegalCommands.contains(primaryCmd)) {
            final String msg = player.getName() + " attempted an illegal command: " + primaryCmd + ". Originally: " + cmd;
            player.sendMessage(Prism.messenger.playerError("Sorry, this command is not available in-game."));
            this.plugin.alertPlayers(null, msg);
            event.setCancelled(true);
            if (this.plugin.getConfig().getBoolean("prism.alerts.illegal-commands.log-to-console")) {
                Prism.log(msg);
            }
            final List<String> commands = (List<String>)this.plugin.getConfig().getStringList("prism.alerts.illegal-commands.log-commands");
            MiscUtils.dispatchAlert(msg, commands);
        }
        if (!Prism.getIgnore().event("player-command", player)) {
            return;
        }
        if (this.ignoreCommands.contains(primaryCmd)) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createPlayer("player-command", player, event.getMessage()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        PlayerIdentification.cachePrismPlayer(player);
        if (!Prism.getIgnore().event("player-join", player)) {
            return;
        }
        String ip = null;
        if (this.plugin.getConfig().getBoolean("prism.track-player-ip-on-join")) {
            ip = player.getAddress().getAddress().getHostAddress().toString();
        }
        RecordingQueue.addToQueue(ActionFactory.createPlayer("player-join", event.getPlayer(), ip));
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Prism.prismPlayers.remove(event.getPlayer().getName());
        if (!Prism.getIgnore().event("player-quit", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createPlayer("player-quit", event.getPlayer(), null));
        if (Prism.playersWithActiveTools.containsKey(event.getPlayer().getName())) {
            Prism.playersWithActiveTools.remove(event.getPlayer().getName());
        }
        if (this.plugin.playerActivePreviews.containsKey(event.getPlayer().getName())) {
            this.plugin.playerActivePreviews.remove(event.getPlayer().getName());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        if (!Prism.getIgnore().event("player-chat", event.getPlayer())) {
            return;
        }
        if (this.plugin.dependencyEnabled("Herochat")) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createPlayer("player-chat", event.getPlayer(), event.getMessage()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (!Prism.getIgnore().event("item-drop", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", event.getItemDrop().getItemStack(), event.getItemDrop().getItemStack().getAmount(), -1, null, event.getPlayer().getLocation(), event.getPlayer().getName()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
        if (!Prism.getIgnore().event("item-pickup", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createItemStack("item-pickup", event.getItem().getItemStack(), event.getItem().getItemStack().getAmount(), -1, null, event.getPlayer().getLocation(), event.getPlayer().getName()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerExpChangeEvent(final PlayerExpChangeEvent event) {
        if (!Prism.getIgnore().event("xp-pickup", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createPlayer("xp-pickup", event.getPlayer(), "" + event.getAmount()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
        final Player player = event.getPlayer();
        final String cause = (event.getBucket() == Material.LAVA_BUCKET) ? "lava-bucket" : "water-bucket";
        if (!Prism.getIgnore().event(cause, player)) {
            return;
        }
        final Block spot = event.getBlockClicked().getRelative(event.getBlockFace());
        final int newId = cause.equals("lava-bucket") ? 11 : 9;
        RecordingQueue.addToQueue(ActionFactory.createBlockChange(cause, spot.getLocation(), spot.getTypeId(), spot.getData(), newId, (byte)0, player.getName()));
        if (this.plugin.getConfig().getBoolean("prism.alerts.uses.lava") && event.getBucket() == Material.LAVA_BUCKET && !player.hasPermission("prism.alerts.use.lavabucket.ignore") && !player.hasPermission("prism.alerts.ignore")) {
            this.plugin.useMonitor.alertOnItemUse(player, "poured lava");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
        final Player player = event.getPlayer();
        if (!Prism.getIgnore().event("bucket-fill", player)) {
            return;
        }
        final Block spot = event.getBlockClicked().getRelative(event.getBlockFace());
        String liquid_type = "milk";
        if (spot.getTypeId() == 8 || spot.getTypeId() == 9) {
            liquid_type = "water";
        }
        else if (spot.getTypeId() == 10 || spot.getTypeId() == 11) {
            liquid_type = "lava";
        }
        final Handler pa = ActionFactory.createPlayer("bucket-fill", player, liquid_type);
        pa.setX(spot.getX());
        pa.setY(spot.getY());
        pa.setZ(spot.getZ());
        RecordingQueue.addToQueue(pa);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (!Prism.getIgnore().event("player-teleport", event.getPlayer())) {
            return;
        }
        final PlayerTeleportEvent.TeleportCause c = event.getCause();
        if (c.equals((Object)PlayerTeleportEvent.TeleportCause.END_PORTAL) || c.equals((Object)PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) || c.equals((Object)PlayerTeleportEvent.TeleportCause.ENDER_PEARL)) {
            RecordingQueue.addToQueue(ActionFactory.createEntityTravel("player-teleport", (Entity)event.getPlayer(), event.getFrom(), event.getTo(), event.getCause()));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(final EnchantItemEvent event) {
        if (!Prism.getIgnore().event("enchant-item", event.getEnchanter())) {
            return;
        }
        final Player player = event.getEnchanter();
        RecordingQueue.addToQueue(ActionFactory.createItemStack("enchant-item", event.getItem(), event.getEnchantsToAdd(), event.getEnchantBlock().getLocation(), player.getName()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(final CraftItemEvent event) {
        final Player player = (Player)event.getWhoClicked();
        if (!Prism.getIgnore().event("craft-item", player)) {
            return;
        }
        final ItemStack item = event.getRecipe().getResult();
        RecordingQueue.addToQueue(ActionFactory.createItemStack("craft-item", item, 1, -1, null, player.getLocation(), player.getName()));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (Prism.playersWithActiveTools.containsKey(player.getName())) {
            final Wand wand = Prism.playersWithActiveTools.get(player.getName());
            final int item_id = wand.getItemId();
            final byte item_subid = wand.getItemSubId();
            if (wand != null && player.getItemInHand().getTypeId() == item_id && player.getItemInHand().getDurability() == item_subid) {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    wand.playerLeftClick(player, block.getLocation());
                }
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    block = block.getRelative(event.getBlockFace());
                    wand.playerRightClick(player, block.getLocation());
                }
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    Prism.debug("Cancelling event for wand use.");
                    event.setCancelled(true);
                    player.updateInventory();
                    return;
                }
            }
        }
        if (event.isCancelled()) {
            return;
        }
        if (block != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            switch (block.getType()) {
                case JUKEBOX: {
                    this.recordDiscInsert(block, player);
                    break;
                }
                case CAKE_BLOCK: {
                    this.recordCakeEat(block, player);
                    break;
                }
                case WOODEN_DOOR:
                case TRAP_DOOR:
                case FENCE_GATE:
                case LEVER:
                case STONE_BUTTON:
                case WOOD_BUTTON: {
                    if (!Prism.getIgnore().event("block-use", player)) {
                        return;
                    }
                    RecordingQueue.addToQueue(ActionFactory.createBlock("block-use", block, player.getName()));
                    break;
                }
                case LOG: {
                    this.recordCocoaPlantEvent(block, player.getItemInHand(), event.getBlockFace(), player.getName());
                    break;
                }
                case CROPS:
                case GRASS:
                case MELON_STEM:
                case PUMPKIN_STEM:
                case SAPLING:
                case CARROT:
                case POTATO: {
                    this.recordBonemealEvent(block, player.getItemInHand(), event.getBlockFace(), player.getName());
                    break;
                }
                case RAILS:
                case DETECTOR_RAIL:
                case POWERED_RAIL:
                case ACTIVATOR_RAIL: {
                    final String coord_key = block.getX() + ":" + block.getY() + ":" + block.getZ();
                    this.plugin.preplannedVehiclePlacement.put(coord_key, player.getName());
                    break;
                }
                case TNT: {
                    if (!player.getItemInHand().getType().equals((Object)Material.FLINT_AND_STEEL)) {
                        break;
                    }
                    if (!Prism.getIgnore().event("tnt-prime", player)) {
                        return;
                    }
                    RecordingQueue.addToQueue(ActionFactory.createUse("tnt-prime", "tnt", block, player.getName()));
                    break;
                }
            }
            if (player.getItemInHand().getType().equals((Object)Material.MONSTER_EGG)) {
                this.recordMonsterEggUse(block, player.getItemInHand(), player.getName());
            }
            if (player.getItemInHand().getType().equals((Object)Material.FIREWORK)) {
                this.recordRocketLaunch(block, player.getItemInHand(), event.getBlockFace(), player.getName());
            }
            if (player.getItemInHand().getType().equals((Object)Material.BOAT)) {
                final String coord_key = block.getX() + ":" + (block.getY() + 1) + ":" + block.getZ();
                this.plugin.preplannedVehiclePlacement.put(coord_key, player.getName());
            }
        }
        if (block != null && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block above = block.getRelative(BlockFace.UP);
            if (above.getType().equals((Object)Material.FIRE)) {
                RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", above, player.getName()));
            }
        }
        if (!this.plugin.getConfig().getBoolean("prism.tracking.crop-trample")) {
            return;
        }
        if (block != null && event.getAction() == Action.PHYSICAL && block.getType() == Material.SOIL) {
            if (!Prism.getIgnore().event("crop=trample", player)) {
                return;
            }
            RecordingQueue.addToQueue(ActionFactory.createBlock("crop-trample", block.getRelative(BlockFace.UP), player.getName()));
        }
    }
    
    protected void recordCocoaPlantEvent(final Block block, final ItemStack inhand, final BlockFace clickedFace, final String player) {
        if (!Prism.getIgnore().event("block-place", block)) {
            return;
        }
        if (block.getType().equals((Object)Material.LOG) && block.getData() >= 3 && inhand.getTypeId() == 351 && inhand.getDurability() == 3) {
            final Location newLoc = block.getRelative(clickedFace).getLocation();
            final Block actualBlock = block.getWorld().getBlockAt(newLoc);
            final BlockAction action = new BlockAction();
            action.setActionType("block-place");
            action.setPlayerName(player);
            action.setX(actualBlock.getX());
            action.setY(actualBlock.getY());
            action.setZ(actualBlock.getZ());
            action.setWorldName(newLoc.getWorld().getName());
            action.setBlockId(127);
            action.setBlockSubId(1);
            RecordingQueue.addToQueue(action);
        }
    }
    
    protected void recordBonemealEvent(final Block block, final ItemStack inhand, final BlockFace clickedFace, final String player) {
        if (inhand.getTypeId() == 351 && inhand.getDurability() == 15) {
            if (!Prism.getIgnore().event("bonemeal-use", block)) {
                return;
            }
            RecordingQueue.addToQueue(ActionFactory.createUse("bonemeal-use", "bonemeal", block, player));
        }
    }
    
    protected void recordMonsterEggUse(final Block block, final ItemStack inhand, final String player) {
        if (!Prism.getIgnore().event("spawnegg-use", block)) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createUse("spawnegg-use", "monster egg", block, player));
    }
    
    protected void recordRocketLaunch(final Block block, final ItemStack inhand, final BlockFace clickedFace, final String player) {
        if (!Prism.getIgnore().event("firework-launch", block)) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createItemStack("firework-launch", inhand, null, block.getLocation(), player));
    }
    
    protected void recordCakeEat(final Block block, final Player player) {
        if (!Prism.getIgnore().event("cake-eat", block)) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createUse("cake-eat", "cake", block, player.getName()));
    }
    
    protected void recordDiscInsert(final Block block, final Player player) {
        if (!player.getItemInHand().getType().isRecord()) {
            return;
        }
        final Jukebox jukebox = (Jukebox)block.getState();
        if (!jukebox.getPlaying().equals((Object)Material.AIR)) {
            final ItemStack i = new ItemStack(jukebox.getPlaying(), 1);
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", i, i.getAmount(), 0, null, block.getLocation(), player.getName()));
        }
        else {
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-insert", player.getItemInHand(), 1, 0, null, block.getLocation(), player.getName()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEntityInteract(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final Entity entity = event.getRightClicked();
        if (Prism.playersWithActiveTools.containsKey(player.getName())) {
            final Wand wand = Prism.playersWithActiveTools.get(player.getName());
            if (wand != null && wand instanceof ProfileWand) {
                wand.playerRightClick(player, entity);
                event.setCancelled(true);
            }
        }
    }
}
