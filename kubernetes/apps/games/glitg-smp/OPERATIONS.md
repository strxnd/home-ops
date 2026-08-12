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

## Plugin Ownership

| Plugin | Enforced behavior |
| --- | --- |
| KaiCore 1.5 | One Mace, combat tag/logging, combat Elytra/Riptide/restock restrictions, Xaero fair mode, villager restocks, enchantment caps, Netherite ban, End gate, locator bar, kit limits, vanished death messages |
| LifeStealZ | Hearts, heart items, withdrawals, eliminations, crystal/bed/anchor prevention |
| ItemBlocker | Banned items and potion restrictions not covered by KaiCore |
| CartLimiter | TNT minecart damage cap |
| CoreProtect CE | Audit logs and rollback |
| GrimAC | Anti-cheat detection |
| ViaVersion | Client protocol compatibility |
| Chunky | World pre-generation |

KaiCore replaces ConditionalEvents, EpicSafePvp, ItemLimiter, DisableRiptide,
ForceXaeroFairPlay, Instant Villager Restock, Denizen, and PlaceholderAPI. An
init container removes their stale JARs from the persistent plugin directory
before Paper starts, so only KaiCore owns the overlapping rules.

## KaiCore Operations

| Action | Command |
| --- | --- |
| Open KaiCore management UI | `/kaicore gui` or `/kc gui` |
| Upload KaiCore logs | `/kc logs` |
| Open the End | `/end open` |
| Close the End | `/end close` |
| Pre-generate a world | `/chunky world <world>`; `/chunky radius <blocks>`; `/chunky start` |
| Monitor/pause Chunky | `/chunky progress`; `/chunky pause`; `/chunky continue` |
| Inspect blocks/containers | `/co i` |
| Roll back verified activity | `/co rollback` |

`kaicore.use`, `antiend.use`, and `kaicore.bypass` are operator-only. Do not
give bypass permissions to players.

## Season Operations

KaiCore starts with the End closed and locator bar disabled. Use `/end open`
when the End event begins. Use the KaiCore GUI to change supported rule-state
settings; changes persist in its plugin configuration on the server PVC.

Week-specific Lifesteal heart floors and final-day elimination are not automated
without a scheduling plugin. Change those only during a quiet period through
LifeStealZ's native administration interface, then verify a floor death before
announcing the phase.

## Smoke Test

1. Confirm TCP accepts connections on `192.168.20.12:25565`.
2. Confirm the log says KaiCore enabled and does not load ConditionalEvents, EpicSafePvp, ItemLimiter, DisableRiptide, ForceXaeroFairPlay, Instant Villager Restock, Denizen, or PlaceholderAPI.
3. Confirm `/kc gui` opens for an operator and is denied to a player.
4. Confirm `/end close` blocks access and `/end open` permits it.
5. Test one Mace craft, combat logging, combat Elytra/Riptide restrictions, armour restock prevention, villager restocking, Xaero fair mode, item limits, enchant caps, and Netherite restrictions.
6. Confirm CoreProtect records a block change and container transaction.
