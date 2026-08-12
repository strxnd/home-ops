# GLITG SMP Operations

## Access And Health

- LAN: `192.168.20.12:25565`
- External: `glitg.kumaraarav.dev:25565`, after forwarding WAN TCP `25565` to the LAN address.

```sh
flux get kustomizations -n games glitg-smp
flux get helmreleases -n games glitg-smp
kubectl -n games get pods,svc
kubectl -n games logs deployment/glitg-smp --tail=200
```

Expected: both Flux resources are `Ready`, the pod is `1/1 Running`, and the
LoadBalancer service has external IP `192.168.20.12`.

## Installed Plugins

| Plugin | Purpose |
| --- | --- |
| LifeStealZ | Hearts, heart items, withdrawals, eliminations |
| ConditionalEvents | Week-one and week-two heart-floor events |
| EpicSafePvp | Combat tags, logout penalties, Elytra restrictions |
| ItemLimiter | Global Mace and inventory limits |
| ItemBlocker | Banned items, potions, enchantments |
| CartLimiter | TNT minecart damage cap |
| DisableRiptide | Combat Riptide restriction |
| ForceXaeroFairPlay | Xaero fair-play mode |
| Instant Villager Restock | Immediate villager restocking |
| Denizen | Installed without scripts or custom commands |
| PlaceholderAPI | Plugin placeholder support |
| CoreProtect CE | Audit logs and rollback |
| GrimAC | Anti-cheat detection |
| ViaVersion | Client protocol compatibility |
| Chunky | World pre-generation |

The stock Minecraft image downloads these version-pinned plugins at startup.

## Native Operations

There is no `/glitg` command and no custom script. Use native plugin and
vanilla commands instead.

| Action | Command |
| --- | --- |
| Enable week-one floor | `/ce enable lifesteal_week1_floor`; `/ce disable lifesteal_week2_floor` |
| Enable week-two floor | `/ce disable lifesteal_week1_floor`; `/ce enable lifesteal_week2_floor` |
| Verify/reload ConditionalEvents | `/ce verify`; `/ce reload` |
| Inspect combat state | `/esp admin inspect <player>` |
| Clear combat state | `/esp admin clear <player>` |
| Apply respawn protection | `/esp admin respawn <player> <seconds>` |
| Reload EpicSafePvp | `/esp reload` |
| Pre-generate a world | `/chunky world <world>`; `/chunky radius <blocks>`; `/chunky start` |
| Monitor/pause Chunky | `/chunky progress`; `/chunky pause`; `/chunky continue` |
| CoreProtect lookup | `/co i`, then inspect blocks and containers |
| Performance profile | `/spark healthreport`; `/spark profiler start`; `/spark profiler stop` |

Use native server commands for manual season state:

```text
/pvpglobal off
/pvpglobal on
/worldborder center 0 0
/worldborder set 4000
/gamerule locatorBar false
```

Run borders and gamerules separately in each dimension.

## Not Configured

Removing the Denizen script also removes: automatic season schedules, End
gating, locator-bar scheduling, post-death protection, Pearl teleport
cancellation, chestplate recipe/protection, legendary-item protections, Mace
damage cap, Strength II conversion, healing-potion cap, and vanished death
message suppression.

Do not advertise these as enforced until an off-the-shelf plugin implements and
validates them.

## Smoke Test

1. Confirm TCP accepts connections on `192.168.20.12:25565`.
2. Confirm Paper and every listed plugin starts without errors.
3. Confirm new players start at 10 hearts and PvP kills transfer one heart.
4. Test both ConditionalEvents heart floors.
5. Test combat logging, Elytra/Riptide restrictions, item limits, banned items, and enchantment limits.
6. Test villager restocking and run Chunky only while monitoring `/spark healthreport`.
7. Confirm CoreProtect records a block change and a container transaction.
