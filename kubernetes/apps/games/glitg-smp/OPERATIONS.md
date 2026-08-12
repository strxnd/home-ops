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
| Adjust LifeSteal heart/elimination rules | `config/plugins/LifeStealZ/config.yml`: `minHearts`, `disablePlayerBanOnElimination`, and related heart settings |
| Adjust respawn protection | `config/plugins/PvPManager/config.yml`: `Player Kills.Anti Kill Abuse.Respawn Protection` in seconds |
| Adjust Mace cap | `config/plugins/MaceDamageCap/config.yml`: `damage-cap`, where one point is half a heart |

The original week-one/second heart-floor behavior is not represented by any
current non-custom plugin configuration. Do not claim that those floors are
enforced until a compatible public plugin is selected and smoke-tested.

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
