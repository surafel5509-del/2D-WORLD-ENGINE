# editor-animation — Phase 3

Compose frame timeline with clip naming/FPS/length, looping and ping-pong, exact-frame seek and GL viewport scrubbing, actor assignment, editable transform/color/sprite keys, linear/step interpolation, frame events, and a 65-clip preset library.

The editor emits `AnimationActions` to the editor ViewModel. Authoring is undoable; scrubbing is transient and not persisted. The timeline is not a video exporter or skeletal animation system. Full source and verification results are in `docs/PHASE3.md` and its linked appendices.
