# ArgentumForge Technical Notes

## Target and entry points

- Minecraft 1.8.9, Forge `1.8.9-11.15.1.2318-1.8.9`, MCP `stable_22`, Java 8, LWJGL 2.
- `ArgentumForge` loads `ArgentumConfig` and delegates to the sided proxy.
- `ClientProxy` registers itself, `ClientWorldHooks` and (through `ArgentumRenderer`) `OptionalRenderHooks` on `MinecraftForge.EVENT_BUS`. It drives:
  - `ClientTickEvent` END: optional hooks, the legacy overlay tick (overlay mode only), `IntegratedTerrain.onClientTick`, `BlockEntityDistanceCulling.onClientTick`
  - `RenderTickEvent` START: `IntegratedTerrain.onRenderFrameStart` (snapshot budget reset)
  - `RenderGameOverlayEvent.Text`: F3 counters
- No Mixin JSON, coremod manifest entry, access transformer or `IClassTransformer` exists.

## Integrated terrain path (`terrainMode=integrated`)

### Installation

`IntegratedTerrain.ensureInstalled` resolves `RenderGlobal.renderContainer` (`field_174996_N`) and `RenderGlobal.renderChunkFactory` (`field_175007_a`) with `ReflectionHelper.findField`, using MCP and SRG names from the `stable_22` CSVs and the MCP 1.8.9 SRG. It then sets:

- for `OpenGlHelper.useVbo()`: `ArgentumVboRenderList` + `ArgentumRenderChunkFactory(true)`, which creates `ArgentumRenderChunk extends RenderChunk`
- otherwise: `ArgentumDisplayListRenderList` + `ArgentumRenderChunkFactory(false)`, which creates `ArgentumListedRenderChunk extends ListedRenderChunk`

Finally it calls `RenderGlobal.loadRenderers()`, which builds a new `ViewFrustum` with the factory.

1.8.9 `loadRenderers` only reassigns those two fields when `vboEnabled` flips. The client-tick check then reinstalls, with at most 4 reinstalls per 200 ticks before giving up. Disabling, OptiFine detection, the FeatherOpt terrain gate or any thrown error restores `VboRenderList`/`VboChunkFactory` or `RenderList`/`ListChunkFactory` and reloads renderers.

### Compile lifecycle

1. Client thread: `RenderGlobal.setupTerrain` calls `ChunkRenderDispatcher.updateChunkLater`/`updateChunkNow`, which calls `makeCompileTaskChunk()`. The override calls super, which cancels the previous task, and then `SnapshotCompileSupport.onCompileTaskCreated`. If `IntegratedTerrain.tryReserveSnapshot()` passes (client thread, per-frame budget, pending cap), `SectionSnapshot.capture` copies:
   - the block states and `Chunk.getLightFor` sky/block values for a 20^3 region (section ± 2)
   - `World.getBiomeGenForCoords` for its 20^2 columns
   - tile entities for in-section blocks with `hasTileEntity(state)`, via `Chunk.getTileEntity(pos, QUEUED)`, the same mode as `RegionRenderCache`
   - valid tile entities in the padding ring
   - the `ChunkCache` "levels empty" flag
   - the inclusive bounds of blocks with `getRenderType() != -1`

   The snapshot is stored in an `AtomicReference` together with the task object.
2. Worker thread: `ChunkRenderWorker` calls `rebuildChunk(x, y, z, task)`. The override records the task in a thread-local and calls vanilla/Forge `rebuildChunk`. That calls Forge's `createRegionRenderCache(world, from, to, 1)`. The override CAS-takes the pending snapshot only if it belongs to the same task and `from` equals `origin - 1`, and returns `SnapshotRegionRenderCache`. Otherwise it returns vanilla's cache.
3. `SnapshotRegionRenderCache` answers in-region requests from the snapshot:
   - `getBlockState`, `getTileEntity`, `getLightFor`, `getBiomeGenForCoords`
   - `getCombinedLight`, a port of `ChunkCache.getLightForExt` including `getUseNeighborBrightness` and no-sky handling
   - `extendedLevelsInChunkCache`

   `isAirBlock`, `getStrongPower` and Forge `isSideSolid` reach the snapshot through the overridden `getBlockState`.
4. Vanilla code does the rest: meshing, `canRenderInLayer` layering, tile-entity bookkeeping, `VisGraph`, translucent `sortVertexData`, `ChunkRenderWorker` status checks (stale results are dropped), and client-thread upload. After a snapshot build the compiled chunk's geometry bounds go into a synchronized `WeakHashMap<CompiledChunk, GeometryBounds>`.
5. A pending snapshot is released when the next task replaces it, on `stopCompileTask` (position change), on `deleteGlResources`, or when `rebuildChunk` exits without using it.

### Drawing and visibility

`RenderGlobal.renderBlockLayer` still filters empty layers and calls `renderContainer.addRenderChunk`/`renderChunkLayer`. The Argentum containers inherit vanilla drawing and override:

- `initialize`: captures planes
- `addRenderChunk`: drops a layer only when the compiled chunk's recorded bounds (expanded by 1 block) are behind one of the four side planes

The planes are computed from `GL_PROJECTION_MATRIX`/`GL_MODELVIEW_MATRIX` the same way `ClippingHelperImpl.init` does. Near and far planes are ignored, because the sky pass changes only the far distance between frustum creation and `setupTerrain`. Missing bounds, an invalid plane or a non-Argentum chunk all mean the layer is drawn.

### Threading and GL

- Worker threads never call GL or mutate world state.
- Snapshot capture, uploads, `loadRenderers` and GL resource deletion run on the client thread.
- Snapshot arrays are final and safely published through the `AtomicReference`.

### Memory

About 50 KB per snapshot. Pending snapshots are limited by `terrainMaxPendingSnapshots` (default 256) and by one per `RenderChunk`.

## Legacy overlay path (`terrainMode=overlay`)

`ChunkMeshManager.onTick` builds meshes synchronously from the live `World` with `BlockFaceMesher` (now on `FloatArrayList` buffers), uploads them to `GlVbo`, and `ArgentumRenderer.onRenderWorldLast` draws them after vanilla terrain. It can duplicate terrain. Lighting, AO, fluids and translucency are approximations. The optional framebuffer effect only wraps this pass.

## Other hooks

- **Block entities:** `BlockEntityDistanceCulling` replaces the exact vanilla renderer instances for chests, ender chests, signs, skulls and banners in `TileEntityRendererDispatcher.mapSpecialRenderers` (`field_147559_m`) with `DistanceCulledTileEntityRenderer`.
  - The wrapper delegates `renderTileEntityAt`, Forge `renderTileEntityFast`, `setRendererDispatcher` and `forceTileEntityRender`.
  - It skips only when the block entity's exact class is in the set, it is in the client world, `destroyStage < 0`, and its distance to `TileEntityRendererDispatcher.staticPlayerX/Y/Z` exceeds the limit.
  - Entries cached for subclasses by `getSpecialRendererByClass` still pass through, because of the exact-class check.
- **Living entities:** `RenderLivingEvent.Pre` distance culling, limited to vanilla entity and renderer packages. Unnamed, unmounted and unleashed only. The package checks are cached in `IdentityHashMap`s.
- **Items:** `EntityItem.renderDistanceWeight` adjustment, restored on world change or disable.
- **Particles:** the particle-setting floor is restored when disabled.
- **Weather:** `WorldProvider.setWeatherRenderer` is used only when the provider has none. `WeatherColumnCache` keeps type and precipitation height per column for one world tick and camera window. The world is held weakly.
- **Clouds:** flat grid from `RenderWorldLastEvent`, only when vanilla clouds are off and there is no sky renderer.

## Compatibility

- **FeatherOpt:** static mixin-target review in `PORTING_STATUS.md`. No conflicting registered target was found for the replaced objects.
- **OptiFine:** integrated terrain is not installed when `optifine.OptiFineForgeTweaker` or `optifine.Config` is present.

## Validation record

No Gradle or wrapper command, Java compilation, unit or integration test, Minecraft launch, or FeatherOpt/OptiFine runtime check was performed. API names and signatures were checked against MCP 1.8.9 sources, the Forge 1.8.9 patches (including the 2318 userdev `patches.zip`), and the `stable_22`/SRG mapping files. Source inspection is not evidence that the code compiles, renders correctly or improves performance.
