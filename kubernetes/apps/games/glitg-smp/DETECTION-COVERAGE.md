# GLITG CheckHacks coverage

This file records the detection coverage that is actually configured for the
GLITG SMP. It is deliberately conservative. CheckHacks v1.2.1 is configured
with the checks shipped in its official `checkhacks.yml`; no guessed custom
translation or keybind keys are included.

The live server was inspected before this change and is Paper `26.2-112`.
The official CheckHacks v1.2.1 release is pinned in the HelmRelease. The
release JAR advertises plugin version `1.2.0` in its upstream `plugin.yml`,
which is an upstream packaging detail. The release tag and artifact checksum
remain `v1.2.1` and
`66cbfbb527d83e906b568ae92def055bab5116f6da990b360bdab41bda62bdfc`.

`BUILT_IN` means the exact entry is present in the official CheckHacks
v1.2.1 configuration. `UNDETECTABLE_WITH_CURRENT_METHOD` means no shipped
check or verified custom signature is configured for that GLITG entry. It does
not mean the mod can never be detected by a future CheckHacks release.

The table contains 59 GLITG policy entries: 13 have direct built-in coverage,
0 use custom signatures, and 46 remain undetectable with the current
configured methods.

The installed Grim `2.3.74-0a18c77` JAR contains
`ac.grim.grimac.api.events.FlagEvent`, one of the event classes that
CheckHacks v1.2.1 supports for its Grim hook.

An isolated boot using the live Paper `26.2-112` JAR, Java 25, the installed
Grim JAR, and the pinned CheckHacks release loaded Paper, Grim, CheckHacks,
all 17 hacks, and the Grim hook successfully. No client-profile tests were
available in this workspace.

The config-level review covered all entries below. Real client-profile tests
were not run in this workspace, so `Tested` does not claim that a client-side
probe passed on Paper 26.2.

| Mod | Status | Detection mode | Signature/source | Tested |
| --- | --- | --- | --- | --- |
| Any hacked or cheat client | UNDETECTABLE_WITH_CURRENT_METHOD | - | No generic signature; use the named built-in checks below | SOURCE REVIEW |
| Freecam | BUILT_IN | KEYBIND | `key.freecam.toggle`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| Xray mods | BUILT_IN | KEYBIND | `xray.config.toggle`, CheckHacks XRay (Fabric) built-in | SOURCE PASS; CLIENT NOT RUN |
| Omniscience | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Camera Utils | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Better Third Person | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Shoulder Surfing Reloaded | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Meteor Client | BUILT_IN | METEOR | `key.meteor-client.open-gui`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| LiquidBounce | BUILT_IN | TRANSLATE | `liquidbounce.module.killaura.name`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| Wurst Client | BUILT_IN | KEYBIND | `key.wurst.zoom`, CheckHacks built-in for `-1.21` | SOURCE PASS; CLIENT NOT RUN |
| BleachHack | BUILT_IN | TRANSLATE | `bleachhack.module.killaura`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| Aristois | BUILT_IN | TRANSLATE | `emc.module.killaura.name`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| Coffee Client | BUILT_IN | TRANSLATE | `coffee.module.killaura.name`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| ChestESP | BUILT_IN | KEYBIND | `key.chestesp.toggle`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| KillAura | BUILT_IN | KEYBIND | `key.killaura`, CheckHacks built-in for Fabric | SOURCE PASS; CLIENT NOT RUN |
| World Downloader | BUILT_IN | TRANSLATE | `key.wdl.startStop`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| Auto Clicker | BUILT_IN | TRANSLATE / KEYBIND | Fabric AutoClicker and p1k0chu Auto Clicker built-ins | SOURCE PASS; CLIENT NOT RUN |
| Autoswitch | BUILT_IN | KEYBIND | `key.autoswitch.toggle`, CheckHacks built-in | SOURCE PASS; CLIENT NOT RUN |
| Armor Hotswap | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Arrow Shifter | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Better Mace Swap | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Click Crystals | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Hazel Crystal Optimizer | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Kind's Crystal Optimizer | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| WalksyCrystalOptimizer | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Mace Attack Assistance | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Mace Optimiser | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| SwitchTotems | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| D-hand Mod | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Dokko's Hotbar Optimizer | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Double Hotbar | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Frostbyte's Improved Inventory | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Inventory Control Tweaks | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Inventory Management | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Inventory Profiles Next | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| InvMove | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Item Scroller | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Mouse Tweaks | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Mouse Wheelie | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Quick Hotkeys | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Quickcraft | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Slot Cycler | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Sort | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Accurate Block Placement | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Bridging Mod | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Tweakeroo | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Flour's Various Tweaks | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Fluidlogged | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Multi Key Bindings | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| No Delay Optimizer | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| No Input Lag Tick Rate | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Quickmäth | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Fast XP | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Quick Exp | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Auto Elytra | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Quick Elytra | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Bedrockify | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Better Screens | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |
| Client Commands | UNDETECTABLE_WITH_CURRENT_METHOD | - | No built-in or verified custom signature | NOT RUN |

## Additional built-in checks enabled

The join and Grim-triggered lists also retain all other checks shipped by
CheckHacks v1.2.1: AutoFish, Lumina, AntiAFK, and p1k0chu Auto Clicker. They
are not new GLITG policy entries, but they remain enabled because the upstream
plugin identifies them as prohibited cheat or automation features.

## Allowed-client audit

No detection or punishment entry was added for Litematica, MaLiLib, Sodium,
Lithium, FerriteCore, ImmediatelyFast, EntityCulling, Iris, Fabric API, or
Mod Menu. None of those names or generic keys appears in the configured
CheckHacks list. No client profiles were available for live false-positive
tests.

## Result handling

CheckHacks' built-in SQLite database remains enabled by default under the
persistent plugin data directory. The configured command runs only when a
scan has a `DETECTED` result and kicks the player. `PROTECTED` and `SKIPPED`
results are logged and sent to the console and `checkhacks.alerts` staff, but
they do not kick or count as clean. CheckHacks does not expose a detected-mod
placeholder to its automatic command, so the friendly name remains in the
staff result lines and database record.

The existing container image manages the plugin JAR list through its
`var-list` manifest, which removes the old ClientID JAR when the new list is
reconciled. The init container also removes the old ClientID plugin directory
from the persistent PVC. No other plugin directory is touched.
