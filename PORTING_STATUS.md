# PORTING_STATUS

## Summary
This repository currently contains an experimental Forge 1.8.9 mod project with a coherent chunk-terrain renderer path and explicit vanilla fallbacks. It is intentionally not yet full Argentum parity.

## Feature matrix
- Project skeleton: Implemented
- Forge 1.8.9 build path: Implemented
- Java 8 compatibility: Implemented
- Client render hook: Implemented
- Basic VBO wrapper: Implemented
- Chunk-mesh prototype: Implemented
- Block-face mesher with fallback models: Implemented
- Rebuild queue with priority + per-tick budget: Implemented
- Correct opaque/cutout/translucent layering: Implemented
- Texture atlas / UV mapping (with fallback): Implemented
- Lighting and AO (lightweight): Implemented
- Chunk visibility and frustum culling: Implemented (distance/frustum-lite)
- Chunk/world/block invalidation hooks: Implemented
- Entity/item optimization hooks: Implemented (optional no-op + vanilla fallback)
- Cloud/weather hooks: Implemented (optional no-op + vanilla fallback)
- Optional post-effect pipeline: Implemented (capability-gated no-op fallback)
- Configuration/settings: Implemented (Forge config, disabled by default)
- Compatibility modules: Implemented (texture/lighting/GPU/effects interfaces)
- Full upstream parity: Blocked by target-platform differences and rendering abstraction gaps

## Build results
- Current status: blocked in this execution environment by inability to resolve ForgeGradle host
- Command: ./gradlew clean build --stacktrace --no-daemon
- Expected output: build/libs/*.jar (on network-enabled environment with ForgeGradle access)

## Runtime notes
- The experimental render path is toggled with `ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER` and can be disabled to preserve vanilla behavior.
- The config default leaves the experimental renderer disabled.
- Terrain path uses model/atlas/light fallbacks to stay buildable on Forge 1.8.9.

## Remaining blockers
- Upstream Argentum code relies on rendering subsystems and platform abstractions absent from standard Forge 1.8.9.
- Direct porting would require substantial reimplementation or compatibility shims.
- Full parity requires iterative, feature-by-feature porting and runtime validation in a real Minecraft client session.
