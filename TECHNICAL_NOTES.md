# Technical Notes

## Target environment
- Minecraft 1.8.9
- Forge 11.15.1.2318
- Java 8
- LWJGL 2
- Forge-only runtime; no Fabric or Ornithe runtime is assumed

## Current implementation
The project contains a staged Forge renderer with:

- baked-model terrain meshing and conservative fallback geometry
- world-light/AO approximation
- bounded chunk rebuild scheduling and invalidation hooks
- conservative chunk visibility culling and reusable VBOs
- opt-in entity culling and particle throttling
- opt-in cloud rendering with custom-sky fallback
- opt-in weather replacement with distance/frustum filtering
- capability/thread-gated framebuffer composite/tint effects
- explicit vanilla fallback paths

## PR #9 compatibility decisions
Weather rendering is installed only when the experimental renderer and weather hook are enabled. A non-null provider weather renderer is treated as custom and is never replaced. The controller removes only the renderer instance it installed. World changes, disabling the option, or renderer failure restore the provider's vanilla/null path.

The weather renderer performs render-only filtering. It does not modify rain strength, snow state, biome state, or gameplay behavior. Its output remains an approximation and requires client comparison against vanilla.

## Current implementation status
This repository currently includes:
- A Forge 1.8.9 mod project skeleton and Java 8-compatible Gradle setup
- A client hook via `RenderWorldLastEvent`
- A VBO-based chunk renderer with solid/cutout/translucent passes
- Texture-atlas binding and baked-model/state/default-state sprite lookup before missing-sprite fallback
- Baked-model quad decoding with state-aware bounds/collision-shape fallback
- World-light and lightweight AO sampling for approximate vertex brightness
- Conservative geometry fallback for unsupported or missing model data
- Bounded chunk rebuild scheduling and world/chunk/block invalidation hooks
- Conservative chunk visibility culling and reusable chunk VBO buffers
- Optional living-entity culling, dropped-item culling, and particle throttling
- Optional cloud rendering with vanilla/custom-sky fallback
- Optional weather replacement with distance/frustum filtering and vanilla/provider fallback
- Optional framebuffer composite effects with failure self-disable behavior and render-thread gating
- Compatibility interfaces for texture lookup, lighting, GPU buffers, and optional effects

## Optional framebuffer path
The optional framebuffer path:
- requires framebuffer support and the client render thread
- allocates/resizes its own buffer defensively
- attempts to return to the main framebuffer before and after compositing
- restores the depth-test and alpha-test enable states after compositing
- resets color and depth-write state after compositing
- self-disables and falls back when allocation or rendering fails

## Validation
Run the pure-Java tests and the ForgeGradle build in a Java 8 environment. Then test separately:

1. Forge without FeatherOpt or OptiFine.
2. FeatherOpt with OptiFine disabled.
3. FeatherOpt with OptiFine enabled, if supported by the installed client.

No runtime client result should be claimed until those configurations are actually launched.

## Remaining limitations and risk posture
- Item optimization remains partial, and block-entity optimization remains fallback-only because Forge 1.8.9 does not expose a safe generic TESR pre-render hook for conservative culling without replacing renderer registrations.
- Weather rendering is a compatibility-first approximation, not full vanilla or upstream parity.
- Post-processing is a guarded framebuffer composite/tint pass, not a full shader-chain implementation.
- Fluid shape/UV parity, smooth-light/AO parity, translucent ordering, terrain visuals, and performance require real-client testing.
- Upstream code relies on rendering abstractions that are not present in standard Forge 1.8.9. Full parity would require an equivalent compatibility layer or substantial reimplementation.
- Runtime validation is unavailable in this repository-edit session. Do not claim production readiness or a confirmed fix for the Feather/Dawn white screen without testing the affected configurations.
- This repository is the private development port; upstream repositories remain read-only references.