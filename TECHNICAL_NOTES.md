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
- a VBO-based chunk render prototype
- an early block-face mesher that emits debug geometry
- a rebuild queue and simple chunk manager to schedule prototype mesh generation

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
- chunk mesh generation on the client thread

## Known blockers
- Upstream code makes extensive use of custom render abstractions not present in standard Forge 1.8.9.
- A faithful 1:1 port of the entire upstream renderer is not feasible without an equivalent compatibility layer or substantial reimplementation.
- Real runtime validation is required before any terrain-rendering feature can be called production-ready.

## Risk posture
The project remains intentionally private. The code in this repository is the working porting branch only; upstream repositories remain untouched and read-only.
