# LifeSteal SMP staging and administration

The Kubernetes workload installs the pinned Paper image, plugin jars, static
plugin configuration, and the recipe datapack. The following state is
intentionally completed in-game so ItemControl's tracked global counters are
not reset by a declarative bootstrap.

## One-time ItemControl setup

Using an operator account, open `/itemcontrol menu` and configure the actual
items produced by the running plugins:

- Mark the LifeStealZ heart item, mace, and the Dragon Egg chestplate as global
  items with legendary storage restrictions.
- For each of those items, block storage in chests, ender chests, chest boats,
  barrels, bundles, item frames, furnaces, shelves, and hoppers.
- Select the crafted Dragon Egg chestplate itself as an exact custom item,
  identified by the PDC marker `smp:dragon_egg_chestplate=true`, and set its
  global active-item limit to one.
- Configure the exact chestplate to ignore the general Netherite chestplate
  restriction. Set the normal Netherite armor and weapon materials to zero
  allowed items.
- Set Power to a maximum of IV and set Punch, Lunge, Fire Aspect, Flame, and
  Thorns to a maximum of zero.

Every storage type above is a production gate. Verify both insert and extract
paths, including hoppers and portable containers, and verify that a destroyed
reward chestplate frees the one-item global count.

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

## Launch and event commands

The container startup RCON commands set the world border to 4000 blocks in all
three dimensions, disable the locator bar, and set the SMPUtils+ mace cap to
16 health points. The RCON service remains ClusterIP-only.

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
- Item Limiter's `POTION_<EFFECT>` syntax applies to every strength/variant of
  an effect. The current `POTION_SPEED: 0` setting therefore bans Speed I as
  well as Speed II; decide whether that broader rule is acceptable during the
  potion test.
- The ExternalSecret expects a 1Password item named `minecraft` with an
  `RCON_PASSWORD` field. Verify that item and field before reconciliation.

The full test matrix remains the one in the deployment request: plugin load,
three-heart floor, grace persistence, all legendary storage paths, recipe and
replacement behavior, mace and TNT damage, item/potion limits, combat tagging,
End locking, Anti-Xray/Grim alerts, restart persistence, and backup restore.
