# ArgentumForge

Experimental Forge 1.8.9 port of Argentum, built as a separate mod project in this repository.

Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target
- Independent mod ID: `argentumforge`
- Client render hook active: `RenderWorldLastEvent`
- Chunk renderer prototype with solid/cutout/translucent passes and chunk lifecycle hooks
- Terrain atlas binding via `TextureMap.locationBlocksTexture` with state/model/default-state lookup before missing-sprite fallback
- World-light sampling + lightweight AO approximation for face/vertex shading (including emissive full-bright fallback and corner-neighbor AO smoothing)
- Baked-model quad meshing for vanilla block models (including slabs/stairs/fences/panes/doors/trapdoors/rails/etc.) with state-aware bounds/collision-shape fallback and conservative cross/box geometry fallback
- Conservative fluid top/side/bottom geometry with averaged corner heights, same-material internal-face suppression, and translucent layering
- Conservative chunk visibility culling (radius + directional backface rejection) and chunk VBO buffer reuse
- Optional living-entity distance culling and particle throttling (opt-in, conservative, vanilla-compatible defaults)
- Optional conservative cloud layer renderer (opt-in, only active when vanilla clouds are disabled)
- Optional capability-gated framebuffer composite pass for LWJGL2-compatible post-processing fallback with thread/runtime safety checks
- Vanilla fallback preserved; experimental renderer is disabled by default and config-driven

Repository status
- This repo is a working prototype, not the complete upstream Argentum feature set.
- The project is intentionally kept private and separate from upstream repositories.
- Upstream Argentum and Celeritas are used as read-only reference sources only.

Build
- Java 8 is required.
- Run from the repo root:
  - ./gradlew clean build --stacktrace --no-daemon
  - or on Windows: .\gradlew.bat clean build --stacktrace --no-daemon

Configuration
- Generated config file controls renderer behavior:
  - `enableExperimentalRenderer` (default: false)
  - `renderChunkRadius`
  - `rebuildBudgetPerTick`
  - `enableLightingAo`, `enableTransparency`, `enableModelFallback`
  - optional guarded hooks: `enableOptionalEffects`, `optionalEffectsCompositeAlpha`, `enableEntityHooks`, `enableEntityDistanceCulling`, `entityCullingDistance`, `enableParticleHooks`, `particleThrottleSetting`, `enableItemHooks`, `enableBlockEntityHooks`, `enableWeatherHooks`, `enableCloudHooks`, `enableCloudRenderer`, `cloudRenderRadiusBlocks`

Known limits
- This is not full upstream Argentum parity.
- Lighting/AO and model support are conservative Forge 1.8.9 approximations.
- Model path still uses conservative per-face light/AO approximation and does not fully reproduce vanilla block-model AO weighting.
- Item, block-entity, and weather optimization remain fallback-only in this pass; vanilla rendering is always used and no replacement optimization path is advertised.
- Optional cloud rendering intentionally uses a conservative flat cloud layer and only activates when vanilla clouds are disabled and no custom sky renderer is present to avoid double-rendering/conflicts.
- Optional post-processing uses a capability-gated framebuffer composite pass and disables itself automatically on allocation/composite failures.
- Build/runtime verification in this execution environment may be blocked when ForgeGradle cannot resolve `files.minecraftforge.net`.
- Fluids and translucent ordering are chunk-level approximations and require runtime visual verification.
- The experimental renderer is disabled by default unless explicitly enabled for runtime validation.

This repository is intended as a private development workspace for a Forge 1.8.9 rendering port, not a public distribution.
