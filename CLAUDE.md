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

### Fabric-specific Commands
- `./gradlew genSources` - Decompile Minecraft sources (use when working with unmapped code)
- `./gradlew migrateMappings` - Update to newer yarn mappings
- `./gradlew downloadAssets` - Download required game assets

## Architecture

### Core Components
- **Main mod class**: `BlockEntityTooltip.kt` - Server-side initialization 
- **Client mod class**: `BlockEntityTooltipClient.kt` - Client-side initialization and HUD rendering
- **Configuration**: `BlockEntityTooltipModConfig.kt` - Cloth Config integration for mod settings
- **ModMenu integration**: `BlockEntityTooltipModMenuIntegration.kt` - Configuration screen integration

### Key Features
- **HUD Rendering**: Uses Fabric's `HudElementRegistry` to register custom HUD elements
- **Raycasting**: Custom `PlayerEntity.getLookingAt()` extension handles both block and entity detection
- **Configuration**: Runtime config with save callbacks using Cloth Config AutoConfig system
- **Mixins**: Uses client and server-side mixins for Minecraft integration

### Source Structure
- `src/main/` - Common code (server + client)
- `src/client/` - Client-only code (HUD rendering, config)
- Split environment source sets configured in `build.gradle`

## Important Configuration

### Version Information
- **Minecraft**: 1.21.8
- **Fabric Loader**: 0.16.14
- **Java**: 21 (required)
- **Kotlin**: 2.1.10
- **Fabric Loom**: 1.10.4

### Dependencies
- Fabric API 0.133.4+1.21.8
- Fabric Language Kotlin 1.13.1+kotlin.2.1.10
- Cloth Config 19.0.147 (configuration UI)
- ModMenu 15.0.0 (integration for config screen)

### Development Environment
The mod uses Fabric Loom plugin with split environment source sets. Client and server code are separated, with mixins configured for both environments in respective `.mixins.json` files.