# PORTING_STATUS

## Summary
This repository contains an experimental Forge 1.8.9 renderer with explicit vanilla fallbacks. It is not full upstream Argentum parity.

## Current compatibility changes
- `ArgentumRenderer` now uses unconditional cleanup for matrices, GL attributes, optional hooks, and the optional framebuffer pipeline.
- The optional framebuffer pipeline now records and restores the main framebuffer, cleans up after allocation/composite failures, and remains disabled by default.
- FeatherOpt is detected without a hard class-linkage dependency. Optional framebuffer effects are disabled by default when FeatherOpt is present because the installed client artifact and runtime interaction are not available for verification.
- No FeatherOpt source is vendored or modified.

## Feature matrix
- Forge 1.8.9 build path: Implemented in source; build requires ForgeGradle dependency access.
- Java 8 compatibility: Implemented in source.
- Terrain/model/fluid/VBO paths: Implemented as conservative prototype paths with vanilla fallback.
- Entity distance culling: Partial, opt-in, and guarded for named/leashed/mounted entities.
- Particle throttling: Partial, opt-in, with restoration behavior.
- Cloud renderer: Partial, opt-in, disabled when vanilla clouds or custom sky renderers are active.
- Item rendering optimization: Fallback-only.
- Block-entity optimization: Fallback-only.
- Weather replacement: Fallback-only on `main`; vanilla weather is preserved.
- Optional post-processing: Partial, capability/thread gated, FeatherOpt-conservative, self-disabling on failure.
- FeatherOpt compatibility: Source-level defensive fix implemented; affected runtime white-screen fix remains unverified without Feather/Dawn client logs and an installed FeatherOpt runtime.

## FeatherOpt investigation
Upstream FeatherOpt uses ForgeGradle 2.1 for Forge 1.8.9, SpongePowered Mixin, a `FeatherCoreMod` FML core plugin, `mixins.featheropt.json`, an access transformer, and Mixin launch configuration. ArgentumForge does not currently declare a coremod, Mixin config, or FeatherOpt dependency. Therefore no direct overlapping injection was found in this repository. The remaining risk is runtime OpenGL/framebuffer interaction with the installed FeatherOpt artifact, which cannot be proven from source alone.

## Validation status
- Deterministic pure-Java helper tests: source updated; execution must be performed in a Java/Gradle environment.
- `gradle clean build --stacktrace --no-daemon`: not run in this direct repository-edit session.
- Runtime Forge-only validation: not available here.
- FeatherOpt enabled / OptiFine disabled validation: not available here.
- FeatherOpt plus OptiFine validation: not available here.

Do not claim the white-screen issue is confirmed fixed until the affected Feather/Dawn configuration has been launched and logs/screenshots compared with and without ArgentumForge.
