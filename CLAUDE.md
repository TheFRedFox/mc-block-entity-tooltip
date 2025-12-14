# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BET - Block'n'Entity Tooltip** is a Fabric Minecraft mod that displays the name of blocks and entities the player is looking at. Written in Kotlin using Fabric Loader with dependencies on Cloth Config and ModMenu for configuration management.

## Development Commands

### Essential Commands
- `./gradlew build` - Full build including tests
- `./gradlew clean` - Clean build directory
- `./gradlew jar` - Build mod jar only
- `./gradlew runClient` - Launch Minecraft client with mod for testing
- `./gradlew runDatagen` - Run data generation
- `./gradlew publishModrinth` - Publish to Modrinth (requires MODRINTH_TOKEN environment variable)

### Fabric-specific Commands
- `./gradlew genSources` - Decompile Minecraft sources (uses Mojang mappings)
- `./gradlew downloadAssets` - Download required game assets

## Architecture

### Core Components
- **Main mod class**: `BlockEntityTooltip.kt` - Server-side initialization 
- **Client mod class**: `BlockEntityTooltipClient.kt` - Client-side initialization and HUD rendering
- **Configuration**: `BlockEntityTooltipModConfig.kt` - Cloth Config integration for mod settings
- **ModMenu integration**: `BlockEntityTooltipModMenuIntegration.kt` - Configuration screen integration

### Key Features
- **HUD Rendering**: Uses Fabric's `HudElementRegistry` to register custom HUD elements
- **Raycasting**: Custom `Player.getLookingAt()` extension handles both block and entity detection
- **Configuration**: Runtime config with save callbacks using Cloth Config AutoConfig system
- **Mixins**: Uses client and server-side mixins for Minecraft integration

### Source Structure
- `src/main/` - Common code (server + client)
- `src/client/` - Client-only code (HUD rendering, config)
- Split environment source sets configured in `build.gradle`

## Important Configuration

### Version Information
- **Minecraft**: 1.21.11
- **Fabric Loader**: 0.18.1
- **Java**: 21 (required)
- **Kotlin**: 2.2.20
- **Fabric Loom**: 1.14.6
- **Gradle**: 9.2
- **Mappings**: Mojang (official)

### Dependencies
- Fabric API 0.139.5+1.21.11
- Fabric Language Kotlin 1.13.7+kotlin.2.2.21
- Cloth Config 21.11.151 (configuration UI)
- ModMenu 17.0.0-alpha.1 (integration for config screen)

### Development Environment
The mod uses Fabric Loom plugin with split environment source sets. Client and server code are separated, with mixins configured for both environments in respective `.mixins.json` files.

## Release Process

### Versioning Strategy
The mod uses semantic versioning with a special scheme to track Minecraft API compatibility:
- **Major version**: Minecraft API compatibility level (increments on breaking Minecraft changes)
- **Minor version**: Mod features (currently 9)
- **Patch version**: Bug fixes within a version branch

Example: `2.9.0` where `2` = MC 1.21.9 API level, `9` = feature set, `0` = no patches

### Branch Strategy
- `main` - Current Minecraft version (MC 1.21.9+, version 2.x.x)
- `1.21.6` - MC 1.21.6-1.21.8 (version 1.x.x)
- `1.21.4` - MC 1.21.4-1.21.5 (version 0.x.x)

### Releasing to Modrinth

#### Prerequisites
1. Set `MODRINTH_TOKEN` secret in GitHub repository settings
2. Ensure version is updated in `gradle.properties`
3. Update `CHANGELOG.md` with release notes

#### Via GitHub Actions (Recommended)
1. Create and push a git tag: `git tag v2.9.0+1.21.9 && git push origin v2.9.0+1.21.9`
2. Go to Actions → "Release to Modrinth" → "Run workflow"
3. Enter the tag (e.g., `v2.9.0+1.21.9`)
4. The workflow will:
   - Build from the tagged commit
   - Publish to Modrinth
   - Create a GitHub Release

#### Manual Release
```bash
export MODRINTH_TOKEN=your_token_here
./gradlew build publishModrinth
```

### Publishing Configuration
Modrinth publishing is configured in `build.gradle` using the `mod-publish-plugin`:
- **Project ID**: `block-entity-tooltip`
- **Dependencies**: Fabric API (required), Fabric Language Kotlin (required), ModMenu (optional), Cloth Config (optional)
- **Changelog**: Automatically reads from `CHANGELOG.md`