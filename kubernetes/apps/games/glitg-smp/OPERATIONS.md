# GLITG SMP Operations

## Access And Health

- LAN: `192.168.20.12:25565`
- External: `glitg.kumaraarav.dev:25565`, after forwarding WAN TCP `25565` to `192.168.20.12`.

```sh
flux get kustomizations -n games glitg-smp
flux get helmreleases -n games glitg-smp
kubectl -n games get pods,svc
kubectl -n games logs deployment/glitg-smp --tail=200
```

Expected: both Flux resources are `Ready`, the pod is `1/1 Running`, and the
LoadBalancer service has IP `192.168.20.12`.

## Rule Ownership

| Owner | Enforced behavior |
| --- | --- |
| LifeStealZ | Hearts, withdrawals, eliminations, anti-alt protection, crystal/bed/anchor prevention |
| KaiCore 1.5 | One Mace, Pearl-use ban, combat tag/logging, combat Elytra/Riptide/restock restrictions, Xaero fair mode, villager restocks, enchant caps, Netherite ban, End gate, locator bar, kit limits, vanished death messages |
| PvPManager 4.0.9 | 30-minute PvP-only respawn protection; all combat-tag, combat-log, toggle, cooldown, and update functions are disabled |
| LongerPotionLevels 1.0.1 | Strength II and Speed II brewed drinkable and splash potions last eight minutes |
| MaceDamageCap 1.0.2 | Mace damage capped at 16 points, or eight hearts |
| BuffedItems 1.9.0 | Dragon-egg legendary Netherite chestplate recipe; unbreakable, soulbound, and protected from dropping, containers, and item frames |
| ItemBlocker | Banned items, potions, and enchantments not owned by KaiCore |
| CartLimiter | TNT minecart damage cap |
| CoreProtect CE | Audit logs and rollback |
| GrimAC | Anti-cheat detection |
| ViaVersion | Client protocol compatibility |
| Chunky | World pre-generation |

All plugin versions are pinned in `app/helmrelease.yaml`. The stock image removes
stale root-level plugin JARs before each startup, so removed rule owners cannot
continue loading from the persistent volume.

## Manual Season Changes

Week and final-day transitions are intentionally not scheduled. Make each change
through Git during a quiet period, validate locally, push, reconcile Flux, and
perform the relevant smoke test before announcing the phase.

| Manual action | Git configuration |
| --- | --- |
| Open/close the End | `config/plugins/KaiCore/config.yml`: `dimensions.allow-end`; alternatively use `/end open` or `/end close` for an immediate operator action, then update Git to match |
| Enable/disable locator bar | `config/plugins/KaiCore/config.yml`: `locator-bar.enabled` |
| Week one heart floor | Set `config/plugins/LifeStealZ/config.yml`: `minHearts: 3`, keeping `disablePlayerBanOnElimination: true` |
| Week two heart floor | Set `config/plugins/LifeStealZ/config.yml`: `minHearts: 1`, keeping `disablePlayerBanOnElimination: true` |
| Final-day elimination | Set `config/plugins/LifeStealZ/config.yml`: `minHearts: 0` and `disablePlayerBanOnElimination: false` |
| Adjust respawn protection | `config/plugins/PvPManager/config.yml`: `Player Kills.Anti Kill Abuse.Respawn Protection` in seconds |
| Adjust Mace cap | `config/plugins/MaceDamageCap/config.yml`: `damage-cap`, where one point is half a heart |

The default configuration has no heart floor and does not ban eliminations.
The manual settings above are LifeStealZ's native floor and elimination controls;
smoke-test each phase change before announcing it.

## Admin Commands

| Action | Command |
| --- | --- |
| Open KaiCore UI | `/kaicore gui` or `/kc gui` |
| Open/close the End immediately | `/end open` or `/end close` |
| Check/change Mace cap | `/macecap` |
| Give the legendary chestplate | `/bi give <player> legendary_netherite_chestplate 1` |
| List BuffedItems | `/bi list` |
| Pre-generate a world | `/chunky world <world>`; `/chunky radius <blocks>`; `/chunky start` |
| Monitor/pause Chunky | `/chunky progress`; `/chunky pause`; `/chunky continue` |
| Inspect blocks/containers | `/co i` |
| Roll back verified activity | `/co rollback` |

`kaicore.use`, `antiend.use`, `kaicore.bypass`, `macedamagecap.admin`, and
`buffeditems.admin` are operator-only. Never grant bypass permissions to players.

## Smoke Test

1. Confirm TCP accepts connections on `192.168.20.12:25565`.
2. Confirm logs enable KaiCore, PvPManager, LongerPotionLevels, MaceDamageCap, and BuffedItems without errors.
3. Confirm an Ender Pearl cannot be used or teleport a non-operator.
4. Confirm a respawned player cannot deal or receive PvP damage for 30 minutes.
5. Confirm brewed Strength II is eight minutes for both drinkable and splash potion variants.
6. Confirm a Mace hit never exceeds eight hearts of final damage.
7. Confirm each Healing potion delivery type is capped at six per inventory.
8. Confirm the dragon-egg recipe creates the named chestplate; verify it is unbreakable, remains after death, and cannot be dropped, container-stored, or frame-stored.
9. Confirm `/end close` blocks access and `/end open` permits it.
10. Confirm CoreProtect records a block change and container transaction.

## Complete Test Matrix

Run these with a non-operator test account unless the row says otherwise. Use
an operator only for setup, inspection, and commands. Mark each row with the
server revision and date after testing.

| Area | Test | How to test | Expected result | Current status |
| --- | --- | --- | --- | --- |
| Health | Flux | Run `flux get kustomizations -n games glitg-smp` and `flux get helmreleases -n games glitg-smp` | Both resources are `Ready` at the intended Git revision | Verified at `291e6b1` |
| Health | Pod | Run `kubectl -n games get pods` | One GLITG pod is `1/1 Running` with no restarts | Verified at `291e6b1` |
| Health | LAN connection | Join `192.168.20.12:25565` | Vanilla client reaches the server and can join | TCP verified; join not verified |
| Health | External connection | Join `glitg.kumaraarav.dev:25565` from outside the LAN | Server is reachable after WAN port forwarding | Not verified |
| Health | Plugin load | Inspect startup logs | No load/enable errors for LifeStealZ, KaiCore, PvPManager, LongerPotionLevels, MaceDamageCap, BuffedItems, ItemBlocker, CartLimiter, CoreProtect, GrimAC, ViaVersion, or Chunky | Load verified; gameplay not verified |
| Access | Operator access | As `Strxnd`, run `/kc gui`, `/end close`, `/macecap`, and `/bi list` | Commands work for the operator | Not verified |
| Access | Player permissions | As a non-operator, run `/kc gui`, `/end open`, `/macecap`, and `/bi give` | Commands are denied | Not verified |
| LifeSteal | Starting health | Join with a new test player | Player starts with 10 hearts | Not verified |
| LifeSteal | PvP heart transfer | Kill a player above the configured floor | Victim loses one heart and killer gains one heart | Not verified |
| LifeSteal | Natural death loss | Die to a non-player cause | Player loses one heart according to LifeStealZ settings | Not verified |
| LifeSteal | Heart withdrawal | Withdraw a heart at more than one heart | Withdrawal succeeds without permitting self-elimination | Not verified |
| LifeSteal | Heart item use | Use a crafted heart at 10 hearts or fewer, then above 10 hearts | It adds one heart at or below 10; use is denied above 10 | Not verified |
| LifeSteal | Heart recipe | Craft the configured Nether Star heart recipe | Recipe produces the configured heart item | Not verified |
| LifeSteal | Anti-alt | Attempt repeated kills between the same test accounts | Anti-alt prevention/logging behaves as configured | Not verified |
| LifeSteal | Crystal PvP | Try to damage a player with an End Crystal | Player damage is prevented | Not verified |
| LifeSteal | Bed PvP | Try to damage a player with an exploding bed | Player damage is prevented | Not verified |
| LifeSteal | Respawn Anchor PvP | Try to damage a player with a Respawn Anchor | Player damage is prevented | Not verified |
| Season | Default phase | Inspect `LifeStealZ/config.yml` in the pod | `minHearts: 0` and `disablePlayerBanOnElimination: true` | Verified at `291e6b1` |
| Season | Week one floor | Temporarily set `minHearts: 3`, roll out, then kill a three-heart player | Player cannot fall below three hearts; elimination remains disabled | Not tested; manual Git transition |
| Season | Week two floor | Temporarily set `minHearts: 1`, roll out, then kill a one-heart player | Player cannot fall below one heart; elimination remains disabled | Not tested; manual Git transition |
| Season | Final day | Temporarily set `minHearts: 0` and `disablePlayerBanOnElimination: false`, then eliminate a player | Elimination ban occurs only in this phase | Not tested; manual Git transition |
| Respawn | Protection duration | Kill a test player, respawn, and attempt PvP both directions at 0, 29, and 31 minutes | PvP is blocked for 30 minutes, then allowed | Not verified |
| Respawn | Armour cancellation | Kill a player, respawn, equip armour, then attempt PvP | This is **not implemented**: current PvPManager protection does not end on armour equip | Known gap |
| Combat | Tag duration | Deal PvP damage, stop fighting, and time the tag | KaiCore tag lasts 30 seconds | Not verified |
| Combat | Combat logging | Deal PvP damage, then disconnect before tag expiry | KaiCore kills the logger | Not verified |
| Combat | Combat restock | Tag a player, then attempt villager restock/trade interaction | Restock is blocked while tagged | Not verified |
| Combat | Combat Elytra | Tag a player, equip/use Elytra | Elytra use is blocked while tagged | Not verified |
| Combat | Combat Riptide | Tag a player, use a Riptide trident | Riptide is blocked while tagged | Not verified |
| Combat | Combat trigger | Deal one point of PvP damage | KaiCore applies a combat tag at the configured zero minimum threshold | Not verified |
| Mace | One Mace | Craft a Mace, then attempt to craft another | First craft succeeds; subsequent craft is denied | Not verified |
| Mace | Damage cap | Deal high fall-distance Mace damage to an unarmoured target | Final Mace damage is never more than 16 points, or eight hearts | Config/load verified; gameplay not verified |
| Mace | Enchant caps | Try Protection IV, Sharpness IV, and Power V | Each configured over-cap enchantment is blocked | Not verified |
| Mace | Mace storage protection | Store the Mace in chests, hoppers, shulkers, frames, and other containers | This is **not implemented** by the current KaiCore configuration | Known gap |
| Pearls | Pearl use | Right-click an Ender Pearl | KaiCore blocks use; the Pearl is not thrown or teleported | Config verified; gameplay not verified |
| Pearls | Throw-without-teleport behavior | Throw an Ender Pearl and observe projectile behavior | This is **not implemented**. Pearls are fully banned, not converted to non-teleporting projectiles | Known gap |
| Potions | Strength II duration | Brew and drink Strength II | Effect duration is eight minutes | Config/load verified; gameplay not verified |
| Potions | Strength II splash | Brew and throw Strength II splash | Applied effect duration is eight minutes | Config/load verified; gameplay not verified |
| Potions | Speed II preservation | Brew and drink Speed II | Vanilla duration remains unchanged | Config verified; gameplay not verified |
| Potions | Banned effects | Brew/use Poison, Turtle Master, Slow Falling, Weakness, Slowness, and Speed II | ItemBlocker blocks each configured effect | Not verified |
| Limits | Healing potions | Put seven regular Healing potions in inventory | Excess beyond six is dropped | Config verified; gameplay not verified |
| Limits | Splash Healing potions | Put seven Splash Healing potions in inventory | Excess beyond six is dropped | Config verified; gameplay not verified |
| Limits | Lingering Healing potions | Put seven Lingering Healing potions in inventory | Excess beyond six is dropped | Config verified; gameplay not verified |
| Limits | Other kit limits | Exceed XP bottles, cobwebs, golden apples, and Breeze Rod limits | Excess is dropped at 192, 128, 128, and 64 respectively | Not verified |
| Items | Banned items | Obtain End Crystals, Enchanted Golden Apples, Totems, Tipped Arrows, and banned Netherite gear | ItemBlocker blocks all configured actions | Not verified |
| Items | Netherite restriction | Craft, smith, pick up, equip, and store banned Netherite sword, axe, helmet, leggings, and boots | KaiCore and ItemBlocker prevent use/circulation as configured | Not verified |
| Chestplate | Recipe | Craft eight Diamonds around one Dragon Egg | Produces one named legendary Netherite chestplate | Load verified; crafting not verified |
| Chestplate | Unbreakable | Use the chestplate until it would take durability | Durability does not decrease | Not verified |
| Chestplate | Death retention | Die while carrying/equipping the chestplate | Chestplate remains with its owner after death | Not verified |
| Chestplate | Drop protection | Attempt to drop the chestplate | Drop is blocked | Not verified |
| Chestplate | Container protection | Attempt to place it in a chest, barrel, hopper, shulker, and item frame | Container and frame storage are blocked | Not verified |
| Chestplate | Duplication paths | Try shift-crafting and automated crafting paths | Recipe produces exactly one chestplate without duplication | Not verified |
| End | Closed End | As a non-operator, enter an End portal while `/end close` is active | Travel is denied | Not verified |
| End | Open End | Run `/end open`, then enter an End portal | Travel succeeds | Not verified |
| End | State persistence | Restart after changing End state through `/end` | This is **not Git-authoritative** unless `KaiCore/config.yml` is updated too | Manual-operation risk |
| Locator | Locator disabled | Join with `locator-bar.enabled: false` | Locator bar is disabled | Not verified |
| Locator | Locator event | Set `locator-bar.enabled: true` through Git and roll out | Locator bar appears, then can be disabled by reverting Git | Not tested; manual Git transition |
| Xaero | Xaero disabler | Join with Xaero's Minimap installed | KaiCore enforces Xaero disable/fair mode for players without bypass permissions | Not verified |
| Villagers | Instant restock | Exhaust a villager trade, then reopen/trade | KaiCore immediately restocks the villager | Not verified |
| TNT minecart | Damage cap | Damage a player with a TNT minecart | CartLimiter limits damage to 20 points, or 10 hearts | Not verified |
| World | Breeze Rod loot | Kill a Breeze | Modified loot table behaves as intended | Not verified |
| World | Border | Run `/worldborder get` in every relevant world | This is **not configured**. The former 4000-block border was custom SMPRules behavior | Known gap |
| Chunky | Pre-generation | Run `/chunky world <world>`, `/chunky radius <blocks>`, `/chunky start` | Generation starts and `/chunky progress` advances | Not verified |
| CoreProtect | Block audit | Place and break a block, then run `/co i` | Actor and timestamp are recorded | Not verified |
| CoreProtect | Container audit | Move an item into/out of a container, then inspect | Container transaction is recorded | Not verified |
| CoreProtect | Rollback | In a disposable area, roll back a known change | Only intended changes are reverted | Not verified |
| GrimAC | Startup | Review startup logs | Plugin enables; existing SLF4J warnings are non-blocking upstream warnings | Enabled; gameplay not verified |
| GrimAC | Detection | Perform a safe, known-invalid movement/combat test | Violation is logged without false positives in normal play | Not verified |
| ViaVersion | Older client | Join with a supported non-1.21.11 client | Client can join and play normally | Not verified |
| Config | Git authority | Edit a mounted plugin config, push, reconcile, then restart deployment | ConfigMap is copied into `/data/plugins/<plugin>` and rule changes take effect | Verified for current revision |
| Cleanup | Plugin inventory | List `/data/plugins/*.jar` after restart | Only declared plugin JARs exist; no ConditionalEvents, Denizen, EpicSafePvp, ItemLimiter, DisableRiptide, ForceXaeroFairPlay, InstantVillagerRestock, or PlaceholderAPI JAR remains | Verified at `291e6b1` |
