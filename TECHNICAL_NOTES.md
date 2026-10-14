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

The optional framebuffer path:

- requires framebuffer support and the client render thread
- allocates/resizes its own buffer defensively
- returns to the main framebuffer before and after compositing
- restores color, depth, alpha, and depth-mask state
- self-disables and falls back when allocation or rendering fails

## Validation
Run the pure-Java tests and the ForgeGradle build in a Java 8 environment. Then test separately:

1. Forge without FeatherOpt or OptiFine.
2. FeatherOpt with OptiFine disabled.
3. FeatherOpt with OptiFine enabled, if supported by the installed client.

No runtime client result should be claimed until those configurations are actually launched.

## Remaining limitations
Item/block-entity optimization remains fallback-only. Weather and post-processing are partial compatibility-first implementations, not full upstream Argentum/Celeritas parity.
