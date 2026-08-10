# GLITG SMP operations

This is a new, independent Paper deployment. It does not mount, modify, or
migrate the existing `minecraft` PVC.

## Runtime and sources

| Component | Exact version | Official source |
|---|---:|---|
| Minecraft / Paper | 26.1.2 / `26.1.2-61-main` | [Paper downloads](https://papermc.io/downloads/paper) |
| Java | Temurin 25.0.3+9 LTS | [Paper Java requirements](https://docs.papermc.io/paper/getting-started/) |
| KaiCore | 1.5 | [Modrinth](https://cdn.modrinth.com/data/kV8iMq85/versions/yCPbvC8z/kaicore-1.5.jar) |
| LifeStealZ | 2.21.1 | [GitHub release](https://github.com/ZetaPlugins/LifeStealZ/releases/download/2.21.1/lifestealz-2.21.1.jar) |
| ItemBlocker | 1.1.3 | [Modrinth](https://cdn.modrinth.com/data/klHgtaeR/versions/1fW12xlf/ItemBlocker-1.1.3.jar) |
| CustomRecipes | 1.0 | [Modrinth](https://cdn.modrinth.com/data/YD56Nhy5/versions/ikKvjBcF/customRecipes.jar) |
| CoreProtect CE | 24.0 | [Modrinth](https://cdn.modrinth.com/data/Lu3KuzdV/versions/Kma0kBsY/CoreProtect-CE-24.0.jar) |
| GrimAC | 2.3.74-2614909 | [Modrinth](https://cdn.modrinth.com/data/LJNGWSvH/versions/fbt7nJt5/grimac-bukkit-2.3.74-2614909.jar) |
| SMPRules | 1.0.0 | `smprules/` in this repository |

Paper's current 26.1.2 build is intentionally pinned. The Paper API used to
compile SMPRules is `26.1.2.build.61-stable`.

## Ownership

| Owner | Mechanics |
|---|---|
| LifeStealZ | Heart persistence, transfer, withdrawal, max health and Final Day elimination. SMPRules adjusts its documented public death events for weekly floors. |
| SMPRules | Season state, global grace, post-death protection, the only combat tags/logoff penalty, End gate, locator event, legendary identities/recovery, chestplate/mace uniqueness, damage caps, dragon scoreboard/egg, pearl teleport veto, kit limits, Breeze drops, contextual restrictions. |
| ItemBlocker | Unconditional prohibited materials, potion types and enchantment limits. |
| KaiCore | Xaero minimap blocker and infinite villager restock only. Its combat, locator, dimension, netherite, mace, recipe, TNT-cart and enchant systems are explicitly disabled. |
| CoreProtect | SQLite investigation log: block changes, containers/hoppers, interactions, inventory changes, sessions and commands. Retain data by reviewing disk usage monthly and running a tested `/co purge t:180d` only after a backup. |
| Grim | Conservative default predictive movement/combat/impossible-interaction checks. |
| CustomRecipes | Available only for future ordinary recipes. SMPRules owns the special PDC chestplate recipe. |

## Launch and backup

Build the immutable server image from the repository root:

```sh
docker build -f kubernetes/apps/games/glitg-smp/image/Dockerfile -t ghcr.io/strxnd/glitg-smp:1.0.0 .
```

The HelmRelease starts it with the equivalent runtime command:

```sh
java -Xms6G -Xmx6G -jar paper-26.1.2-61.jar --nogui
```

Before the first Flux reconciliation, publish the image above to the configured
GHCR repository and confirm that `192.168.20.12` is unused. The new PVC is
`glitg-smp-data`; back it up while the pod is stopped with a filesystem-level
archive or a storage snapshot. Restore by scaling the release down, replacing
only that PVC's data from the verified archive/snapshot, then scaling up.

For updates: take and verify a fresh backup, update one pinned component at a
time, build a new tagged image, smoke-test it against a copied data directory,
change the Helm image tag, inspect Flux's diff, then reconcile only after an
explicit operator approval. Never downgrade a world after Paper has opened it.

## Test checklist

The following must be exercised with two real test accounts before the public
season. The local smoke test proves startup, plugin compatibility and restart
persistence only; it cannot simulate client combat or a real dragon fight.

- [ ] Lifesteal: normal transfer; Week 1/Week 2 floors; Final Day elimination;
  20-heart cap; heart use at 9 and 10 hearts.
- [ ] Protection: death timer, both PvP directions, armour removal, expiry and reconnect.
- [ ] Legendary: one Mace, damage cap, all listed loss modes/recovery/restart;
  chestplate recipe/PDC/unbreakable status; every forbidden storage type.
- [ ] Dragon: two players, disconnect/tie, leaderboard winner, no duplicate egg.
- [ ] Combat: tag, normal/network logout, immediate reconnect, teleport/restart,
  Elytra, Riptide versus ordinary trident, lava/ice/bucket/sponge/armour limits.
- [ ] Damage/potions/kit/pearls/Breeze: every case in the supplied SMP checklist.
- [x] Clean first boot: all seven plugins load on Paper 26.1.2 build 61 / Java 25.
- [x] Clean restart: same persisted local world starts with the same plugin set.

## Human-enforced rules

Staff must judge public/visible bases and ownership signs, offline raiding,
unjustified destruction, naked-player kills, team high-tier classification,
reasonable farms/redstone, modified-client intent, replay-mod intent, lag
machines, and novel dupes/exploits. CoreProtect and Grim provide evidence; they
cannot perfectly determine those subjective or client-side behaviours.
