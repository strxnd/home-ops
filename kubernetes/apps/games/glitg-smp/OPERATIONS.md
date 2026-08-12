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
| KaiCore 1.5 | One normally craftable Mace, Pearl-use ban, crystal restriction, Xaero fair mode, villager restocks, Netherite ban, End gate, locator bar, and vanished death messages |
| ItemControl 1.2.0 | Global two-Mace ceiling, legendary-item storage rules, and banned-item controls configured through its persistent operator UI |
| The Limiters 2.0 | Potion restrictions and enchantment caps/bans; its item limiter is disabled |
| PvPManager 4.0.9 | 30-second combat tags, combat-log punishment, combat Elytra restriction, and 30-minute PvP-only respawn protection |
| LongerPotionLevels 1.0.1 | Strength II brewed drinkable and splash potions last eight minutes; Speed II remains banned by The Limiters |
| MaceDamageCap 1.0.2 | Mace damage capped at 16 points, or eight hearts |
| BuffedItems 1.9.0 | Dragon-egg legendary Netherite chestplate recipe and unbreakable property |
| CartLimiter | TNT minecart damage cap |
| CoreProtect CE | Audit logs and rollback |
| GrimAC | Anti-cheat detection |
| ViaVersion | Client protocol compatibility |
| Chunky | World pre-generation |

All plugin versions are pinned in `app/helmrelease.yaml`. The stock image removes
stale root-level plugin JARs before each startup, so removed rule owners cannot
continue loading from the persistent volume.

## Required ItemControl Initialization

ItemControl persists exact global/custom-item limits and storage restrictions in
its database on the data PVC; its upstream release deliberately creates them
through the operator UI rather than YAML. Before admitting players, an operator
must use `/itemcontrol menu` to create and verify these rules:

| ItemControl rule | Required value |
| --- | --- |
| Mace global limit | `2`; block storage in chests, Ender chests, chest boats, barrels, bundles, item frames, furnaces, shelves, and hoppers |
| LifeStealZ Heart custom-item rule | Block the same storage destinations without blocking normal use below 10 hearts; if ItemControl requires a restrictive count to apply storage rules, retain the gameplay rule as staff-enforced rather than imposing an unintended heart cap |
| GLITG Legendary Netherite Chestplate global/custom-item rule | `1`; block the same storage destinations without blocking its recipe or use |
| End Crystal, Enchanted Golden Apple, Totem of Undying, Ender Pearl, and Tipped Arrow | Block obtain/carry/use as supported by ItemControl; verify each action with a non-operator |
| Kit quantities | 6 healing potions combined where the UI supports it; 192 XP bottles; 128 cobwebs; 128 Golden Apples; 64 Breeze Rods; do not create any armour limit |

Do not grant `itemcontrol.bypass.*` or `thelimiters.bypass` to normal players.
Record the resulting ItemControl screen/export and test its persistent counts
after a normal server restart.

## Manual Season Changes

Week and final-day transitions are intentionally not scheduled. Make each change
through Git during a quiet period, validate locally, push, reconcile Flux, and
perform the relevant smoke test before announcing the phase.

| Manual action | Git configuration |
| --- | --- |
| Open/close the End | `config/plugins/KaiCore/config.yml`: `dimensions.allow-end`; alternatively use `/end open` or `/end close` for an immediate operator action, then update Git to match |
| Enable/disable locator bar | `config/plugins/KaiCore/config.yml`: `locator-bar.enabled` |
| Complete opening grace | Exactly one hour after launch, set `app/helmrelease.yaml`: `PVP: "true"`, then reconcile and verify PvP is enabled |
| Week one heart floor | The initial configuration is `minHearts: 3` with `disablePlayerBanOnElimination: true` |
| Week two heart floor | Set `config/plugins/LifeStealZ/config.yml`: `minHearts: 1`, keeping `disablePlayerBanOnElimination: true`; set both `invisibility-qol.hide-name-when-*` values to `false` in KaiCore; run `/end open` and commit the matching `dimensions.allow-end: true` |
| Final-day elimination | Set `config/plugins/LifeStealZ/config.yml`: `minHearts: 0` and `disablePlayerBanOnElimination: false` |
| Adjust respawn protection | `config/plugins/PvPManager/config.yml`: `Player Kills.Anti Kill Abuse.Respawn Protection` in seconds |
| Adjust Mace cap | `config/plugins/MaceDamageCap/config.yml`: `damage-cap`, where one point is half a heart |

The initial configuration is the Week 1 phase: a three-heart floor with no
elimination bans. PvP is disabled globally for the opening hour. The manual
settings above are LifeStealZ's native floor and elimination controls;
smoke-test each phase change before announcing it.

## Admin Commands

| Action | Command |
| --- | --- |
| Open KaiCore UI | `/kaicore gui` or `/kc gui` |
| Open/close the End immediately | `/end open` or `/end close` |
| Check/change Mace cap | `/macecap` |
| Give the legendary chestplate | `/bi give <player> legendary_netherite_chestplate 1` |
| Open ItemControl's operator UI | `/itemcontrol menu` or `/ic menu` |
| Open The Limiters operator UI | `/tl gui` |
| List BuffedItems | `/bi list` |
| Pre-generate a world | `/chunky world <world>`; `/chunky radius <blocks>`; `/chunky start` |
| Monitor/pause Chunky | `/chunky progress`; `/chunky pause`; `/chunky continue` |
| Inspect blocks/containers | `/co i` |
| Roll back verified activity | `/co rollback` |

`kaicore.use`, `antiend.use`, `kaicore.bypass`, `itemcontrol.bypass.*`,
`thelimiters.bypass`, `macedamagecap.admin`, and `buffeditems.admin` are
operator-only. Never grant bypass permissions to players.

## Smoke Test

1. Confirm TCP accepts connections on `192.168.20.12:25565`.
2. Confirm logs enable KaiCore, ItemControl, The Limiters, PvPManager, LongerPotionLevels, MaceDamageCap, and BuffedItems without errors.
3. Confirm an Ender Pearl cannot be used or teleport a non-operator.
4. Confirm a respawned player cannot deal or receive PvP damage for 30 minutes. The required attack-cancellation behavior is not implemented by the pinned PvPManager release and remains staff-enforced.
5. Confirm brewed Strength II is eight minutes for both drinkable and splash potion variants.
6. Confirm a Mace hit never exceeds eight hearts of final damage.
7. Confirm the ItemControl limits and all listed legendary storage destinations with non-operator test accounts.
8. Confirm the dragon-egg recipe creates the named chestplate and verify it is unbreakable.
9. Confirm `/end close` blocks access and `/end open` permits it.
10. Confirm CoreProtect records a block change and container transaction.
11. Confirm both the 4-ingot Golden Apple recipe and the absence of the normal 8-ingot alternative.
12. Confirm Paper Anti-Xray has no bypass permission granted to normal players.

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
| Health | Plugin load | Inspect startup logs | No load/enable errors for LifeStealZ, KaiCore, ItemControl, The Limiters, PvPManager, LongerPotionLevels, MaceDamageCap, BuffedItems, CartLimiter, CoreProtect, GrimAC, ViaVersion, or Chunky | Not verified after this revision |
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
| Season | Initial Week 1 phase | Inspect `LifeStealZ/config.yml` in the pod | `minHearts: 3` and `disablePlayerBanOnElimination: true` | Configured; not deployed/tested |
| Season | Week one floor | Kill a three-heart player | Player cannot fall below three hearts; elimination remains disabled | Not tested |
| Season | Week two floor | Temporarily set `minHearts: 1`, roll out, then kill a one-heart player | Player cannot fall below one heart; elimination remains disabled | Not tested; manual Git transition |
| Season | Final day | Temporarily set `minHearts: 0` and `disablePlayerBanOnElimination: false`, then eliminate a player | Elimination ban occurs only in this phase | Not tested; manual Git transition |
| Respawn | Protection duration | Kill a test player, respawn, and attempt PvP both directions at 0, 29, and 31 minutes | PvP is blocked for 30 minutes, then allowed | Not verified |
| Respawn | Attack cancellation | Kill a player, respawn, attack another player, then attempt PvP | **Not automated:** staff enforce abuse until a compatible implementation is verified | Known gap; see `ENFORCEMENT.md` |
| Combat | Tag duration | Deal PvP damage, stop fighting, and time the tag | PvPManager tag lasts 30 seconds | Not verified |
| Combat | Combat logging | Deal PvP damage, then disconnect before tag expiry | PvPManager kills the logger | Not verified |
| Combat | Combat armour switching/restocking | Tag a player, then switch/restock armour | Staff enforcement only; no general spare-armour restriction exists outside combat | Manual enforcement required |
| Combat | Combat Elytra | Tag a player, equip/use Elytra | Elytra use is blocked while tagged | Not verified |
| Combat | Combat Riptide | Tag a player, use a Riptide trident | Riptide is banned by The Limiters | Not verified |
| Combat | Combat trigger | Deal one point of PvP damage | PvPManager applies a 30-second combat tag | Not verified |
| Mace | One normal craft | Craft a Mace, then attempt to craft another | First craft succeeds; subsequent craft is denied | Not verified |
| Mace | Dragon Egg reward | Give the legitimate Dragon Egg holder one operator-issued Mace | ItemControl reports exactly two global Maces; any third Mace is denied | Pending ItemControl initialization |
| Mace | Damage cap | Deal high fall-distance Mace damage to an unarmoured target | Final Mace damage is never more than 16 points, or eight hearts | Config/load verified; gameplay not verified |
| Mace | Enchant caps | Try Density IV, Breach IV, and Wind Burst II | Each configured over-cap enchantment is blocked by The Limiters | Not verified |
| Mace | Mace storage protection | Store each Mace in every prohibited storage type | ItemControl denies storage after its required initialization | Pending ItemControl initialization |
| Pearls | Pearl ban | Obtain, carry, or use an Ender Pearl | ItemControl denies it after initialization; KaiCore also blocks use | Pending ItemControl initialization |
| Potions | Strength II duration | Brew and drink Strength II | Effect duration is eight minutes | Config/load verified; gameplay not verified |
| Potions | Strength II splash | Brew and throw Strength II splash | Applied effect duration is eight minutes | Config/load verified; gameplay not verified |
| Potions | Speed II ban | Brew or use Speed II | The Limiters denies the potion | Not verified |
| Potions | Banned effects | Brew/use Poison, Turtle Master, Slow Falling, Weakness, Slowness, and Speed II | The Limiters blocks each configured effect | Not verified |
| Limits | Healing potions | Put seven total Healing potions across delivery types in inventory | ItemControl limits the configured total where its UI supports a combined rule | Pending ItemControl initialization |
| Limits | Other kit limits | Exceed XP bottles, cobwebs, golden apples, and Breeze Rod limits | ItemControl limits are 192, 128, 128, and 64 | Pending ItemControl initialization |
| Items | Banned items | Obtain End Crystals, Enchanted Golden Apples, Totems, Tipped Arrows, and Ender Pearls | ItemControl blocks all configured actions after initialization | Pending ItemControl initialization |
| Items | Netherite restriction | Craft, smith, pick up, equip, and store banned Netherite sword, axe, helmet, leggings, and boots | KaiCore prevents the configured combat equipment | Not verified |
| Chestplate | Recipe | Craft eight Diamonds around one Dragon Egg | Produces one named legendary Netherite chestplate | Load verified; crafting not verified |
| Chestplate | Unbreakable | Use the chestplate until it would take durability | Durability does not decrease | Not verified |
| Chestplate | Legendary storage protection | Store the chestplate in each prohibited storage type | ItemControl denies storage after its required initialization | Pending ItemControl initialization |
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
| World | Border | Run `/worldborder get` in the Overworld, Nether, and End | Each dimension reports a 4,000-block border | Configured; not deployed/tested |
| World | Golden Apple recipe | Craft with one Apple and four Gold Ingots, then try the vanilla eight-ingot ring | Only the four-ingot cross recipe produces one Golden Apple | Configured; not deployed/tested |
| World | Paper Anti-Xray | Inspect the Paper world config and mine with a non-bypass account | Engine mode 2 is enabled; no normal player has `paper.antixray.bypass` | Configured; not deployed/tested |
| Chunky | Pre-generation | Run `/chunky world <world>`, `/chunky radius <blocks>`, `/chunky start` | Generation starts and `/chunky progress` advances | Not verified |
| CoreProtect | Block audit | Place and break a block, then run `/co i` | Actor and timestamp are recorded | Not verified |
| CoreProtect | Container audit | Move an item into/out of a container, then inspect | Container transaction is recorded | Not verified |
| CoreProtect | Rollback | In a disposable area, roll back a known change | Only intended changes are reverted | Not verified |
| GrimAC | Startup | Review startup logs | Plugin enables; existing SLF4J warnings are non-blocking upstream warnings | Enabled; gameplay not verified |
| GrimAC | Detection | Perform a safe, known-invalid movement/combat test | Violation is logged without false positives in normal play | Not verified |
| ViaVersion | Older client | Join with a supported non-1.21.11 client | Client can join and play normally | Not verified |
| Config | Git authority | Edit a mounted plugin config, push, reconcile, then restart deployment | ConfigMap is copied into `/data/plugins/<plugin>` and rule changes take effect | Verified for current revision |
| Cleanup | Plugin inventory | List `/data/plugins/*.jar` after restart | Only declared plugin JARs exist; no ConditionalEvents, Denizen, EpicSafePvp, ItemLimiter, DisableRiptide, ForceXaeroFairPlay, InstantVillagerRestock, or PlaceholderAPI JAR remains | Verified at `291e6b1` |
