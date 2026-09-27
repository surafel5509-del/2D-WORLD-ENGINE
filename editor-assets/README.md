# editor-assets — Phase 2 implementation

Compose asset grid/list with search, tags/favorites/type filters, Coil file previews, local drag-and-drop payloads, expandable browsing, asset metadata/actions, non-destructive texture tools, and manual/grid/alpha sprite-sheet region editing.

The `AssetActions` interface connects UI actions to the editor view model without introducing a dependency back to editor-ui. Sheet layouts are explicitly saved. Long-press dragging works from the embedded browser into the viewport; the expanded browser uses the explicit Use action.

No Phase 3 controls are exposed. Full app/Compose compilation and physical-device gesture verification remain pending.
