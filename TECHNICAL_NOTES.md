# ArgentumForge Technical Notes

## Target and entry points

- Minecraft 1.8.9, Forge 11.15.1.2318, Java 8, LWJGL 2.
- `ArgentumForge` loads the config and delegates to the sided proxy.
- `ClientProxy` registers itself and `ClientWorldHooks` on `MinecraftForge.EVENT_BUS`, enables/disables the shared manager, and calls its tick scheduler.
- `ArgentumRenderer` registers `RenderWorldLastEvent` and `OptionalRenderHooks`. The experimental terrain flag defaults off.

## Terrain path and Async Worker Architecture

`ChunkMeshManager` creates an immutable `ChunkSnapshot` capturing block states, light levels, and biomes on the client thread. The snapshot task is submitted to an `ExecutorService` worker thread pool. `BlockFaceMesher` generates primitive `FloatArrayList` vertex arrays containing 11 floats per vertex (position x,y,z, texture u,v, lightmap u,v, and RGBA).

Completed mesh data is queued and uploaded to LWJGL 2 VBOs (`GlVbo`) on the main render thread. Stale builds are rejected using chunk revision counters.

At draw time, `ArgentumRenderer.onRenderWorldLast` binds the block atlas and draws the VBOs. Forge's `RenderWorldLastEvent` is not a terrain-layer replacement hook; vanilla `RenderGlobal` still draws vanilla terrain first.

## Other connected hooks

- TileEntity culling checks distance thresholds in `OptionalRenderHooks`.
- Living entity culling listens to cancelable Forge `RenderLivingEvent.Pre` with bounding box checks.
- Item culling adjusts `EntityItem.renderDistanceWeight` after a configurable threshold.
- Particle throttling temporarily changes `GameSettings.particleSetting` while preserving user setting changes.
- `FontAndGlintOptimization` manages state optimization for text and enchantment glint rendering.

## Build configuration

The Forge coordinate in `build.gradle` was updated statically to `1.8.9-11.15.1.2318-1.8.9` for ForgeGradle 2.1 setup compatibility without executing Gradle.

## Validation record

No Gradle command, Java compilation, unit/integration test, Minecraft launch, or runtime check was performed.
