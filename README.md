# ArgentumForge

Experimental Forge 1.8.9 port of Argentum, built as a separate mod project in this repository.

## Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target
- Independent mod ID: `argentumforge`
- Client render hook via `RenderWorldLastEvent`
- Chunk renderer prototype with solid/cutout/translucent passes and chunk lifecycle hooks
- Terrain atlas binding, baked-model quad meshing, conservative fluid geometry, lighting/AO approximation, and VBO reuse
- Optional entity distance culling and particle throttling with conservative vanilla fallback
- Optional cloud layer renderer only when vanilla clouds are disabled and no custom sky renderer is active
- Optional capability-gated framebuffer composite/tint pass with thread and failure safety
- Optional conservative weather renderer replacement with distance/frustum filtering and automatic fallback
- Vanilla fallback preserved; experimental renderer disabled by default and config-driven

## Build
Java 8 is required:

```text
./gradlew clean build --stacktrace --no-daemon
```

On Windows:

```text
.\gradlew.bat clean build --stacktrace --no-daemon
```

## Configuration
The generated Forge config controls renderer behavior, including:

- `enableExperimentalRenderer`
- `enableOptionalEffects`
- `optionalEffectsCompositeAlpha`
- `optionalEffectsTintRed`, `optionalEffectsTintGreen`, `optionalEffectsTintBlue`
- `enableEntityHooks`, `enableEntityDistanceCulling`, `entityCullingDistance`
- `enableParticleHooks`, `particleThrottleSetting`
- `enableWeatherHooks`, `weatherRenderRadiusBlocks`, `weatherFrustumMinDotPercent`
- `enableCloudHooks`, `enableCloudRenderer`, `cloudRenderRadiusBlocks`
- `enableItemHooks` and `enableBlockEntityHooks` remain compatibility toggles with vanilla fallback

All optional features default to disabled or conservative settings.

## Known limits
- This is not full upstream Argentum parity.
- Lighting, AO, fluid geometry, and translucent ordering are conservative Forge 1.8.9 approximations.
- Weather optimization is partial and compatibility-first; vanilla/provider renderers remain the fallback.
- Item and block-entity optimization remain fallback-only because safe generic Forge 1.8.9 interception is unavailable.
- Post-processing is a framebuffer composite/tint path, not a modern shader chain.
- Build and runtime verification require a Java 8/Forge 1.8.9 environment and a real client.
