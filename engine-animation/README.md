# engine-animation — Phase 3

`AnimationPlayer` advances deterministic frame time with ONCE/LOOP/PING_PONG playback and crossed-frame event dispatch. `AnimationSampler` applies position, rotation, scale, RGBA and asset-UUID tracks without modifying authored nodes. `AnimationPresets` provides 65 deterministic transform/color clips: 13 actions × five archetypes. These are not new sprite-sheet artwork.

The runtime exposes real TRIGGER and SET_SPRITE events. Sound/particle/script consumers are intentionally not exposed before their later-phase modules exist. See `docs/PHASE3.md` for runtime integration and `docs/PHASE3_VERIFICATION.md` for executed versus pending tests.
