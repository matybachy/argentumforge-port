# PORTING_STATUS

## Summary
This repository currently contains an experimental Forge 1.8.9 mod project with a working build skeleton and a chunk-mesh prototype. It is intentionally not yet the complete Argentum renderer.

## Feature matrix
- Project skeleton: Implemented
- Forge 1.8.9 build path: Implemented
- Java 8 compatibility: Implemented
- Client render hook: Implemented
- Basic VBO wrapper: Implemented
- Chunk-mesh prototype: Implemented
- Block-face mesher prototype: Implemented
- Rebuild queue: Implemented
- Advanced terrain renderer: In progress
- Correct opaque/cutout/translucent layering: Not started
- Texture atlas / UV mapping: Not started
- Lighting and AO: Not started
- Chunk visibility and frustum culling: Not started
- Entity/item instancing: Not started
- Cloud/weather renderer: Not started
- Post-processing / FXAA: Not started
- Video options / settings integration: Not started
- Compatibility modules: Not started
- Full upstream parity: Blocked by target-platform differences and rendering abstraction gaps

## Build results
- Current status: build not confirmed in this session; must be run locally with Java 8
- Command: ./gradlew clean build --stacktrace --no-daemon
- Expected output: build/libs/*.jar

## Runtime notes
- The experimental render path is toggled with `ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER` and can be disabled to preserve vanilla behavior.
- The current prototype is designed as a validation step for mesh generation and VBO rendering, not as a final renderer.

## Remaining blockers
- Upstream Argentum code relies on rendering subsystems and platform abstractions absent from standard Forge 1.8.9.
- Direct porting would require substantial reimplementation or compatibility shims.
- Full parity requires iterative, feature-by-feature porting and runtime validation in a real Minecraft client session.
