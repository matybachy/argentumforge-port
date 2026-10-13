# ArgentumForge

Experimental Forge 1.8.9 port of Argentum, built as a separate mod project in this repository.

Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target
- Independent mod ID: `argentumforge`
- Client render hook active: `RenderWorldLastEvent`
- Chunk renderer prototype with solid/cutout/translucent passes and chunk lifecycle hooks
- Terrain atlas binding via `TextureMap.locationBlocksTexture` with baked-model sprite lookup and missing-sprite fallback
- World-light sampling + lightweight AO approximation for face/vertex shading
- Baked-model quad meshing for vanilla block models (including slabs/stairs/fences/panes/doors/trapdoors/rails/etc.) with conservative block-bounds/crossed fallback
- Conservative fluid top/side geometry with averaged corner heights and translucent layering
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
  - optional guarded hooks: `enableOptionalEffects`, `enableEntityHooks`, `enableWeatherHooks`, `enableCloudHooks`

Known limits
- This is not full upstream Argentum parity.
- Lighting/AO and model support are conservative Forge 1.8.9 approximations.
- Model path still uses conservative per-face light/AO approximation and does not fully reproduce vanilla block-model AO weighting.
- Optional effects/entity/weather/cloud hooks currently default to vanilla/no-op fallback.
- Fluids and translucent ordering are chunk-level approximations and require runtime visual verification.
- The experimental renderer is disabled by default unless explicitly enabled for runtime validation.

This repository is intended as a private development workspace for a Forge 1.8.9 rendering port, not a public distribution.
