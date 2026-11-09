# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.1+1.21.3] - 2026-11-09

**Compatible with Minecraft 1.21.3**

The `minecraft` range in `fabric.mod.json` starts at 1.21.3. Earlier Modrinth
listings also named 1.21.2, where the jar never loaded.

### Changed
- Updated ModMenu to 12.0.1
- Updated Fabric Loader to 0.20.1 (was 0.16.9)
- Updated Fabric Language Kotlin to 1.14.2+kotlin.2.4.21 (was 1.11.0+kotlin.2.0.0)
- Updated Kotlin to 2.4.21 (was 2.1.10)
- **Requires Fabric Language Kotlin 1.14.2+kotlin.2.4.21 or newer.** The
  mod is built with Kotlin 2.4 and can crash on an older Kotlin runtime.

### Technical
- The jar name and the Modrinth version number carry the MC version
  (`0.9.1+1.21.3`).
- The Fabric Language Kotlin floor in `fabric.mod.json` comes from
  `fabric_kotlin_version` in `gradle.properties`.
