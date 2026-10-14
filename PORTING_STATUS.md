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
- Particle throttling: Partial, opt-in, with restoration behavior
- Cloud renderer: Partial, opt-in, disabled when vanilla clouds or custom sky renderers are active
- Item rendering optimization: Fallback-only
- Block-entity optimization: Fallback-only
- Weather optimization: Partial, opt-in, conservative distance/frustum filtering with vanilla/custom-renderer fallback
- Optional post-processing: Partial framebuffer composite/tint path, capability/thread gated and self-disabling on failure
- Configuration/settings: Implemented, disabled by default
- Deterministic pure-Java helper tests: Implemented

## PR #9 conflict-resolution status
The weather and post-processing implementation is reconciled with the existing fallback-first renderer:

- Weather replacement is installed only when the experimental renderer and `enableWeatherHooks` are enabled.
- Existing custom/provider weather renderers are never replaced.
- Disabling the feature, changing worlds, or encountering a runtime error restores the vanilla/provider path.
- Weather filtering is render-only and does not change precipitation or gameplay state.
- Post-processing remains optional, framebuffer-capability gated, render-thread gated, and disabled after an allocation/composite failure.
- Framebuffer, depth, alpha, color, and depth-mask state are restored after the composite attempt.

## Build and runtime validation
- Deterministic helper tests: added for percentage clamping, weather distance/frustum decisions, and optional-effect gates.
- `gradle clean build --stacktrace --no-daemon`: must be run in a Java 8 environment with ForgeGradle 2.1 dependencies available.
- Runtime Forge-only validation: not available in this repository-edit environment.
- FeatherOpt with OptiFine disabled: not available in this repository-edit environment.
- FeatherOpt with OptiFine enabled: not available in this repository-edit environment.

The white-screen issue must not be described as confirmed fixed until the affected Feather/Dawn client configuration is launched and compared with and without ArgentumForge.

## Remaining limitations
- Item and block-entity optimization remain fallback-only to avoid unsafe generic Forge 1.8.9 interception.
- Weather rendering is an approximation of vanilla rain/snow visuals, not full parity.
- Post-processing is a framebuffer composite/tint path, not a shader-chain implementation.
- Exact translucent ordering, terrain visual parity, and performance require real-client testing.
