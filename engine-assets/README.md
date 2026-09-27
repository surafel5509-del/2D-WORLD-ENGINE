# engine-assets — Phase 2 implementation

SQLite UUID catalog, immutable PNG/prefab revisions, dependency protection, phase-1 PNG indexing, bitmap operations, named sprite-sheet layouts, PNG/ZIP export, modular splitting, and a 335-recipe original procedural generator.

All repository operations dispatch to IO and serialize through a per-project mutex. A payload is written and fsynced before a transaction publishes its path. Old payload revisions remain on disk; deletion removes only unreferenced catalog records and leaves tombstones for legacy images. Full asset-operation undo and garbage collection are not implemented.

See `docs/PHASE2.md` for exact scope, tests and limitations. Standalone compilation is not Android runtime verification.
