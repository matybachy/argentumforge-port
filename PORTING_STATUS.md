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
- Baked-model quad terrain meshing for vanilla blockstate models: Implemented (with fallback when unavailable)
- Rebuild queue with priority + per-tick budget: Implemented (distance-prioritized and radius-filtered candidates)
- Correct opaque/cutout/translucent layering: Implemented
- Texture atlas / UV mapping (with missing-sprite fallback): Implemented
- Lighting and AO (lightweight): Implemented (conservative per-vertex approximation with emissive full-bright fallback and corner-neighbor AO sampling)
- Chunk visibility and frustum culling: Implemented (radius + conservative directional backface culling)
- Chunk/world/block invalidation hooks: Implemented (chunk load/unload, block place/break, multiplace, and chunk-edge spillover)
- Chunk VBO lifecycle/reuse: Implemented (persistent VBOs with in-place subdata uploads when capacity allows)
- Entity/item optimization hooks: Implemented (optional no-op + vanilla fallback)
- Cloud/weather hooks: Implemented (optional no-op + vanilla fallback)
- Optional post-effect pipeline: Implemented (capability-gated no-op fallback)
- Configuration/settings: Implemented (Forge config, disabled by default)
- Compatibility modules: Implemented (texture/lighting/GPU/effects interfaces)
- Deterministic pure-Java helper tests: Implemented (`MeshMathTest`)
- Entity/item/particle optimizations: Fallback-only (opt-in hooks exist; no broad replacement enabled by default)
- Cloud/weather optimization: Fallback-only (opt-in hooks exist; vanilla remains compatibility path)
- Post-processing pipeline: Fallback-only (capability-gated no-op unless explicitly enabled and supported)
- Full upstream parity: Blocked by target-platform differences and rendering abstraction gaps

## Build results
- Current status: blocked in this execution environment by inability to resolve ForgeGradle host
- Command: ./gradlew clean build --stacktrace --no-daemon
- Expected output: build/libs/*.jar (on network-enabled environment with ForgeGradle access)

## Runtime notes
- The experimental render path is toggled with `ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER` and can be disabled to preserve vanilla behavior.
- The config default leaves the experimental renderer disabled.
- Terrain path uses model/atlas/light fallbacks to stay buildable on Forge 1.8.9.
- Unsupported/failed model decoding falls back to bounds/cross/fluid geometry instead of dropping terrain faces.
- Fluid meshing now suppresses same-material internal faces (including flowing/still variants), uses Forge-style falling-level normalization, and renders visible bottom faces.

## Remaining blockers
- Upstream Argentum code relies on rendering subsystems and platform abstractions absent from standard Forge 1.8.9.
- Direct porting would require substantial reimplementation or compatibility shims.
- Full parity requires iterative, feature-by-feature porting and runtime validation in a real Minecraft client session.
- Exact vanilla translucent ordering and full vanilla AO parity remain partial and require additional runtime comparison/tuning.
- Runtime Minecraft visual/performance verification remains unverified in this environment due ForgeGradle host resolution failure.
