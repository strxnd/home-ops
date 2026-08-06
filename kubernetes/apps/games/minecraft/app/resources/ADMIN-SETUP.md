# LifeSteal SMP staging and administration

The Kubernetes workload installs the pinned Paper image, plugin jars, static
plugin configuration, and the recipe datapack. The following state is
intentionally completed in-game because ItemControl's custom-item identity and
storage restrictions are persisted on the server volume.

## One-time ItemControl setup

Using an operator account, open `/itemcontrol menu` and configure the actual
items produced by the running plugins:

- Mark the LifeStealZ heart item, mace, and the Dragon Egg chestplate as global
  items with legendary storage restrictions. These global entries provide
  identity and storage control; they are not the source of the mace count.
- Keep the mace count exclusively in MaceConfig (`max-count: 1`). Do not add a
  redundant ItemControl global count for the mace.
- For each of those items, block storage in chests, ender chests, chest boats,
  barrels, bundles, item frames, furnaces, shelves, and hoppers.
- Select the crafted Dragon Egg chestplate itself as an exact custom item,
  identified by the PDC marker `smp:dragon_egg_chestplate=true`. If the
  chestplate is produced by consuming the world's single Dragon Egg, do not
  add a redundant ItemControl count; the recipe itself guarantees uniqueness.
- Configure the exact chestplate to ignore the general Netherite chestplate
  restriction. Set the normal Netherite armor and weapon materials to zero
  allowed items.
- KaiCore owns the global enchantment caps: Power IV, Protection III, and
  Sharpness III maximums, with Punch, Lunge, Fire Aspect, Flame, and Thorns
  blocked. ItemControl should not be used for those global level caps.

Every storage type above is a production gate. Verify both insert and extract
paths, including hoppers and portable containers. Verify that the
single-Dragon-Egg recipe consumes the only Dragon Egg and produces the one
unbreakable tagged chestplate.

## Recipe implementation

The `Custom Recipes` jar is installed as requested, but its pinned 1.0
serializer persists material, name, lore, and enchantments only. It does not
persist the required PDC marker. The actual shaped recipe is therefore the
server-side datapack in `lifesteal-smp`, which produces the exact
`smp:dragon_egg_chestplate=true` marker without a client mod.

The recipe is:

```text
D D D
D E D
D D D
```

with diamonds for `D` and a Dragon Egg for `E`.

## Heart recipe

The LifeStealZ heart recipe is:

```text
Trial Key   Ghast Tear   Trial Key
Ghast Tear  Eye of Ender Ghast Tear
Trial Key   Ghast Tear   Trial Key
```

## Launch and event commands

The container startup RCON commands set the world border to 4000 blocks in all
three dimensions and disable the locator bar. SMPUtils+ enables its 16-health
point mace cap from its persistent configuration; the startup command is not
needed. The RCON service remains ClusterIP-only.

Run these from the server console at the events:

```text
gamerule minecraft:locator_bar true
gamerule minecraft:locator_bar false
end open
```

The first two commands are the locator-bar event and its 24-hour close. Run
`end open` on day seven. Do not add an automatic kit command; the kit remains
rules-defined only.

## Staging blockers

- LifeStealZ is configured with `minHearts: 3`. Confirm that a player remains
  active at three hearts. If LifeStealZ eliminates at that floor, do not ship
  this configuration; replace it with EzLifesteal.
- SMPUtils+ 1.5 is pinned by Modrinth version ID but its listing advertises
  1.21.x only. Confirm the plugin loads on Paper 26.2 and that the cap applies
  to final damage after armor and effects. If it fails, build and publish the
  planned `MaceCap` fallback before production.
- CoreProtect 24.0 is pinned, but its listing currently stops at 26.1.2.
  Confirm startup and investigation/rollback behavior on Paper 26.2.
- Speed I remains allowed through Item Limiter. KaiCore blocks Speed II, and
  ItemControl blocks the drinkable Strong Swiftness potion; verify splash and
  lingering variants during the potion test.
- The ExternalSecret expects a 1Password item named `minecraft` with an
  `RCON_PASSWORD` field. Verify that item and field before reconciliation.

The full test matrix remains the one in the deployment request: plugin load,
three-heart floor, grace persistence, all legendary storage paths, recipe and
replacement behavior, mace and TNT damage, item/potion limits, combat tagging,
End locking, Anti-Xray/Grim alerts, restart persistence, and backup restore.
