# engine-physics — Phase 3

Actual JBox2D 2.2.1.1, 100 pixels/metre, 60 fixed steps/second, eight velocity and three position iterations. Provides static/dynamic/kinematic bodies; rectangle/circle/convex polygon/capsule/compound fixtures; materials, sensors, aggregated contact events, grounding, AABB queries, raycasts and seven joint kinds. GL debug data contains fixture boundaries and joint anchors.

`ColliderGeometry` constructs convex hulls and real Bitmap-alpha approximations. Alpha holes/concavities are filled; reduction is limited to eight vertices and may shrink the hull. Physics ancestors need uniform non-reflected scale. Nonuniform circles become octagons. Tiny fixtures are rejected before JBox2D can substitute fallback geometry.

Preview limits are 512 bodies and 4096 collider definitions. Scene validation caps each body at 32 collider definitions and scenes at 1000 joints. Compound capsules create multiple fixtures. These are safety ceilings, not verified mobile performance targets. Body centres must remain within ±1,000,000 pixels. See `docs/PHASE3.md`.
