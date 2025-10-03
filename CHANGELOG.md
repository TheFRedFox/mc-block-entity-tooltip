# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.0+1.21.6] - 2025-09-07

**Compatible with Minecraft 1.21.6, 1.21.7, and 1.21.8**

### Changed
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