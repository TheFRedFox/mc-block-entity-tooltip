# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.10.0] - 2025-09-07

### Added
- Support for Minecraft 1.21.8
- Updated all dependencies to latest versions

### Changed
- **BREAKING**: Migrated from deprecated `HudLayerRegistrationCallback` to new `HudElementRegistry` API
- Updated Minecraft version from 1.21.4 to 1.21.8
- Updated Fabric API from 0.118.0+1.21.4 to 0.133.4+1.21.8
- Updated Fabric Loader from 0.16.10 to 0.16.14
- Updated Cloth Config from 17.0.144 to 19.0.147
- Updated ModMenu from 13.0.3 to 15.0.0
- Updated Yarn mappings from 1.21.4+build.8 to 1.21.8+build.1

### Fixed
- Text rendering issues with new HUD API - text now properly displays with correct ARGB color format
- HUD element positioning changed from before chat to after crosshair for better rendering order

### Technical
- Refactored HUD rendering to use `HudElementRegistry.attachElementAfter()`
- Fixed text color format to use full ARGB (`0xFFFFFFFF`) instead of RGB
- Replaced `LookingAtRenderer` class with functional approach using lambda

## [0.9.0] - Previous Version

### Added
- Initial release of Block Entity Tooltip mod
- Display block names when looking at blocks
- Display entity names when looking at entities
- Configuration support via Cloth Config
- ModMenu integration for easy configuration access
- Configurable display settings (enable/disable blocks, entities, fluids)
- Configurable detection distance