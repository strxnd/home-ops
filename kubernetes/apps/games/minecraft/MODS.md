# Minecraft 26.2 mod selection

Source archive: `~/Downloads/mods.zip`

The archive contains 41 JARs and 32 Packwiz index entries, but the bundled
files target Minecraft 1.21 or 1.21.1. The server therefore resolves pinned
Minecraft 26.2 Fabric releases for the compatible projects instead of copying
the archive's JARs directly.

## Installed on the server

| Project | Minecraft 26.2 release | Modrinth version ID |
| --- | --- | --- |
| Axiom | 5.5.0 | `QO99kC52` |
| Carpet | 26.2 | `bGrLxJ8v` |
| Carpet TIS Addition | 1.82.4 | `lW1s6HL1` |
| Elytra Trims | 4.8.3 | `xe4XXryG` |
| Fabric API | 0.157.0 | `vmQp7ixA` |
| Fabric Language Kotlin | 1.13.13 | `bdhiINYC` |
| FerriteCore | 9.0.0 | `d5ddUdiB` |
| G4mespeed | 1.6.1 | `KwnkHbt9` |
| Lithium | 0.25.3 | `f7vZ0VWU` |
| Syncmatica | 0.3.19 | `f74T22XS` |
| WorldEdit | 7.4.5 | `6YnCYPwc` |

## Not installed on the server

The following indexed projects were not installed because the source pack marks
them client-only or their 26.2 project declares server support unsupported: 3D
Skin Layers, Borderless Fullscreen, Client Commands, Continuity, Distant
Horizons, Flashback, Indium, Iris, Isometric Renders, Item Scroller, Litematica,
MaLiLib, MiniHUD, Mod Menu, Sodium, Sodium Extra, Sound Controller, TweakerMore,
Tweakeroo, and VoxelMap.

Minimap Sync and owo-lib have no Fabric release for Minecraft 26.2. The
archive's unindexed/local JARs also have no verified 26.2 artifact: Breeze Mod,
ChunkDebug, Intricarpet, Item Scroller Craftfix, Local Server Entity ID Fix,
Tech Utils, and TrialDivers. Four `.jar.<suffix>` files in the ZIP are partial
download artifacts and are intentionally ignored.

Clients need their own compatible 26.2 client modpack. Gameplay-affecting mods
such as Elytra Trims and Syncmatica must be present at the same compatible
version on participating clients.
