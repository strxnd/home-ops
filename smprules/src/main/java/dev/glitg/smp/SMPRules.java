package dev.glitg.smp;

import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.events.death.ZPlayerEliminationEvent;
import com.zetaplugins.lifestealz.events.death.ZPlayerPvPDeathEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Small compatibility layer for rules not safely owned by a 26.2-compatible plugin. */
public final class SMPRules extends JavaPlugin implements Listener {
    private SeasonState state;
    private NamespacedKey legendaryType;
    private NamespacedKey legendaryId;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        state = new SeasonState(this);
        legendaryType = new NamespacedKey(this, "legendary_type");
        legendaryId = new NamespacedKey(this, "legendary_id");
        ShapedRecipe chestplateRecipe = new ShapedRecipe(new NamespacedKey(this, "legendary_netherite_chestplate"), legendaryChestplate());
        chestplateRecipe.shape("DDD", "DED", "DDD");
        chestplateRecipe.setIngredient('D', Material.DIAMOND);
        chestplateRecipe.setIngredient('E', Material.DRAGON_EGG);
        Bukkit.addRecipe(chestplateRecipe);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("smprules").setExecutor((sender, command, label, args) -> command(sender, args));
        applyWorldSettings();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (state.getLong("season.grace-until") > 0 && now >= state.getLong("season.grace-until")) {
            state.setLong("season.grace-until", 0);
            Bukkit.broadcast(Component.text("Global PvP grace period has ended."));
        }
        if (!state.getBoolean("season.end-open") && state.getLong("season.started-at") > 0
            && now >= state.getLong("season.started-at") + days("season.end-open-delay-days")) {
            state.setBoolean("season.end-open", true);
            Bukkit.broadcast(Component.text("The End is now open."));
        }
        if (state.getLong("season.locator-until") > 0 && now >= state.getLong("season.locator-until")) {
            state.setLong("season.locator-until", 0);
            setLocator(false);
        }
    }

    private long minutes(String path) { return getConfig().getLong("season." + path) * 60_000L; }
    private long days(String path) { return getConfig().getLong("season." + path) * 86_400_000L; }
    private boolean active(long until) { return until > System.currentTimeMillis(); }
    private boolean grace() { return active(state.getLong("season.grace-until")); }
    private boolean protectedPlayer(Player player) { return active(state.protectionUntil(player.getUniqueId())); }
    private boolean bypass(Player player) { return player.hasPermission("smprules.bypass"); }
    private int hearts(Player player) { return (int) Math.round(player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() / 2.0); }

    private void applyWorldSettings() {
        for (World world : Bukkit.getWorlds()) world.getWorldBorder().setSize(getConfig().getDouble("world.border-size"));
    }

    // LifeStealZ keeps the authoritative heart data. Its public events safely suppress only floor transfers.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void heartFloor(ZPlayerPvPDeathEvent event) {
        int floor = switch (state.phase()) {
            case "WEEK1" -> getConfig().getInt("hearts.week1-minimum");
            case "WEEK2" -> getConfig().getInt("hearts.week2-minimum");
            default -> 0;
        };
        Player victim = event.getOriginalEvent().getEntity();
        if (floor > 0 && hearts(victim) <= floor) {
            event.setHeartsToLose(0);
            event.setHeartsKillerGains(0);
            event.setKillerShouldGainHearts(false);
            event.setShouldDropHearts(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preFinalDayElimination(ZPlayerEliminationEvent event) {
        if (!state.phase().equals("FINAL_DAY")) {
            event.setShouldBanPlayer(false);
            event.setShouldAnnounceElimination(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pvpProtectionAndCaps(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = playerAttacker(event.getDamager());
        if (attacker != null && attacker != victim && (grace() || protectedPlayer(victim) || protectedPlayer(attacker))) {
            event.setCancelled(true);
            return;
        }
        if (attacker != null && attacker.getInventory().getItemInMainHand().getType() == Material.MACE) {
            event.setDamage(Math.min(event.getDamage(), getConfig().getDouble("damage-caps.mace")));
        }
        if (event.getDamager() instanceof ExplosiveMinecart) {
            event.setDamage(Math.min(event.getDamage(), getConfig().getDouble("damage-caps.tnt-minecart")));
        }
    }

    @EventHandler
    public void grantDeathProtection(PlayerDeathEvent event) {
        state.protectionUntil(event.getEntity().getUniqueId(), System.currentTimeMillis() + minutes("post-death-protection-minutes"));
    }

    @EventHandler(ignoreCancelled = true)
    public void armorEndsProtection(PlayerInventorySlotChangeEvent event) {
        if (isArmor(event.getNewItemStack())) state.protectionUntil(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler(ignoreCancelled = true)
    public void armorClickEndsProtection(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (isArmor(event.getCurrentItem()) && (event.getSlotType() == InventoryType.SlotType.ARMOR || event.isShiftClick())) {
            state.protectionUntil(player.getUniqueId(), 0);
        }
        if (movesLegendaryToContainer(event.getClickedInventory(), event.getCurrentItem())
            || movesLegendaryToContainer(event.getClickedInventory(), event.getCursor())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void armorDragEndsProtection(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && isArmor(event.getOldCursor())) state.protectionUntil(player.getUniqueId(), 0);
        if (isLegendary(event.getOldCursor()) && event.getInventory().getType() != InventoryType.CRAFTING) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void blockLegendaryStorage(InventoryMoveItemEvent event) {
        if (isLegendary(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void blockLegendaryFrame(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType().name().contains("ITEM_FRAME") && isLegendary(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void itemRestrictions(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        if (bannedMaterial(item.getType())) event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && isLegendary(item) && event.getClickedBlock() != null
            && event.getClickedBlock().getState() instanceof InventoryHolder) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void noPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && !bypass(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void closeEnd(PlayerPortalEvent event) {
        if (event.getTo() != null && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END
            && !state.getBoolean("season.end-open") && !bypass(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void normalizeStrengthOnBrew(BrewEvent event) {
        for (int slot = 0; slot < event.getContents().getSize(); slot++) event.getContents().setItem(slot, normalizedPotion(event.getContents().getItem(slot)));
    }

    @EventHandler(ignoreCancelled = true)
    public void normalizeStrengthOnConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (isBannedPotion(item)) {
            event.setCancelled(true);
        } else {
            event.setItem(normalizedPotion(item));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void legendaryChestplateRecipe(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        if (isChestplateRecipe(inventory.getMatrix())) {
            inventory.setResult(legendaryChestplate());
        } else if (inventory.getResult() != null && inventory.getResult().getType() == Material.MACE) {
            inventory.setResult(legendaryMace());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void registerCraft(CraftItemEvent event) {
        if (isLegendary(event.getCurrentItem()) && event.isShiftClick()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void noNormalNetheriteArmor(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result != null && illegalNetherite(result)) event.setResult(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void retainDroppedLegendary(ItemDespawnEvent event) {
        if (isLegendary(event.getEntity().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void protectDroppedMace(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && item.getItemStack().getType() == Material.MACE && isLegendary(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    private ItemStack normalizedPotion(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof PotionMeta meta)) return item;
        String type = String.valueOf(meta.getBasePotionType()).toLowerCase();
        if (type.contains("strong_strength")) {
            meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, getConfig().getInt("potions.strength-ii-duration-seconds") * 20, 1), true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isBannedPotion(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof PotionMeta meta)) return false;
        String type = String.valueOf(meta.getBasePotionType()).toLowerCase();
        return type.contains("poison") || type.contains("turtle") || type.contains("slow_falling")
            || type.contains("weakness") || type.contains("strong_swiftness") || type.contains("slowness");
    }

    private Player playerAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        return damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player ? player : null;
    }

    private boolean isChestplateRecipe(ItemStack[] matrix) {
        int eggs = 0;
        int diamonds = 0;
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            if (item.getType() == Material.DRAGON_EGG) eggs += item.getAmount();
            else if (item.getType() == Material.DIAMOND) diamonds += item.getAmount();
            else return false;
        }
        return eggs == 1 && diamonds == 8;
    }

    private ItemStack legendaryChestplate() {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(legendaryType, PersistentDataType.STRING, "chestplate");
        meta.getPersistentDataContainer().set(legendaryId, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.displayName(Component.text("GLITG Legendary Netherite Chestplate"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack legendaryMace() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(legendaryType, PersistentDataType.STRING, "mace");
        meta.getPersistentDataContainer().set(legendaryId, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.displayName(Component.text("GLITG Legendary Mace"));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isLegendary(ItemStack item) {
        return item != null && item.hasItemMeta() && (item.getItemMeta().getPersistentDataContainer().has(legendaryType, PersistentDataType.STRING) || isHeart(item));
    }
    private boolean isHeart(ItemStack item) {
        if (item == null || Bukkit.getPluginManager().getPlugin("LifeStealZ") == null) return false;
        try { return LifeStealZ.getAPI().getCustomItemID(item).toLowerCase().contains("heart"); }
        catch (RuntimeException exception) { return false; }
    }
    private boolean movesLegendaryToContainer(Inventory inventory, ItemStack item) {
        return isLegendary(item) && inventory != null && inventory.getType() != InventoryType.CRAFTING && inventory.getType() != InventoryType.PLAYER;
    }
    private boolean isArmor(ItemStack item) { return item != null && item.getType().name().matches(".*_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"); }
    private boolean illegalNetherite(ItemStack item) {
        if (item == null) return false;
        return switch (item.getType()) {
            case NETHERITE_SWORD, NETHERITE_AXE, NETHERITE_HELMET, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> true;
            case NETHERITE_CHESTPLATE -> !isLegendary(item);
            default -> false;
        };
    }
    private boolean bannedMaterial(Material material) {
        return material == Material.END_CRYSTAL || material == Material.ENCHANTED_GOLDEN_APPLE
            || material == Material.TOTEM_OF_UNDYING || material == Material.TIPPED_ARROW;
    }

    private void setLocator(boolean enabled) {
        for (World world : Bukkit.getWorlds()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey() + " run gamerule locatorBar " + enabled);
    }
    private boolean command(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(Component.text("phase=" + state.phase() + ", grace=" + grace() + ", end-open=" + state.getBoolean("season.end-open")));
            return true;
        }
        if (!sender.hasPermission("smprules.admin")) return true;
        long now = System.currentTimeMillis();
        if (args[0].equalsIgnoreCase("season") && args.length == 2) {
            if (args[1].equalsIgnoreCase("start")) {
                state.phase("WEEK1"); state.setLong("season.started-at", now); state.setLong("season.grace-until", now + minutes("grace-period-minutes"));
            } else if (List.of("week1", "week2", "final_day").contains(args[1].toLowerCase())) state.phase(args[1].toUpperCase());
            else return false;
        } else if (args[0].equalsIgnoreCase("grace") && args.length == 2) state.setLong("season.grace-until", args[1].equalsIgnoreCase("on") ? now + minutes("grace-period-minutes") : 0);
        else if (args[0].equalsIgnoreCase("end") && args.length == 2) state.setBoolean("season.end-open", args[1].equalsIgnoreCase("open"));
        else if (args[0].equalsIgnoreCase("locator") && args.length == 2) { boolean on = args[1].equalsIgnoreCase("on"); state.setLong("season.locator-until", on ? now + getConfig().getLong("season.locator-event-duration-hours") * 3_600_000L : 0); setLocator(on); }
        else return false;
        sender.sendMessage(Component.text("GLITG SMP state updated."));
        return true;
    }
}
