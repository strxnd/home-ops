# GLITG SMP Operations

This free-plugin server is independent from the `minecraft` vanilla hardcore server. It
uses the `glitg-smp` PVC, `192.168.20.12`, and `glitg.${SECRET_DOMAIN}`. The
season seed, icon, and MOTD `GLITG S1 - LifeSteal` are intentionally retained.

## Runtime

| Component | Version | Responsibility |
| --- | --- | --- |
| Paper | 1.21.11 build 132 | Server runtime |
| Java | Temurin 21 | JVM runtime |
| LifeStealZ | 2.21.1 | Hearts, transfers, withdrawals, heart items, elimination |
| ConditionalEvents | 4.79.2 | Atomic weekly heart floors using LifeStealZ events |
| Denizen | 1.3.2 build 7279 | Season gates, recipes, legendary-item rules, damage caps |
| EpicSafePvP | 1.39.1 | Combat tagging, logging, Elytra and command restrictions |
| ItemLimiter | 1.1.0 | Global Mace count and inventory kit limits |
| ItemBlocker | 1.1.3 | Banned items, potions, and enchantment ceilings |
| CartLimiter | 1.7 | TNT minecart damage cap |
| DisableRiptide | 1.0.0 | Riptide restriction during combat |
| ForceXaeroFairPlay | 2.4.0 | Xaero fair-play mode |
| InstaRestock | 1.1.1 | Infinite villager restocking |
| CoreProtect CE | 24.0 | Block audit and rollback |
| GrimAC | 2.3.74-2614909 | Anti-cheat |
| ViaVersion | 5.11.0 | Client protocol compatibility |
| PlaceholderAPI | 2.12.3 | Plugin placeholder integration |
| Chunky | 1.5.3 | Optional border pre-generation |

All plugin artifacts and SHA-256 values are pinned in `plugins.lock`. The image
build fails if any downloaded artifact differs from its lock entry. No locally
compiled Minecraft plugin is used.

## Rule Matrix

| Rule | Owner | Configuration |
| --- | --- | --- |
| Starting hearts | LifeStealZ | 10 |
| Maximum hearts | LifeStealZ | 20 |
| Week-one floor | ConditionalEvents | 3 hearts; neither side transfers at floor |
| Week-two floor | ConditionalEvents | 1 heart; neither side transfers at floor |
| Final-day elimination | LifeStealZ | Zero hearts bans; floor events disabled |
| Natural deaths | LifeStealZ | Lose one heart without dropping a heart item |
| Heart-item use | LifeStealZ | One heart; usable at 10 hearts or fewer |
| Heart withdrawal | LifeStealZ | Cannot reduce player below one heart |
| Global grace | Denizen | 60 minutes; bidirectional PvP block |
| Death protection | Denizen | 30 minutes; armor equip or attack ends it |
| End gate | Denizen | Closed until week two or seven days after start |
| World border | Denizen | 4000 blocks wide in all dimensions |
| Locator bar | Denizen | Admin-controlled 24-hour event |
| Ender pearls | Denizen/EpicSafePvP | Teleport vetoed; also blocked in combat |
| Combat tag | EpicSafePvP | 30 seconds |
| Combat logging | EpicSafePvP | Player is killed and inventory drops |
| Combat Elytra | EpicSafePvP | Equip, boost, and glide blocked |
| Combat Riptide | DisableRiptide | Blocked for 30 seconds |
| Combat restocking | Server rule | Bucket draining remains manually enforced |
| Mace | ItemLimiter/Denizen | One global legendary Mace |
| Mace damage | Denizen | 16 HP maximum raw damage |
| TNT minecart damage | CartLimiter/Denizen | 20 HP maximum raw damage |
| Legendary chestplate | Denizen | Dragon Egg plus eight Diamonds; unbreakable |
| Legendary storage | Denizen | Mace, chestplate, and heart items cannot enter containers or frames |
| Legendary drops | Denizen/LifeStealZ | Fire, explosion, and despawn protected |
| Strength II | Denizen | Strong Strength plus Redstone creates genuine 8:00 Strength II |
| Healing potions | Denizen | Six each: drinkable, splash, and lingering |
| Experience bottles | ItemLimiter | 192 per inventory |
| Cobwebs | ItemLimiter | 128 per inventory |
| Golden apples | ItemLimiter | 128 per inventory |
| Breeze rods | ItemLimiter | 64 per inventory; Breeze loot replaced by datapack |
| Banned combat items | ItemBlocker/LifeStealZ | Crystals, enchanted apples, totems, tipped arrows |
| Banned netherite | ItemBlocker | Sword, axe, helmet, leggings, and boots |
| Banned potions | ItemBlocker | Poison, Turtle Master, Slow Falling, Weakness, Speed II, Slowness |
| Enchantment ceilings | ItemBlocker | Protection III, Sharpness III, Power IV; Thorns, Fire Aspect, Punch, Lunge banned |
| Bed/anchor PvP | LifeStealZ | Explosive use blocked in hostile dimensions |
| Invisible death chat | Denizen | Message hidden when victim or killer is invisible |
| Villager restock | InstaRestock | Immediate unlimited restocking |
| Xaero minimap | ForceXaeroFairPlay | Fair-play; Nether roof cave mode |

## Season Commands

Only operators have `glitg.admin`.

| Action | Command |
| --- | --- |
| Show state | `/glitg status` |
| Start season, week one, grace, and End timer | `/glitg start` |
| Restore week-one floor | `/glitg week1` |
| Start week two and open the End | `/glitg week2` |
| Enable zero-heart elimination | `/glitg final_day` |
| Toggle global grace | `/glitg grace on` or `/glitg grace off` |
| Toggle End access | `/glitg end open` or `/glitg end close` |
| Toggle 24-hour locator event | `/glitg locator on` or `/glitg locator off` |

Heart-floor configuration changes must be made during a quiet period. Do not
reload or toggle floor events while a PvP death is being processed.

## Acceptance Tests

Run in this order so foundational failures stop the test before destructive or
season-state tests.

1. Confirm DNS resolves to `192.168.20.12` and port 25565 accepts connections.
2. Confirm the server list shows `GLITG S1 - LifeSteal` and the retained icon.
3. Confirm Paper reports `1.21.11` build 132 and Java reports version 21.
4. Confirm all components in the runtime table are enabled with no startup exception.
5. Confirm KaiCore, SMPRules, EzLifesteal, and CustomRecipes are absent.
6. Confirm `/seed` returns `-3085124641489919468`.
7. Confirm `Strxnd` is an operator and another normal player is not.
8. Confirm all three dimensions have a 4000-block world border.
9. Confirm a new player starts at 10 hearts and reconnecting preserves the value.
10. Confirm a PvP kill transfers exactly one heart from victim to killer.
11. Confirm a natural death removes one heart and gives no killer heart.
12. At three hearts in week one, confirm PvP death changes neither player’s hearts.
13. At four hearts in week one, confirm PvP death leaves the victim at three and rewards the killer.
14. Switch to week two; at one heart confirm PvP death changes neither player’s hearts.
15. At two hearts in week two, confirm PvP death leaves the victim at one and rewards the killer.
16. Switch to final day; confirm a one-heart victim reaches zero, is eliminated, and the killer gets no elimination reward.
17. Confirm a withdrawn heart cannot reduce a player below one heart.
18. Confirm a heart item works at 10 hearts and is rejected above 10 hearts.
19. Start grace and confirm melee, projectile, and indirect PvP are blocked in both directions.
20. End grace and confirm normal PvP resumes.
21. Die, respawn, and confirm 30-minute bidirectional PvP protection.
22. Equip one armor piece and confirm respawn protection ends immediately.
23. Confirm attacking another player also ends the attacker’s respawn protection.
24. During week one confirm End portal travel is blocked; during week two confirm it succeeds.
25. Confirm Ender Pearls never teleport the thrower.
26. Enter combat and confirm the 30-second timer, command restrictions, Elytra restrictions, and Riptide restriction.
27. Disconnect in combat and confirm the player dies and inventory drops once.
28. Confirm draining buckets during combat is rejected by staff under the published rule.
29. Craft the first Mace and confirm it becomes the named legendary Mace; confirm a second global acquisition is blocked.
30. Confirm Mace raw damage never exceeds 16 HP and TNT minecart raw damage never exceeds 20 HP.
31. Craft the chestplate with one Dragon Egg and eight Diamonds; confirm name, unbreakable state, and recipe layout.
32. Confirm Mace, chestplate, and LifeSteal heart items cannot enter chests, barrels, shulkers, Ender Chests, hoppers, or item frames.
33. Drop each protected item into fire, lava, an explosion, and past normal despawn time; confirm it survives.
34. Brew Strong Strength with Redstone; confirm the resulting item tooltip and consumed effect both show Strength II for 8:00.
35. Confirm drinkable, splash, and lingering Healing potions each cap independently at six.
36. Confirm XP bottles, cobwebs, golden apples, and Breeze rods cap at 192, 128, 128, and 64 respectively.
37. Confirm every banned item, potion, netherite item, and over-limit enchantment is blocked through crafting, loot, trade, pickup, use, and container transfer.
38. Confirm bed/anchor PvP, invisible death messages, villager restocking, Breeze loot replacement, CoreProtect logging, Grim checks, ViaVersion clients, and Xaero fair-play behavior.
