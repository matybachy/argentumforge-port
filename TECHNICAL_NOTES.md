# Technical Notes

## Target environment
- Minecraft: 1.8.9
- Forge: 11.15.1.2318 (targeted and validated by the build system)
- Java: 8
- Graphics runtime: standard LWJGL 2
- Loader: Forge only; no Fabric or Ornithe runtime is required or assumed

## Upstream references used
- Argentum: https://github.com/QuicksilverMC/Argentum
- Celeritas: https://github.com/GTNewHorizons/Celeritas

These are treated as read-only architecture references only. No upstream code is committed to this repository.

## Important compatibility constraints
The upstream Argentum renderer is written for a different rendering stack than standard Forge 1.8.9. It relies on modern rendering abstractions, shader systems, and render-device/tooling patterns that do not map 1:1 to standard Minecraft Forge 1.8.9. The main compatibility issues are:

- modern GL abstractions and shader framework concepts not present in vanilla MC 1.8.9
- newer model/resource patterns not available in standard Forge 1.8.9
- custom Embeddium/Celeritas integration points not available in a standalone Forge mod
- instancing and GPU-resource patterns that must be adapted for LWJGL2 and the target mappings

## Current implementation status
This repository currently includes:
- a Forge 1.8.9 mod project skeleton
- Java 8-compatible Gradle build setup
- client hook via RenderWorldLastEvent
- a VBO-based chunk renderer with solid/cutout/translucent passes
- texture-atlas binding (`TextureMap.locationBlocksTexture`) and baked-model sprite UV lookup with missing-sprite fallback
- baked-model quad decoding for block geometry, with conservative fallback geometry if model access/decoding fails
- world-light + lightweight AO sampling for per-vertex brightness approximation (including emissive full-bright fallback and corner-neighbor AO smoothing)
- conservative model fallback geometry (block bounds, crossed quads for plant-like blocks, fluid corner heights + side/bottom faces)
- prioritized, bounded chunk rebuild scheduling with world/chunk/block invalidation hooks (including MultiPlace updates and edge-neighbor dirtying)
- conservative chunk visibility culling with radius checks plus directional backface rejection for distant chunks
- reusable chunk VBO allocation strategy that updates in-place when capacity permits
- optional living-entity distance culling and particle throttling hooks (both opt-in and disabled by default)
- optional conservative cloud layer rendering (opt-in; active only when vanilla clouds are disabled to avoid double rendering)
- optional capability-gated framebuffer composite effect pipeline with failure self-disable fallback
- compatibility interfaces for texture lookup, lighting, GPU buffers, and optional effects

This is intentionally a staged prototype and not a full Argentu m-equivalent renderer yet.

## Planned porting strategy
1. Build a correct Forge 1.8.9 terrain path with chunk meshing, rebuild scheduling and camera-relative rendering
2. Separate render layers and add correct block visibility checks
3. Add texture/UV mapping and lighting/AO
4. Add chunk resource reuse and safe cleanup
5. Port higher-level effects such as clouds, weather, instancing, and item/entity optimizations in smaller steps
6. Keep vanilla terrain as fallback until the experimental path is stable

## Render prototype notes
The current prototype intentionally avoids claiming full terrain correctness. It is used to validate:
- Forge client event integration
- OpenGL rendering with LWJGL2
- VBO upload and draw calls
- camera-relative transforms
- chunk mesh generation and bounded rebuild processing on the client thread
- stable fallback behavior when model/texture resolution is unavailable

## Validation steps
Runtime validation should be performed in a real 1.8.9 client session:
1. Enable `enableExperimentalRenderer=true` in the generated config file.
2. Start client and load a world.
3. Verify terrain in all three passes (solid/cutout/translucent), including water/glass/leaves.
4. Validate representative model-heavy blocks (stairs/slabs/fences/panes/doors/trapdoors/rails/ladders/buttons/plates/plants/signs) against vanilla output.
5. Place/break blocks near chunk borders and verify dirty rebuilds.
6. Change dimension and verify mesh disposal/rebuild without stale VBOs.

## Current truth-in-advertising status
- Implemented: baked-model quad meshing path with safe fallback for unsupported/missing model data.
- Partially implemented: fluid shape/UV parity and smooth-light/AO parity (current implementation is conservative and stable, not 1:1 vanilla; same-material liquid internals are now suppressed, emissive blocks remain full-bright, and AO uses local corner-neighbor sampling).
- Partially implemented: optional performance hooks (living-entity distance culling + particle throttle + conservative cloud layer + framebuffer composite post pass).
- Fallback-only in this pass: item rendering optimization, block-entity optimization, and weather replacement rendering to preserve vanilla/custom renderer compatibility on Forge 1.8.9.
- Unverified in this execution environment: real in-game visual parity/performance due unavailable runtime client validation here.
- Platform-blocked: direct parity with upstream Argentum/Celeritas renderer abstractions beyond what Forge 1.8.9 exposes.

## Known blockers
- Upstream code makes extensive use of custom render abstractions not present in standard Forge 1.8.9.
- A faithful 1:1 port of the entire upstream renderer is not feasible without an equivalent compatibility layer or substantial reimplementation.
- Real runtime validation is required before any terrain-rendering feature can be called production-ready.

## Risk posture
The project remains intentionally private. The code in this repository is the working porting branch only; upstream repositories remain untouched and read-only.
