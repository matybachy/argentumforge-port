# ArgentumForge

Experimental Forge 1.8.9 port of Argentum rendering ideas, built as a separate mod project in this repository.

## Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target, independent mod ID `argentumforge`
- Every rendering change is opt-in behind `enableExperimentalRenderer` (default `false`); with it off the vanilla renderer runs unchanged. The only exception is the font width cache, which gives identical results and has its own switch
- `terrainMode=integrated` (default when the master switch is on): ArgentumForge's section compiler is installed inside `RenderGlobal`'s own chunk lifecycle. It replaces the `RenderGlobal` chunk factory and render container, so vanilla's worker threads compile every terrain section through ArgentumForge `RenderChunk` subclasses, and vanilla's own solid/cutout-mipped/cutout/translucent passes draw them. There is no overlay and nothing is drawn twice
  - Each compile task gets an immutable 20x20x20 snapshot (empty sections store nothing; only already-loaded chunks are read) of block states, raw light, biomes and tile-entity references, captured on the client thread. Worker-side block rendering reads the snapshot through Forge's `RenderChunk.createRegionRenderCache` hook instead of live chunks. Vanilla/Forge block, fluid, model, AO/lightmap, tint, layer and translucency-sort code still does the meshing
  - Per-frame capture budget, a cap on pending snapshots, per-task pairing, and discard on position change or GL deletion. If the budget runs out or a capture fails, that section is compiled from the vanilla region cache
  - The snapshot-backed `RegionRenderCache` is also constructed on the client thread, so its superclass constructor never reads the live world on a worker
  - Second-stage culling of each section layer by the bounds of blocks that can render in that layer (one-block margin) against the camera side planes; lock-free per-frame lookup
  - Disabled automatically when OptiFine is detected, and optionally when FeatherOpt is present
- `terrainMode=overlay`: legacy late `RenderWorldLastEvent` overlay (can duplicate vanilla terrain; kept for comparison only). Its meshers now use primitive float buffers
- Optional distance culling for the vanilla chest, ender chest, sign, skull, banner, mob spawner and enchantment table renderers
- Optional living-entity distance culling with compatibility guards (cached class-eligibility checks)
- Optional non-living entity distance culling (`enableNonLivingEntityCulling`) for the exact vanilla arrow, XP orb, snowball, egg, ender pearl and XP bottle renderers. The vanilla renderer instances are wrapped and restored when the option is disabled
- Optional dropped-item distance adjustment and particle-setting throttling with restoration behavior
- Optional cloud layer renderer, active only when vanilla clouds are off and there is no custom sky renderer
- Optional weather approximation with per-tick cached biome/precipitation columns (rain or snow pass skipped when no column needs it), distance and forward filtering, and provider fallback
- Optional framebuffer composite/tint pass, applied only to the legacy overlay pass
- Particle frustum culling (Mixin into `EffectRenderer.renderParticles`) for vanilla particles in the batched layers, with a 2-block margin; F3 shows how many were skipped
- `FontRenderer.getStringWidth` cache (Mixin), cleared on resource reload and unicode-flag change; on by default and independent of the master switch
- In-game settings screen: Mods list -> ArgentumForge -> Config, or the "Open ArgentumForge settings" key (unbound by default in Controls). Changes apply without a restart
- FeatherOpt and OptiFine detected at runtime without class linkage (memoized); no FeatherOpt source is vendored or modified

## Repository status
This is an experimental port. It does not reach full upstream Argentum/Celeritas parity. Upstream repositories are used only as read-only references. See `PORTING_STATUS.md` for the feature matrix and `TECHNICAL_NOTES.md` for lifecycle details.

**Validation:** `./gradlew build` compiles, runs the 15 unit tests (all pass) and produces the reobfuscated jar. `./gradlew runClient -PsmokeTest` passes in the Forge 1.8.9 dev client. Details are in `TECHNICAL_NOTES.md` under "Validation record". Not verified: loading the jar from `mods/` in a non-dev Forge install, compatibility with OptiFine or FeatherOpt, visual parity across many worlds and resource packs, and controlled performance measurements.

## Build
Java 8 is required (`JAVA_HOME` must point at a JDK 8; Gradle 2.7 does not run on newer Java). ForgeGradle 2.1, MixinGradle 0.6, Forge `1.8.9-11.15.1.2318-1.8.9`, MCP `stable_22`.

```text
./gradlew setupDecompWorkspace
./gradlew build
```

`build` compiles, runs the unit tests, generates `mixins.argentumforge.refmap.json` and writes the reobfuscated mod to `build/libs/argentumforge-<version>.jar`, with Mixin 0.7.11 embedded. Drop that jar into a Forge 1.8.9 `mods/` folder.

Development client:

```text
./gradlew runClient
./gradlew runClient -PsmokeTest
```

`-PsmokeTest` creates a creative world, runs a vanilla phase, enables every feature in memory (the config file is not changed), spawns particles, checks the terrain, particle and font counters, saves `run/screenshots/argentum-smoke.png` and quits. The log ends with `[SmokeTest] PASS` or `[SmokeTest] FAIL [...]`.

## Configuration
The generated Forge config controls renderer behavior:

- Master switch: `enableExperimentalRenderer`
- Terrain path: `terrainMode` (`integrated` or `overlay`)
- Integrated terrain: `terrainSnapshotsPerFrame`, `terrainMaxPendingSnapshots`, `enableTerrainBoundsCulling`
- Legacy overlay: `renderChunkRadius`, `rebuildBudgetPerTick`, `enableLightingAo`, `enableTransparency`, `enableModelFallback`
- Compatibility: `disableExperimentalRendererWithFeatherOpt`, `disableOptionalEffectsWithFeatherOpt`
- Optional effects (overlay only): `enableOptionalEffects`, `optionalEffectsCompositeAlpha`, `optionalEffectsTintRed`, `optionalEffectsTintGreen`, `optionalEffectsTintBlue`
- Block entities: `enableBlockEntityHooks`, `enableBlockEntityDistanceCulling`, `blockEntityCullingDistance`
- Entities: `enableEntityHooks`, `enableEntityDistanceCulling`, `entityCullingDistance`, `enableNonLivingEntityCulling`, `nonLivingEntityCullingDistance`
- Dropped items: `enableItemHooks`, `enableItemDistanceCulling`, `itemCullingDistance`
- Particles: `enableParticleHooks`, `particleThrottleSetting`, `enableParticleCulling`
- Fonts: `enableFontWidthCache`
- Clouds: `enableCloudHooks`, `enableCloudRenderer`, `cloudRenderRadiusBlocks`
- Weather: `enableWeatherHooks`, `weatherRenderRadiusBlocks`, `weatherFrustumMinDotPercent`

With `enableExperimentalRenderer=true`, the F3 screen shows integrated-terrain counters (snapshot builds, vanilla-cache builds, fallbacks and culled layers) and, with particle culling on, culled particles.

## Known limits
- No performance improvement has been measured. Snapshot capture moves block, light and biome reads from worker threads to the client thread, bounded by `terrainSnapshotsPerFrame`. Whether this helps frame time is unknown.
- Integrated mode keeps vanilla section visibility (the frustum and cave-culling graph in `setupTerrain`). There is no GPU occlusion culling and no custom visibility graph.
- Tile-entity objects referenced by a snapshot are live objects. Block models that read them on a worker thread have the same thread-safety as vanilla.
- Name-tag, GUI/HUD, item-model and enchantment-glint optimization is not implemented, and particles are culled but not batched; `PORTING_STATUS.md` records the reasons.
- Weather and clouds are approximations, not vanilla-equivalent renderers.
- FeatherOpt and OptiFine runtime compatibility is unverified.
