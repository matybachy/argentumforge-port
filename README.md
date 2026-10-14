# ArgentumForge

Experimental Forge 1.8.9 port of Argentum, built as a separate mod project in this repository.

## Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target
- Independent mod ID: `argentumforge`
- Late custom terrain overlay through `RenderWorldLastEvent`; vanilla terrain compilation and drawing remain active
- Experimental solid/cutout/translucent mesh prototype with chunk lifecycle hooks; it is not a replacement terrain renderer
- Terrain atlas binding, baked-model quad extraction, approximate fluid geometry and lighting/AO, and VBO reuse
- Chunk-radius and Minecraft 1.8.9 camera-plane frustum checks for the overlay; there is no section-visibility graph or occlusion culling
- Optional living-entity distance culling with compatibility guards
- Optional dropped-item distance adjustment and particle throttling with restoration behavior
- Optional cloud layer renderer, active only when vanilla clouds are disabled and no custom sky renderer is active
- Optional weather renderer approximation with distance and horizontal-forward filtering plus provider fallback
- Optional capability-gated framebuffer composite/tint pass for the late custom overlay, with render-thread and failure handling
- FeatherOpt detection without a hard class-linkage dependency; no FeatherOpt source is vendored or modified
- The vanilla renderer remains active; the experimental overlay and optional features are disabled by default

## Repository status
This repository is an experimental Forge 1.8.9 port, not full upstream Argentum/Celeritas parity. The project is kept separate from upstream repositories, which are used as read-only references.

## Build
Java 8 is required.

On Windows:

```text
.\gradlew.bat clean build --stacktrace --no-daemon
```

On other platforms:

```text
./gradlew clean build --stacktrace --no-daemon
```

Building requires ForgeGradle 2.1 dependencies to be available. Do not treat a build as verified unless it has actually been run successfully.

## Configuration
The generated Forge config controls renderer behavior. Relevant settings include:

- Renderer core: `enableExperimentalRenderer`, `renderChunkRadius`, `rebuildBudgetPerTick`
- Terrain: `enableLightingAo`, `enableTransparency`, `enableModelFallback`
- Optional effects: `enableOptionalEffects`, `optionalEffectsCompositeAlpha`, `optionalEffectsTintRed`, `optionalEffectsTintGreen`, `optionalEffectsTintBlue`
- Entities: `enableEntityHooks`, `enableEntityDistanceCulling`, `entityCullingDistance`
- Dropped items: `enableItemHooks`, `enableItemDistanceCulling`, `itemCullingDistance`
- Particles: `enableParticleHooks`, `particleThrottleSetting`
- Clouds: `enableCloudHooks`, `enableCloudRenderer`, `cloudRenderRadiusBlocks`
- Weather: `enableWeatherHooks`, `weatherRenderRadiusBlocks`, `weatherFrustumMinDotPercent`

Optional features use conservative settings and remain disabled by default unless explicitly enabled.

## Known limits
- This is not full upstream Argentum parity.
- Lighting, ambient occlusion, fluid geometry, and translucent ordering are conservative Forge 1.8.9 approximations.
- Model access or decoding failures use conservative fallback geometry.
- Item distance adjustment is partial. Block-entity optimization is not implemented.
- Chunk building is synchronous on the client tick and uses mutable live world state.
- Forge place/break events and chunk/world lifecycle events do not observe every possible direct modded world mutation.
- VBO overlay draw occurs after vanilla terrain and can duplicate it; no performance improvement has been measured.
- Weather rendering approximates vanilla rain/snow visuals and is not full visual parity.
- The cloud renderer uses a conservative flat cloud layer and avoids running alongside vanilla clouds or a custom sky renderer.
- Post-processing is a framebuffer composite/tint pass, not a modern shader chain.
- FeatherOpt compatibility is guarded defensively, but the white-screen issue remains unverified without real-client testing.
- Build, visual parity, compatibility, and performance require validation in a Java 8 / Forge 1.8.9 environment with a real client.

