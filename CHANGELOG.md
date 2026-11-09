# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.9.1+1.21.9] - 2026-11-09

**Compatible with Minecraft 1.21.9, 1.21.10**

### Changed
- Updated Fabric API to 0.138.4+1.21.10
- Updated ModMenu to 16.0.1
- Updated Fabric Loader to 0.20.1 (was 0.17.2)
- Updated Fabric Language Kotlin to 1.14.2+kotlin.2.4.21 (was 1.13.7+kotlin.2.2.21)
- Updated Kotlin to 2.4.21 (was 2.2.20)
- **Requires Fabric Language Kotlin 1.14.2+kotlin.2.4.21 or newer.** The
  mod is built with Kotlin 2.4 and can crash on an older Kotlin runtime.

### Technical
- The jar name and the Modrinth version number carry the MC version
  (`2.9.1+1.21.10`).
- The Fabric Language Kotlin floor in `fabric.mod.json` comes from
  `fabric_kotlin_version` in `gradle.properties`.

## [0.9.0+1.21.9] - 2025-10-04

**Compatible with Minecraft 1.21.9**

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