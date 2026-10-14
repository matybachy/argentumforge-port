# PORTING_STATUS

## Summary
This repository contains an experimental Forge 1.8.9 renderer with explicit vanilla fallbacks. It is not full upstream Argentum parity.

## Feature matrix
- Project skeleton: Implemented
- Forge 1.8.9 build path: Implemented in source; validation depends on ForgeGradle dependency availability
- Java 8 compatibility: Implemented in source
- Client render hook: Implemented
- Terrain/model/fluid/VBO paths: Implemented as conservative prototype paths with vanilla fallback
- Entity distance culling: Partial, opt-in, guarded for named/leashed/mounted entities
- Dropped-item distance culling: Partial, opt-in, with restoration behavior
- Particle throttling: Partial, opt-in, with restoration behavior
- Cloud renderer: Partial, opt-in, disabled when vanilla clouds or custom sky renderers are active
- Item rendering optimization: Partial, opt-in, with vanilla-compatible defaults
- Block-entity optimization: Fallback-only
- Weather renderer replacement: Partial, opt-in, conservative distance/frustum filtering with vanilla/provider fallback
- Optional post-processing: Partial framebuffer composite/tint path, capability/thread gated and self-disabling on failure
- Configuration/settings: Implemented, experimental features disabled by default
- Deterministic pure-Java helper tests: Added; execution not verified here

## PR #9 compatibility and conflict-resolution status
- Weather replacement is installed only when the experimental renderer and `enableWeatherHooks` are enabled.
- Existing custom/provider weather renderers are not replaced.
- The controller removes only the renderer instance it installed.
- Disabling the feature, changing worlds, or encountering a runtime error restores the vanilla/provider path.
- Weather filtering is render-only and does not change precipitation or gameplay state.
- Post-processing remains optional, framebuffer-capability gated, render-thread gated, and disabled after an allocation/composite failure.
- The framebuffer pipeline attempts to restore the main framebuffer and relevant OpenGL state after compositing.
- FeatherOpt is detected without a hard class-linkage dependency. Optional framebuffer effects remain disabled by default when FeatherOpt is present because runtime compatibility cannot be verified from source alone.
- No FeatherOpt source is vendored or modified.

## FeatherOpt investigation
Upstream FeatherOpt uses ForgeGradle 2.1 for Forge 1.8.9, SpongePowered Mixin, a `FeatherCoreMod` FML core plugin, `mixins.featheropt.json`, an access transformer, and Mixin launch configuration. ArgentumForge does not currently declare a coremod, Mixin config, or FeatherOpt dependency. No direct overlapping injection was identified from the available source information. Runtime OpenGL/framebuffer interaction with the installed FeatherOpt artifact remains unverified.

## Validation status
- Deterministic pure-Java helper tests: source added/updated; execution not verified.
- Gradle build: not run in this conflict-resolution session.
- Runtime Forge-only validation: not available here.
- FeatherOpt with OptiFine disabled: not verified.
- FeatherOpt with OptiFine enabled: not verified.
- Do not claim the white-screen issue is fixed until the affected Feather/Dawn configurations have been launched and compared with and without ArgentumForge.

## Remaining limitations
- Item optimization is partial, and block-entity optimization remains fallback-only.
- Weather rendering is an approximation of vanilla rain/snow visuals, not full parity.
- Post-processing is a framebuffer composite/tint path, not a shader-chain implementation.
- Exact translucent ordering, terrain visual parity, and performance require real-client testing.