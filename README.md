# ArgentumForge

Experimental Forge 1.8.9 port of Argentum rendering ideas, built as a separate mod project in this repository.

## Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target, independent mod ID `argentumforge`
- Everything is opt-in behind `enableExperimentalRenderer` (default `false`); with it off the vanilla renderer runs unchanged
- `terrainMode=integrated` (default when the master switch is on): ArgentumForge's section compiler is installed inside `RenderGlobal`'s own chunk lifecycle. It replaces the `RenderGlobal` chunk factory and render container, so vanilla's worker threads compile every terrain section through ArgentumForge `RenderChunk` subclasses, and vanilla's own solid/cutout-mipped/cutout/translucent passes draw them. There is no overlay and nothing is drawn twice
  - Each compile task gets an immutable 20x20x20 snapshot (empty sections store nothing; only already-loaded chunks are read) of block states, raw light, biomes and tile-entity references, captured on the client thread. Worker-side block rendering reads the snapshot through Forge's `RenderChunk.createRegionRenderCache` hook instead of live chunks. Vanilla/Forge block, fluid, model, AO/lightmap, tint, layer and translucency-sort code still does the meshing
  - Per-frame capture budget, a cap on pending snapshots, per-task pairing, and discard on position change or GL deletion. If the budget runs out or a capture fails, that section is compiled from the vanilla region cache
  - The snapshot-backed `RegionRenderCache` is also constructed on the client thread, so its superclass constructor never reads the live world on a worker
  - Second-stage culling of each section layer by the bounds of blocks that can render in that layer (one-block margin) against the camera side planes; lock-free per-frame lookup
  - Disabled automatically when OptiFine is detected, and optionally when FeatherOpt is present
- `terrainMode=overlay`: legacy late `RenderWorldLastEvent` overlay (can duplicate vanilla terrain; kept for comparison only). Its meshers now use primitive float buffers
- Optional distance culling for the vanilla chest, ender chest, sign, skull, banner, mob spawner and enchantment table renderers
- Optional living-entity distance culling with compatibility guards (cached class-eligibility checks)
- Optional dropped-item distance adjustment and particle-setting throttling with restoration behavior
- Optional cloud layer renderer, active only when vanilla clouds are off and there is no custom sky renderer
- Optional weather approximation with per-tick cached biome/precipitation columns (rain or snow pass skipped when no column needs it), distance and forward filtering, and provider fallback
- Optional framebuffer composite/tint pass, applied only to the legacy overlay pass
- FeatherOpt and OptiFine detected at runtime without class linkage (memoized); no FeatherOpt source is vendored or modified

## Repository status
This is an experimental port. It does not reach full upstream Argentum/Celeritas parity. Upstream repositories are used only as read-only references. See `PORTING_STATUS.md` for the feature matrix and `TECHNICAL_NOTES.md` for lifecycle details.

**Nothing in this revision has been compiled, built, tested or launched.** No Gradle task, `javac`, unit test or Minecraft client was run. Treat every feature below as unverified until it is built and exercised in a real Forge 1.8.9 client.

## Build
Java 8 is required. The `minecraft.version` coordinate is `1.8.9-11.15.1.2318-1.8.9`, the form used by the official 2318 MDK and published on maven.minecraftforge.net.

```text
./gradlew clean build --stacktrace --no-daemon
```

The repository ships only `gradlew` and `gradle/wrapper/gradle-wrapper.properties` (Gradle 2.14.1). The official 2318 MDK pins Gradle 2.7. Neither combination has been run for this revision.

## Configuration
The generated Forge config controls renderer behavior:

- Master switch: `enableExperimentalRenderer`
- Terrain path: `terrainMode` (`integrated` or `overlay`)
- Integrated terrain: `terrainSnapshotsPerFrame`, `terrainMaxPendingSnapshots`, `enableTerrainBoundsCulling`
- Legacy overlay: `renderChunkRadius`, `rebuildBudgetPerTick`, `enableLightingAo`, `enableTransparency`, `enableModelFallback`
- Compatibility: `disableExperimentalRendererWithFeatherOpt`, `disableOptionalEffectsWithFeatherOpt`
- Optional effects (overlay only): `enableOptionalEffects`, `optionalEffectsCompositeAlpha`, `optionalEffectsTintRed`, `optionalEffectsTintGreen`, `optionalEffectsTintBlue`
- Block entities: `enableBlockEntityHooks`, `enableBlockEntityDistanceCulling`, `blockEntityCullingDistance`
- Entities: `enableEntityHooks`, `enableEntityDistanceCulling`, `entityCullingDistance`
- Dropped items: `enableItemHooks`, `enableItemDistanceCulling`, `itemCullingDistance`
- Particles: `enableParticleHooks`, `particleThrottleSetting`
- Clouds: `enableCloudHooks`, `enableCloudRenderer`, `cloudRenderRadiusBlocks`
- Weather: `enableWeatherHooks`, `weatherRenderRadiusBlocks`, `weatherFrustumMinDotPercent`

With `enableExperimentalRenderer=true`, the F3 screen shows integrated-terrain counters: snapshot builds, vanilla-cache builds, fallbacks and culled layers.

## Known limits
- No performance improvement has been measured. Snapshot capture moves block, light and biome reads from worker threads to the client thread, bounded by `terrainSnapshotsPerFrame`. Whether this helps frame time is unknown.
- Integrated mode keeps vanilla section visibility (the frustum and cave-culling graph in `setupTerrain`). There is no GPU occlusion culling and no custom visibility graph.
- Tile-entity objects referenced by a snapshot are live objects. Block models that read them on a worker thread have the same thread-safety as vanilla.
- Particle, font, name-tag, GUI/HUD, item-model and enchantment-glint optimization is not implemented; `PORTING_STATUS.md` records the specific technical reasons.
- Weather and clouds are approximations, not vanilla-equivalent renderers.
- FeatherOpt and OptiFine runtime compatibility is unverified.
