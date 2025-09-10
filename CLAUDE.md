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
- **HUD Rendering**: Custom HUD layer for displaying tooltips
- **Raycasting**: Custom `PlayerEntity.getLookingAt()` extension handles both block and entity detection
- **Configuration**: Runtime config with save callbacks using Cloth Config AutoConfig system
- **Mixins**: Uses client and server-side mixins for Minecraft integration

### Source Structure
- `src/main/` - Common code (server + client)
- `src/client/` - Client-only code (HUD rendering, config)
- Split environment source sets configured in `build.gradle`