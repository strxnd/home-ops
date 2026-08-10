package dev.glitg.smp;

import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.events.death.ZPlayerEliminationEvent;
import com.zetaplugins.lifestealz.events.death.ZPlayerPvPDeathEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GLITG SMP's single owner for season-specific and contextual rules.  It uses
 * LifeStealZ's public death events rather than changing LifeStealZ internals.
 */
public final class SMPRules extends JavaPlugin implements Listener {
    private final MiniMessage mini = MiniMessage.miniMessage();
    private SeasonState state;
    private NamespacedKey legendaryType;
    private NamespacedKey legendaryId;
    private final Map<UUID, Map<UUID, Double>> dragonDamage = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        state = new SeasonState(this);
        legendaryType = new NamespacedKey(this, "legendary_type");
        legendaryId = new NamespacedKey(this, "legendary_id");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("smprules").setExecutor(this::command);
        getCommand("smprules").setTabCompleter((sender, command, alias, args) -> List.of());
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        applyWorldSettings();
        getLogger().info("GLITG SMP rules enabled; LifeStealZ API integration is active.");
    }

    @Override public void onDisable() { state.save(); }

    private void tick() {
        long now = System.currentTimeMillis();
        if (state.getLong("season.grace-until") > 0 && now >= state.getLong("season.grace-until")) {
            state.setLong("season.grace-until", 0); broadcast("<green>Global PvP grace period has ended.");
        }
        if (state.getLong("season.locator-until") > 0 && now >= state.getLong("season.locator-until")) {
            state.setLong("season.locator-until", 0); setLocator(false); broadcast("<yellow>Locator Bar event has ended.");
        }
        if (!state.getBoolean("season.end-open") && state.getLong("season.started-at") > 0
                && now >= state.getLong("season.started-at") + days("season.end-open-delay-days")) {
            state.setBoolean("season.end-open", true); broadcast("<light_purple>The End is now open.");
        }
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntitiesByClass(Item.class)) {
            Item item = (Item) entity;
            if (!isLegendary(item.getItemStack())) continue;
            long recovery = state.recoveryAt(item.getUniqueId());
            if (item.getLocation().getY() < world.getMinHeight() - 5 && recovery == 0) {
                state.recoveryAt(item.getUniqueId(), now);
            }
            if (recovery > 0 && now >= recovery) recover(item);
        }
    }

    private long minutes(String path) { return getConfig().getLong(path) * 60_000L; }
    private long hours(String path) { return getConfig().getLong(path) * 3_600_000L; }
    private long days(String path) { return getConfig().getLong(path) * 86_400_000L; }
    private void tell(Player player, String message) { player.sendMessage(mini.deserialize(getConfig().getString("messages.prefix") + message)); }
    private void broadcast(String message) { Bukkit.broadcast(mini.deserialize(getConfig().getString("messages.prefix") + message)); }
    private boolean bypass(Player player) { return player.hasPermission("smprules.bypass"); }
    private boolean active(long until) { return until > System.currentTimeMillis(); }
    private boolean grace() { return active(state.getLong("season.grace-until")); }
    private boolean protectedPlayer(Player player) { return active(state.protectionUntil(player.getUniqueId())); }
    private boolean tagged(Player player) { return active(state.combatUntil(player.getUniqueId())); }
    private int hearts(Player player) { return (int) Math.round(player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() / 2.0); }

    private void applyWorldSettings() {
        for (World world : Bukkit.getWorlds()) world.getWorldBorder().setSize(getConfig().getDouble("world.border-size"));
        if (state.getBoolean("season.locator-enabled")) setLocator(true);
    }
    private void setLocator(boolean on) {
        state.setBoolean("season.locator-enabled", on);
        for (World world : Bukkit.getWorlds()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey() + " run gamerule locatorBar " + on);
    }
    private void tag(Player first, Player second) {
        long until = System.currentTimeMillis() + getConfig().getLong("combat.tag-duration-seconds") * 1000L;
        state.combatUntil(first.getUniqueId(), until); state.combatUntil(second.getUniqueId(), until);
    }

    /* LifeStealZ owns health accounting; these public events adjust only the season floor. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void seasonHeartFloor(ZPlayerPvPDeathEvent event) {
        Player victim = event.getOriginalEvent().getEntity();
        int floor = switch (state.phase()) {
            case "WEEK1" -> getConfig().getInt("hearts.week1-minimum");
            case "WEEK2" -> getConfig().getInt("hearts.week2-minimum");
            default -> 0;
        };
        if (floor > 0 && hearts(victim) <= floor) {
            event.setHeartsToLose(0); event.setHeartsKillerGains(0); event.setKillerShouldGainHearts(false); event.setShouldDropHearts(false);
            tell(victim, "<yellow>You are at this week's heart floor; no heart was lost.");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void preventEarlyElimination(ZPlayerEliminationEvent event) {
        if (!"FINAL_DAY".equals(state.phase())) {
            event.setShouldBanPlayer(false); event.setShouldAnnounceElimination(false);
            getLogger().warning("Blocked an unexpected pre-Final-Day LifeStealZ elimination.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        if (grace() || protectedPlayer(victim) || protectedPlayer(attacker)) {
            event.setCancelled(true); tell(attacker, "<red>PvP is currently blocked by grace or post-death protection."); return;
        }
        tag(attacker, victim);
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (hasIllegalEnchant(weapon)) { event.setCancelled(true); tell(attacker, "<red>That item has an illegal enchantment."); return; }
        if (weapon.getType() == Material.MACE) event.setDamage(Math.min(event.getDamage(), getConfig().getDouble("damage-caps.mace")));
        if (event.getDamager() instanceof ExplosiveMinecart) event.setDamage(Math.min(event.getDamage(), getConfig().getDouble("damage-caps.tnt-minecart")));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void tntMinecartCap(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && isLegendary(item.getItemStack())) {
            event.setCancelled(true);
            state.recoveryAt(item.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("legendary.recovery-delay-seconds") * 1000L);
            return;
        }
    }
    private Player attacker(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    @EventHandler public void deathProtection(PlayerDeathEvent event) {
        Player player = event.getEntity();
        state.protectionUntil(player.getUniqueId(), System.currentTimeMillis() + minutes("season.post-death-protection-minutes"));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void hideWeekOneInvisibleDeaths(PlayerDeathEvent event) {
        if (!"WEEK1".equals(state.phase())) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (victim.hasPotionEffect(PotionEffectType.INVISIBILITY) || (killer != null && killer.hasPotionEffect(PotionEffectType.INVISIBILITY))) event.deathMessage(null);
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasArmor(player)) state.protectionUntil(player.getUniqueId(), 0);
        if (tagged(player) && getConfig().getString("combat.logoff-action").equalsIgnoreCase("KILL")) {
            player.setHealth(0.0);
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (tagged(player) && !bypass(player) && getConfig().getString("combat.logoff-action").equalsIgnoreCase("KILL")) {
            player.setHealth(0.0); getLogger().info(player.getName() + " was killed for combat logging.");
        }
    }
    @EventHandler(ignoreCancelled = true) public void armorChange(PlayerInventorySlotChangeEvent event) {
        if (event.getSlot() >= 36 && event.getSlot() <= 39 && isArmor(event.getNewItemStack())) state.protectionUntil(event.getPlayer().getUniqueId(), 0);
    }
    @EventHandler(ignoreCancelled = true) public void inventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack moving = event.getCurrentItem();
        if (isIllegalNetherite(moving) || isIllegalNetherite(event.getCursor())) {
            event.setCancelled(true); tell(player, "<red>Only the PDC-identified GLITG legendary chestplate may be used."); return;
        }
        if (event.getSlotType() == InventoryType.SlotType.ARMOR || (event.isShiftClick() && isArmor(moving))) {
            if (protectedPlayer(player) && isArmor(moving)) { state.protectionUntil(player.getUniqueId(), 0); tell(player, "<yellow>Protection ended because you equipped armour."); }
            if (tagged(player) && !bypass(player)) { event.setCancelled(true); tell(player, "<red>You cannot switch armour while combat tagged."); return; }
        }
        if (isLegendary(moving) && event.getClickedInventory() != null && !isPlayerInventory(event.getClickedInventory())) {
            event.setCancelled(true); tell(player, "<red>Legendary items cannot be stored in containers."); return;
        }
        ItemStack cursor = event.getCursor();
        if (isLegendary(cursor) && event.getClickedInventory() != null && !isPlayerInventory(event.getClickedInventory())) {
            event.setCancelled(true); tell(player, "<red>Legendary items cannot be stored in containers."); return;
        }
        if (!bypass(player) && wouldExceedKitLimit(player, cursor, event.isShiftClick() ? moving : null)) {
            event.setCancelled(true); tell(player, "<red>That exceeds the GLITG PvP kit limit.");
        }
    }
    @EventHandler(ignoreCancelled = true) public void inventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (isLegendary(event.getOldCursor()) && event.getInventory().getType() != InventoryType.CRAFTING) {
            event.setCancelled(true); tell(player, "<red>Legendary items cannot be stored in containers.");
        }
    }
    @EventHandler(ignoreCancelled = true) public void hopper(InventoryMoveItemEvent event) {
        if (isLegendary(event.getItem())) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) public void frame(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType().name().contains("ITEM_FRAME") && isLegendary(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true); tell(event.getPlayer(), "<red>Legendary items cannot be stored in item frames.");
        }
    }

    @EventHandler(ignoreCancelled = true) public void restrictCombatUse(PlayerInteractEvent event) {
        Player player = event.getPlayer(); ItemStack item = event.getItem();
        if (item == null) return;
        if (isBannedMaterial(item.getType())) { event.setCancelled(true); tell(player, "<red>This item is banned on GLITG SMP."); return; }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && isLegendary(item) && event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof org.bukkit.inventory.InventoryHolder) {
            event.setCancelled(true); tell(player, "<red>Legendary items cannot be stored in containers."); return;
        }
        if (tagged(player) && !bypass(player)) {
            if (item.getType() == Material.LAVA_BUCKET || item.getType().name().endsWith("_ICE")) {
                event.setCancelled(true); tell(player, "<red>That is blocked while combat tagged.");
            }
            if (item.getType() == Material.ELYTRA) { event.setCancelled(true); tell(player, "<red>Elytra is blocked while combat tagged."); }
        }
    }
    @EventHandler(ignoreCancelled = true) public void fillBucket(PlayerBucketFillEvent event) { if (tagged(event.getPlayer()) && !bypass(event.getPlayer())) { event.setCancelled(true); tell(event.getPlayer(), "<red>Bucket draining is blocked in combat."); } }
    @EventHandler(ignoreCancelled = true) public void emptyBucket(PlayerBucketEmptyEvent event) { if (tagged(event.getPlayer()) && event.getBucket() == Material.LAVA_BUCKET && !bypass(event.getPlayer())) { event.setCancelled(true); tell(event.getPlayer(), "<red>Lava is blocked in combat."); } }
    @EventHandler(ignoreCancelled = true) public void sponge(BlockBreakEvent event) { if (tagged(event.getPlayer()) && event.getBlock().getType() == Material.SPONGE && !bypass(event.getPlayer())) { event.setCancelled(true); tell(event.getPlayer(), "<red>Sponge draining is blocked in combat."); } }
    @EventHandler(ignoreCancelled = true) public void glide(EntityToggleGlideEvent event) { if (event.getEntity() instanceof Player player && event.isGliding() && tagged(player) && !bypass(player)) { event.setCancelled(true); tell(player, "<red>Elytra is blocked in combat."); } }
    @EventHandler(ignoreCancelled = true) public void riptide(PlayerRiptideEvent event) { if (tagged(event.getPlayer()) && !bypass(event.getPlayer())) { event.setCancelled(true); tell(event.getPlayer(), "<red>Riptide is blocked in combat."); } }
    @EventHandler(ignoreCancelled = true) public void pearl(PlayerTeleportEvent event) { if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void endPortal(PlayerPortalEvent event) {
        if (event.getTo() != null && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END && !state.getBoolean("season.end-open") && !bypass(event.getPlayer())) {
            event.setCancelled(true); tell(event.getPlayer(), "<red>The End is closed until the GLITG season opens it.");
        }
    }

    @EventHandler(ignoreCancelled = true) public void enchant(PrepareAnvilEvent event) { if (hasIllegalEnchant(event.getResult())) event.setResult(null); }
    @EventHandler(ignoreCancelled = true) public void smith(PrepareSmithingEvent event) { if (isIllegalNetherite(event.getResult()) || hasIllegalEnchant(event.getResult())) event.setResult(null); }
    @EventHandler(ignoreCancelled = true) public void enchantTable(EnchantItemEvent event) {
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            String key = entry.getKey().getKey().getKey(); int level = entry.getValue();
            if (key.equals("thorns") || key.equals("fire_aspect") || key.equals("punch") || key.equals("lunge") || (key.equals("protection") && level > 3) || (key.equals("sharpness") && level > 3) || (key.equals("power") && level > 4)) {
                event.setCancelled(true); tell(event.getEnchanter(), "<red>That enchantment is not allowed on GLITG SMP."); return;
            }
        }
    }
    @EventHandler(ignoreCancelled = true) public void potionConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem(); Player player = event.getPlayer();
        if (isHeart(item) && hearts(player) >= getConfig().getInt("hearts.crafted-use-threshold")) { event.setCancelled(true); tell(player, "<red>Crafted hearts cannot be used at 10 or more hearts."); return; }
        if (isBannedPotion(item)) { event.setCancelled(true); tell(player, "<red>That potion is banned."); return; }
        if (isStrengthTwo(item) && item.getItemMeta() instanceof PotionMeta meta) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, getConfig().getInt("potions.strength-ii-duration-seconds") * 20, 1), true);
            item.setItemMeta(meta);
        }
    }
    @EventHandler(ignoreCancelled = true) public void brew(BrewEvent event) {
        for (int slot = 0; slot < event.getContents().getSize(); slot++) {
            ItemStack item = event.getContents().getItem(slot);
            if (isBannedPotion(item)) event.getContents().setItem(slot, null);
            if (isStrengthTwo(item) && item.getItemMeta() instanceof PotionMeta meta) {
                meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, getConfig().getInt("potions.strength-ii-duration-seconds") * 20, 1), true); item.setItemMeta(meta); event.getContents().setItem(slot, item);
            }
        }
    }
    @EventHandler(ignoreCancelled = true) public void potionSplash(PotionSplashEvent event) { if (isBannedPotion(event.getPotion().getItem())) event.setCancelled(true); }

    @EventHandler(ignoreCancelled = true) public void prepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory(); ItemStack[] matrix = inv.getMatrix();
        if (isChestplateRecipe(matrix)) {
            if (state.getString("legendary.chestplate.id") != null) inv.setResult(null);
            else inv.setResult(newLegendary(Material.NETHERITE_CHESTPLATE, "chestplate", UUID.randomUUID().toString()));
            return;
        }
        ItemStack result = inv.getResult();
        if (result == null) return;
        if (result.getType() == Material.MACE) {
            if (state.getString("legendary.mace.id") != null) inv.setResult(null);
            else inv.setResult(newLegendary(Material.MACE, "mace", UUID.randomUUID().toString()));
        }
        if (isIllegalNetherite(result)) inv.setResult(null);
        if (hasIllegalEnchant(result)) inv.setResult(null);
    }
    @EventHandler(ignoreCancelled = true) public void craft(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return;
        if (event.isShiftClick() && (isLegendary(result) || result.getType() == Material.MACE)) { event.setCancelled(true); tell((Player) event.getWhoClicked(), "<red>Craft legendary items one at a time."); return; }
        if (isLegendary(result)) {
            String type = legendaryType(result);
            String statePath = "legendary." + type + ".id";
            if (state.getString(statePath) != null) { event.setCancelled(true); return; }
            state.setString(statePath, legendaryId(result));
            getLogger().info("Registered unique legendary " + type + " id=" + legendaryId(result));
        }
    }

    @EventHandler(ignoreCancelled = true) public void playerDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        if (isLegendary(item.getItemStack())) state.recoveryAt(item.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("legendary.recovery-delay-seconds") * 1000L);
    }
    @EventHandler(ignoreCancelled = true) public void pickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isLegendary(event.getItem().getItemStack())) {
            if (wouldExceedKitLimit(player, event.getItem().getItemStack(), null)) { event.setCancelled(true); tell(player, "<red>That exceeds the GLITG PvP kit limit."); return; }
            state.clearRecovery(event.getItem().getUniqueId());
        }
    }
    @EventHandler(ignoreCancelled = true) public void itemDespawn(ItemDespawnEvent event) {
        if (isLegendary(event.getEntity().getItemStack())) { event.setCancelled(true); state.recoveryAt(event.getEntity().getUniqueId(), System.currentTimeMillis()); }
    }
    @EventHandler(ignoreCancelled = true) public void itemSpawn(ItemSpawnEvent event) {
        if (isLegendary(event.getEntity().getItemStack())) state.recoveryAt(event.getEntity().getUniqueId(), System.currentTimeMillis() + getConfig().getLong("legendary.recovery-delay-seconds") * 1000L);
    }
    private void recover(Item item) {
        World end = Bukkit.getWorlds().stream().filter(world -> world.getEnvironment() == World.Environment.THE_END).findFirst().orElse(null);
        if (end == null) return; // Keep it alive until the End has first been initialized.
        item.teleport(new Location(end, 100.5, 52.0, 0.5)); item.setPickupDelay(0); state.clearRecovery(item.getUniqueId());
        broadcast("<light_purple>A legendary item was recovered at the End platform.");
    }

    @EventHandler(ignoreCancelled = true) public void dragonDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || state.getBoolean("dragon.egg-awarded")) return;
        Player player = attacker(event.getDamager()); if (player == null) return;
        dragonDamage.computeIfAbsent(dragon.getUniqueId(), ignored -> new HashMap<>()).merge(player.getUniqueId(), event.getFinalDamage(), Double::sum);
    }
    @EventHandler public void dragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || state.getBoolean("dragon.egg-awarded")) return;
        Map<UUID, Double> scores = dragonDamage.remove(dragon.getUniqueId()); if (scores == null || scores.isEmpty()) return;
        List<Map.Entry<UUID, Double>> board = new ArrayList<>(scores.entrySet());
        board.sort(Comparator.<Map.Entry<UUID, Double>>comparingDouble(Map.Entry::getValue).reversed().thenComparing(entry -> entry.getKey().toString()));
        StringBuilder log = new StringBuilder("First dragon damage leaderboard: ");
        for (Map.Entry<UUID, Double> entry : board) log.append(entry.getKey()).append('=').append(String.format("%.2f", entry.getValue())).append(' ');
        getLogger().info(log.toString());
        UUID winner = board.getFirst().getKey(); state.setBoolean("dragon.egg-awarded", true); state.setString("dragon.egg-owner", winner.toString());
        Player online = Bukkit.getPlayer(winner);
        if (online != null) { online.getInventory().addItem(new ItemStack(Material.DRAGON_EGG)); state.setBoolean("dragon.egg-delivered", true); }
        Bukkit.getScheduler().runTaskLater(this, () -> removeDragonEggDuplicates(dragon.getWorld()), 40L);
    }
    private void removeDragonEggDuplicates(World world) {
        for (Item item : world.getEntitiesByClass(Item.class)) if (item.getItemStack().getType() == Material.DRAGON_EGG) item.remove();
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) if (world.getBlockAt(0, y, 0).getType() == Material.DRAGON_EGG) world.getBlockAt(0, y, 0).setType(Material.AIR, false);
    }
    @EventHandler public void awardOfflineEgg(PlayerJoinEvent event) {
        String owner = state.getString("dragon.egg-owner");
        if (owner != null && owner.equals(event.getPlayer().getUniqueId().toString()) && !state.getBoolean("dragon.egg-delivered")) {
            event.getPlayer().getInventory().addItem(new ItemStack(Material.DRAGON_EGG)); state.setBoolean("dragon.egg-delivered", true);
        }
    }
    @EventHandler public void doubleBreeze(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Breeze)) return;
        int factor = getConfig().getInt("breeze-rods.multiplier");
        for (ItemStack drop : event.getDrops()) if (drop.getType() == Material.BREEZE_ROD) drop.setAmount(drop.getAmount() * factor);
    }

    private boolean isChestplateRecipe(ItemStack[] matrix) {
        if (matrix == null || matrix.length != 9) return false;
        int eggs = 0, diamonds = 0;
        for (ItemStack item : matrix) { if (item == null || item.getType() == Material.AIR) continue; if (item.getType() == Material.DRAGON_EGG) eggs += item.getAmount(); else if (item.getType() == Material.DIAMOND) diamonds += item.getAmount(); else return false; }
        return eggs == 1 && diamonds == 8;
    }
    private ItemStack newLegendary(Material material, String type, String id) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(legendaryType, PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(legendaryId, PersistentDataType.STRING, id);
        if (type.equals("chestplate")) meta.setUnbreakable(true);
        meta.displayName(mini.deserialize("<light_purple><bold>GLITG Legendary " + (type.equals("mace") ? "Mace" : "Netherite Chestplate") + "</bold></light_purple>"));
        item.setItemMeta(meta); return item;
    }
    private boolean isLegendary(ItemStack item) { return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(legendaryType, PersistentDataType.STRING) || isHeart(item); }
    private String legendaryType(ItemStack item) { return item.getItemMeta().getPersistentDataContainer().get(legendaryType, PersistentDataType.STRING); }
    private String legendaryId(ItemStack item) { return item.getItemMeta().getPersistentDataContainer().get(legendaryId, PersistentDataType.STRING); }
    private boolean isHeart(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || Bukkit.getPluginManager().getPlugin("LifeStealZ") == null) return false;
        try { String id = LifeStealZ.getAPI().getCustomItemID(item); return id != null && id.toLowerCase().contains("heart"); } catch (RuntimeException ignored) { return false; }
    }
    private boolean isIllegalNetherite(ItemStack item) {
        if (item == null) return false;
        return switch (item.getType()) {
            case NETHERITE_SWORD, NETHERITE_AXE, NETHERITE_HELMET, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> true;
            case NETHERITE_CHESTPLATE -> !"chestplate".equals(legendaryType(item));
            default -> false;
        };
    }
    private boolean hasIllegalEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getEnchantments().entrySet().stream().anyMatch(entry -> {
            String key = entry.getKey().getKey().getKey(); int level = entry.getValue();
            return key.equals("thorns") || key.equals("fire_aspect") || key.equals("punch") || key.equals("lunge") || (key.equals("protection") && level > 3) || (key.equals("sharpness") && level > 3) || (key.equals("power") && level > 4);
        });
    }
    private boolean isBannedMaterial(Material material) { return material == Material.END_CRYSTAL || material == Material.ENCHANTED_GOLDEN_APPLE || material == Material.TOTEM_OF_UNDYING || material == Material.TIPPED_ARROW || isIllegalNetherite(new ItemStack(material)); }
    private boolean isBannedPotion(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof PotionMeta meta) || !item.getType().name().contains("POTION")) return false;
        String type = String.valueOf(meta.getBasePotionType()).toLowerCase();
        return type.contains("poison") || type.contains("turtle") || type.contains("slow_falling") || type.contains("weakness") || type.contains("strong_swiftness") || type.contains("slowness");
    }
    private boolean isStrengthTwo(ItemStack item) { return item != null && item.getItemMeta() instanceof PotionMeta meta && String.valueOf(meta.getBasePotionType()).toLowerCase().contains("strong_strength"); }
    private boolean isArmor(ItemStack item) { return item != null && item.getType().name().matches(".*_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"); }
    private boolean hasArmor(Player player) { for (ItemStack item : player.getInventory().getArmorContents()) if (isArmor(item)) return true; return false; }
    private boolean isPlayerInventory(Inventory inventory) { return inventory.getType() == InventoryType.CRAFTING || inventory.getType() == InventoryType.PLAYER; }
    private boolean wouldExceedKitLimit(Player player, ItemStack incoming, ItemStack moved) {
        ItemStack candidate = incoming == null || incoming.getType().isAir() ? moved : incoming; if (candidate == null) return false;
        Material material = candidate.getType(); int existing = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) if (stack != null && sameKitKind(stack, candidate)) existing += stack.getAmount();
        int cap = switch (material) {
            case EXPERIENCE_BOTTLE -> getConfig().getInt("kit-limits.xp-bottle-stacks") * material.getMaxStackSize();
            case COBWEB -> getConfig().getInt("kit-limits.cobweb-stacks") * material.getMaxStackSize();
            case GOLDEN_APPLE -> getConfig().getInt("kit-limits.golden-apple-stacks") * material.getMaxStackSize();
            case BREEZE_ROD -> getConfig().getInt("kit-limits.breeze-rod-stacks") * material.getMaxStackSize();
            default -> isHealingPotion(candidate) ? getConfig().getInt("kit-limits.healing-potions") : Integer.MAX_VALUE;
        };
        return existing + candidate.getAmount() > cap;
    }
    private boolean sameKitKind(ItemStack stack, ItemStack candidate) { return isHealingPotion(candidate) ? isHealingPotion(stack) : stack.getType() == candidate.getType(); }
    private boolean isHealingPotion(ItemStack item) { return item != null && item.getItemMeta() instanceof PotionMeta meta && String.valueOf(meta.getBasePotionType()).toLowerCase().contains("healing"); }

    private boolean command(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("smprules.status")) return true;
            sender.sendMessage(mini.deserialize("<gold>GLITG SMP</gold> <gray>phase=" + state.phase() + ", grace=" + grace() + ", end=" + state.getBoolean("season.end-open") + ", locator=" + state.getBoolean("season.locator-enabled") + "</gray>")); return true;
        }
        if (!sender.hasPermission("smprules.season")) { sender.sendMessage(Component.text("No permission.")); return true; }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); state.reload(); applyWorldSettings(); sender.sendMessage(Component.text("SMPRules reloaded.")); return true; }
        long now = System.currentTimeMillis();
        if (args[0].equalsIgnoreCase("season") && args.length > 1) {
            switch (args[1].toLowerCase()) { case "start" -> { state.phase("WEEK1"); state.setLong("season.started-at", now); state.setLong("season.grace-until", now + minutes("season.grace-period-minutes")); } case "week1" -> state.phase("WEEK1"); case "week2" -> state.phase("WEEK2"); default -> { sender.sendMessage(Component.text("Use start, week1, or week2.")); return true; } }
        } else if (args[0].equalsIgnoreCase("finalday") && args.length > 1) state.phase(args[1].equalsIgnoreCase("on") ? "FINAL_DAY" : "WEEK2");
        else if (args[0].equalsIgnoreCase("grace") && args.length > 1) state.setLong("season.grace-until", args[1].equalsIgnoreCase("on") ? now + minutes("season.grace-period-minutes") : 0);
        else if (args[0].equalsIgnoreCase("end") && args.length > 1) state.setBoolean("season.end-open", args[1].equalsIgnoreCase("open"));
        else if (args[0].equalsIgnoreCase("locator") && args.length > 1) { boolean on = args[1].equalsIgnoreCase("on"); state.setLong("season.locator-until", on ? now + hours("season.locator-event-duration-hours") : 0); setLocator(on); }
        else if (args[0].equalsIgnoreCase("protection") && args.length > 2 && args[1].equalsIgnoreCase("remove")) { Player target = Bukkit.getPlayer(args[2]); if (target != null) state.protectionUntil(target.getUniqueId(), 0); }
        else { sender.sendMessage(Component.text("Unknown SMPRules command.")); return true; }
        sender.sendMessage(Component.text("GLITG SMP state updated.")); return true;
    }
}
