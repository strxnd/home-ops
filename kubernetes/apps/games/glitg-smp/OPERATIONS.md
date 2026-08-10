# GLITG SMP Operations

This is independent from the `minecraft` vanilla hardcore server and uses its
own `glitg-smp-data` PVC. The original GLITG server-list icon and MOTD
`GLITG S1 - LifeSteal` are retained.

## Runtime

| Component | Version | Status |
| --- | --- | --- |
| Paper | 26.1.2 build 61 | Smoke-tested on Java 25 |
| Java | Temurin 25 | Smoke-tested |
| LifeStealZ | 2.21.1 | Declares Paper 26.1.2 |
| KaiCore | 1.5 | Loads on Paper 26.1.2 |
| ItemBlocker | 1.1.3 | Loads on Paper 26.1.2 |
| CustomRecipes | 1.0 | Loads on 26.2 |
| CoreProtect CE | 24.0 | Declares Paper 26.1.2 |
| GrimAC | 2.3.74-2614909 | Loads on Paper 26.1.2 |
| SMPRules | 2.0.0 | Built locally against Paper 26.1.2 API and LifeStealZ 2.21.1 |

Plugin downloads are pinned in `config/plugins.txt`. The LifeStealZ build is
verified in the image build against SHA-256
`c4d466f579ec91ef3dd51c01fbdcab3aa8cf98009489c229abe4c823a4dbaea1`.

Paper 26.1.2 is intentionally pinned because it is the newest Paper release
supported by CoreProtect CE 24.0. Do not change the Paper version independently
of a staged CoreProtect compatibility test.

## Ownership

| Owner | Rules |
| --- | --- |
| LifeStealZ | Heart persistence, transfers, withdrawals, crafted hearts, final-day bans |
| KaiCore | One Mace, global kit limits, End gate, villager restock, invisible death chat, combat tag/logging, combat Elytra/Riptide/restock restrictions |
| ItemBlocker | Global banned items, enchant ceilings/bans, potion bans |
| SMPRules | Week floors, global grace, respawn protection/armour cancellation, Strength II normalization, hard damage caps, pearl veto, legendary storage/chestplate fallback, locator controls |
| SMPChanges | Breeze Rod loot table replacement |
| GrimAC | Conservative anti-cheat defaults |

## Admin Commands

Only operators receive `smprules.admin`, `smprules.bypass`, and plugin bypass
permissions. Do not grant these to normal players.

| Action | Command |
| --- | --- |
| Start SMP / Week 1 / global grace | `/smprules season start` |
| Change to Week 2 | `/smprules season week2` |
| Allow final-day LifeStealZ elimination | `/smprules season final_day` |
| Manually toggle global grace | `/smprules grace on` or `/smprules grace off` |
| Open/close End | `/smprules end open` or `/smprules end close` |
| Locator event (24h) | `/smprules locator on` or `/smprules locator off` |

Build and publish the image before Flux can deploy this release:

```sh
docker build -f kubernetes/apps/games/glitg-smp/image/Dockerfile -t ghcr.io/strxnd/glitg-smp:2.0.0 .
docker push ghcr.io/strxnd/glitg-smp:2.0.0
```
