# GLITG SMP Enforcement Report

`RULES.md` is the public, normative gameplay specification. This file tracks
implementation and verification only; it never changes the public rules.

| Rule | Intended behavior | Status | Implementation / action |
| --- | --- | --- | --- |
| Week 1 hearts | Three-heart floor; no elimination bans | Configured, unverified | Active LifeStealZ phase is `minHearts: 3`, bans disabled |
| Week 2 hearts | One-heart floor; no elimination bans | Pending manual transition | Admin runbook changes the phase |
| Final day | No floor; elimination bans enabled | Pending manual transition | Admin runbook changes the phase |
| Start grace | One global hour of PvP grace | Configured, operational action pending | Global PvP is disabled for launch; admins change `PVP` to `true` exactly one hour later |
| Post-death attack cancellation | Attacking a player ends 30-minute protection | Missing | PvPManager Lite has a duration-only respawn protection setting; staff must remove protection-abuse privileges until a compatible implementation is verified |
| Legendary storage | Hearts, both Maces, and chestplate cannot enter listed storage | Pending ItemControl initialization | The persistent ItemControl rules must be created by an operator before launch; test its custom-heart support without imposing a heart cap, otherwise retain that part as staff-enforced |
| Mace protection | Protect from accidental destruction where possible | Missing | Admin recovery procedure only |
| World border | 4,000 x 4,000 blocks | Configured, unverified | A load datapack applies the border in the Overworld, Nether, and End |
| Combat lava/ice/draining | Banned during combat | Manual enforcement required | PvPManager enforces combat Elytra and logging; the other restrictions need staff enforcement |
| Backup armour | Allowed outside combat; combat switching/restocking is banned | Manual enforcement required | No spare-armour limit is configured; staff enforce only combat switching/restocking |
| Week 1 invisibility chat | Hidden only during Week 1 | Partial | Must enable at Week 1 and disable after it; current plugin cannot schedule this |
| End opening | One week after server start | Pending manual transition | Admin opens it at the scheduled time |
| Locator event | Enabled for one announced day | Pending manual transition | Admin enables then disables it after the event |
| Golden Apple recipe | One Apple plus four Gold Ingots, no eight-ingot alternative | Configured, unverified | Datapack overrides the vanilla Golden Apple recipe; verify both crafting patterns before launch |
| Paper Anti-Xray | Enabled without player bypasses | Configured, unverified | Paper engine mode 2 is enabled in `paper-world-defaults.yml` |

## Verification

Use the detailed test matrix in `OPERATIONS.md`. Record a rule as verified only
after an in-game test, not merely because a configuration file was applied.
