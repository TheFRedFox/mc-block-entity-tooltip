# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.10.0+26.1.2] - 2026-05-15

**Compatible with Minecraft 26.1, 26.1.1, and 26.1.2**

### Added
- Configurable tooltip position (issue #26). The config screen has a
  **Position Editor** entry opening a live editor screen:
  - **Preset buttons** laid out spatially: top-left/center/right, and
    bottom-left / **Action bar** / bottom-right.
  - **X / Y inputs** (0–1 fraction) for precise placement.
  - **Drag** the sample box anywhere; any of the three methods previews live
    and a freehand placement is marked `Custom`.
  - **Cancel** restores the position to what it was when the editor opened.
- Position is stored as a normalized fraction of free screen space, so it is
  independent of GUI scale and resolution.

### Changed
- **Default position is now `ACTION_BAR`** (centred, just above the hotbar —
  `0.5 / 0.83`). New installs get this; existing configs keep their saved
  value. The previous fixed 10px/40px bottom-right placement is gone.

### Technical
- First MINOR feature bump in the project's history (4.9.0 → 4.10.0); the
  feature digit had been static at `9`.
- The editor is the single live hub; `positionPreset`/`posX`/`posY` are
  `@ConfigEntry.Gui.Excluded` (serialized, not shown as Cloth fields). The
  HUD renders from a `PositionDraft` the editor edits live, so **Done** shows
  in-game at once. Persistence stays Cloth's: `FreehandEditorEntry.isEdited()`
  enables "Save & Quit", `save()` (Cloth `saveAll`, never on discard) commits
  the draft to config. Cloth exposes no discard hook, so a client-tick check
  (`screen == null && draft ≠ config`) re-seeds the draft from config — i.e. a
  discard rolls the in-game position back too. **Cancel/Escape** in the editor
  reverts the draft to the editor-open snapshot.
- Cloth Config 26.1.x has no native button list-entry; the launcher is a
  `TextListEntry` subclass registered via `AutoConfigClient.getGuiRegistry` +
  `registerAnnotationProvider` on a `@FreehandEditorButton` marker field, with
  bounds captured from `extractRenderState` so it only triggers on its own row.
- Editor uses the MC 26.1 input/render API (`MouseButtonEvent`, `EditBox`,
  `extractRenderState(GuiGraphicsExtractor, …)`).

## [4.9.0+26.1.2] - 2026-05-15

**Compatible with Minecraft 26.1, 26.1.1, and 26.1.2**

Binary compatibility across the 26.1.x line verified by loading the 26.1.2-built jar on real MC 26.1 client.

### Changed
- Updated Minecraft to 26.1.2
- Updated Fabric API to 0.149.0+26.1.2
- Updated ModMenu to 18.0.0-beta.1 (still pre-release but graduated from alpha — TerraformersMC's v18 ladder is alpha → beta → stable; same MC support range: 26.1, 26.1.1, 26.1.2)
- Updated Fabric Loom to 1.16.2
- Updated Gradle to 9.5.1
- Updated GitHub Actions: `actions/upload-artifact` v6→v7, `gradle/actions` v5→v6, `softprops/action-gh-release` v2→v3

### Technical
- Added Renovate config rule pinning `net.fabricmc.fabric-api:fabric-api` updates to `+26.1.2` builds. Without this, Renovate proposed `+26.2` snapshot-line jars because the `+<mc>` suffix is SemVer build metadata and is ignored during version comparison.

## [4.9.0+26.1.1] - 2026-05-12

**Compatible with Minecraft 26.1.1**

### Changed
- Updated Minecraft to 26.1.1
- Updated Fabric API to 0.145.4+26.1.1

## [4.9.0+26.1] - 2026-05-08

**Compatible with Minecraft 26.1**

### Changed
- Migrated to Minecraft 26.1 (first release on the unobfuscated MC line)
- Switched Fabric Loom plugin ID from `fabric-loom` to `net.fabricmc.fabric-loom` (required for unobfuscated MC)
- Removed `loom.officialMojangMappings()` — not needed; 26.1+ source is already in Mojang names
- Switched dependency configurations from `modImplementation`/`modApi` to `implementation`/`api`
- Bumped Java toolchain from 21 to 25 (required by MC 26.1.x)
- Updated Fabric API to 0.145.1+26.1
- Updated Cloth Config to 26.1.154
- Updated ModMenu to 18.0.0-alpha.8 (only release for MC 26.x at the time of this release; explicitly accepted exception)
- Updated `fabric.mod.json` constraints: `minecraft ~26.1.0`, `cloth-config >=26.1.0`, `fabricloader >=0.19.0`, `java >=25`
- Updated GitHub Actions release workflow to use Java 25

### Technical
- HUD rendering API change: `HudElement.render(GuiGraphics, DeltaTracker)` → `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`
- Method renames in `GuiGraphicsExtractor`: `drawString()` → `text()` (other helpers like `fill()`, `guiWidth()`, `guiHeight()` unchanged)
- `publishMods.file` switched from `remapJar.archiveFile` to `jar.archiveFile` (no remap step on unobfuscated builds)
- Added `org.gradle.toolchains.foojay-resolver-convention` plugin to settings.gradle for Java toolchain auto-resolution

### Note
- ModMenu is still labeled alpha by TerraformersMC, but the underlying code has been frozen since 2026-03-25 and supports all stable MC 26.1.x releases. v18 stable will likely arrive with the next MC 26.2 release ceremony.
- The `1.21.11` branch is preserved for users who can't migrate to MC 26.1.x.

## [3.9.2+1.21.11] - 2026-05-07

**Compatible with Minecraft 1.21.11**

### Changed
- Updated Fabric Loom to 1.16.1
- Updated Gradle to 9.5.0

### Note
- MC 26.x migration deferred: ModMenu has no stable release for MC 26.x yet (only `18.0.0-alpha.8` available)

## [3.9.1+1.21.11] - 2026-05-06

**Compatible with Minecraft 1.21.11**

### Changed
- Updated Fabric API to 0.141.3+1.21.11
- Updated Fabric Loader to 0.19.2
- Updated ModMenu to 17.0.0 (stable)
- Updated Fabric Language Kotlin to 1.13.11+kotlin.2.3.21
- Updated Fabric Loom to 1.14.10
- Updated Kotlin to 2.3.21

### Technical
- Migrated `AutoConfig.getConfigScreen` to `AutoConfigClient.getConfigScreen` for Cloth Config 21.11.x

## [3.9.0+1.21.11] - 2025-12-15

**Compatible with Minecraft 1.21.11**

### Changed
- Updated to Minecraft 1.21.11 compatibility
- **Migrated from Yarn to Mojang (official) mappings** - Prepares for upcoming unobfuscated Minecraft versions (26.1+)
- Updated Fabric API to 0.139.5+1.21.11
- Updated Fabric Loader to 0.18.2
- Updated Cloth Config to 21.11.151
- Updated ModMenu to 17.0.0-alpha.1
- Updated Fabric Language Kotlin to 1.13.7+kotlin.2.2.21
- Updated Fabric Loom to 1.14.6
- Updated Gradle to 9.2.1
- Updated Kotlin to 2.2.21

### Technical
- Refactored all Minecraft API imports from Yarn naming to Mojang naming conventions
- Key class renames: `MinecraftClient` → `Minecraft`, `DrawContext` → `GuiGraphics`, `Text` → `Component`, `PlayerEntity` → `Player`
- Key method renames: `raycast()` → `pick()`, `entityWorld` → `level()`, `drawTextWithShadow()` → `drawString()`

## [0.9.0+1.21.9] - 2025-10-04

**Compatible with Minecraft 1.21.9 and 1.21.10**

### Changed
- Updated to Minecraft 1.21.9 compatibility
- Updated Fabric API to 0.133.14+1.21.9
- Updated ModMenu to 16.0.0-rc.1 for MC 1.21.9 compatibility
- Updated Gradle to 8.14
- Updated Fabric Loom to 1.11.3

### Fixed
- Fixed Entity world field access for MC 1.21.9 - changed from `world` field to `entityWorld` property (MC 1.21.9 made Entity.world field private)

### Technical
- Migrated from direct `world` field access to Kotlin property syntax `entityWorld`

## [0.9.0+1.21.6] - 2025-09-07

**Compatible with Minecraft 1.21.6, 1.21.7, and 1.21.8**

### Changed
- Verified and bumped Fabric API to 0.133.4+1.21.8 for MC 1.21.8 compatibility
- Verified and bumped ModMenu to 15.0.0 for MC 1.21.8 compatibility
- Added support for Minecraft 1.21.6+ (no new mod features, only compatibility updates)
- Migrated from deprecated `HudLayerRegistrationCallback` to new `HudElementRegistry` API (required for MC 1.21.6+)
- Updated Fabric API to 1.21.6+ compatible versions
- Updated Fabric Loader from 0.16.10 to 0.16.14
- Updated Cloth Config to 1.21.6+ compatible versions
- Updated ModMenu to 1.21.6+ compatible versions
- Updated Yarn mappings to match Minecraft version

### Fixed
- Text rendering issues with new HUD API - text now properly displays with correct ARGB color format
- HUD element positioning changed from before chat to after crosshair for better rendering order

### Technical
- Refactored HUD rendering to use `HudElementRegistry.attachElementAfter()`
- Fixed text color format to use full ARGB (`0xFFFFFFFF`) instead of RGB
- Converted `LookingAtRenderer` from `LayeredDrawer.Layer` to `HudElement` interface

## [0.9.0+1.21.4] - Previous Version

**Compatible with Minecraft 1.21.4 and 1.21.5**

### Added
- Initial release of Block Entity Tooltip mod
- Display block names when looking at blocks
- Display entity names when looking at entities
- Configuration support via Cloth Config
- ModMenu integration for easy configuration access
- Configurable display settings (enable/disable blocks, entities, fluids)
- Configurable detection distance

### Note
This version uses the older `HudLayerRegistrationCallback` API which was removed in Minecraft 1.21.6.