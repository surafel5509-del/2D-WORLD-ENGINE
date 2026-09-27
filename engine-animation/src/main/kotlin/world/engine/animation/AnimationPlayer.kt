package world.engine.animation

import world.engine.core.*
import world.engine.math.*
import kotlin.math.floor

/** Deterministic frame clock. Crossing a boundary dispatches that frame's events exactly once. */
class AnimationPlayer(val clip: AnimationClip, private val speed: Float=1f) {
    private var ticks=0.0
    private var started=false
    /** Last dispatched sprite event. It changes only at real event boundaries, including reverse playback. */
    var eventSprite: String?=null
        private set
    init { clip.validated(); require(speed in .01f..10f) }
    val finished get() = clip.loop==LoopMode.ONCE && ticks>=clip.frames
    val frame: Float get() = frameAt(ticks).toFloat()
    /** Seeks in forward frame space without emitting events. */
    fun seek(frame: Float) { require(frame.isFinite()); ticks=frame.coerceIn(0f,(clip.frames-1).toFloat()).toDouble();eventSprite=latestSpriteAt(clip,ticks.toFloat());started=true }
    /** Advances bounded elapsed time, delivering frame-zero and every crossed frame in order. */
    fun advance(seconds: Float, emit: (AnimationEvent)->Unit = {}) {
        require(seconds.isFinite() && seconds in 0f..10f)
        if(!started) { emitAt(0,emit); started=true }
        if(finished)return
        val end=if(clip.loop==LoopMode.ONCE) minOf(ticks+seconds*clip.fps*speed,clip.frames.toDouble()) else ticks+seconds*clip.fps*speed
        val first=floor(ticks).toLong()+1; val last=floor(end+1e-9).toLong()
        require(last-first<20000) { "Animation event catch-up limit exceeded" }
        for(step in first..last) {
            if(clip.loop==LoopMode.ONCE && step>=clip.frames)break
            emitAt(frameAt(step.toDouble()).toInt(),emit)
        }
        ticks=end
        // Keep long-running loops numerically stable without losing a boundary.
        if(ticks>1_000_000 && clip.loop!=LoopMode.ONCE)ticks%=period()
    }
    private fun emitAt(frame: Int,emit: (AnimationEvent)->Unit) { clip.events.filter { it.frame==frame }.forEach { if(it.kind==AnimationEventKind.SET_SPRITE)eventSprite=it.assetId;emit(it) } }
    private fun period() = if(clip.loop==LoopMode.PING_PONG) maxOf(1,2*(clip.frames-1)).toDouble() else clip.frames.toDouble()
    private fun frameAt(t: Double): Double = when(clip.loop) {
        LoopMode.ONCE -> t.coerceIn(0.0,(clip.frames-1).toDouble())
        LoopMode.LOOP -> (t%period()).coerceAtMost((clip.frames-1).toDouble())
        LoopMode.PING_PONG -> { val p=t%period(); if(p<=clip.frames-1)p else period()-p }
    }
}

object AnimationSampler {
    /** A pure sampler used both by timeline scrubbing and the runtime player. */
    fun sample(base: Node,clip: AnimationClip,frame: Float,current: Node=base,eventSprite: String?=latestSpriteAt(clip,frame)): Node {
        require(frame.isFinite())
        var transform=current.transform; var sprite=current.sprite
        val f=frame.coerceIn(0f,(clip.frames-1).toFloat())
        clip.tracks.forEach { track ->
            val keys=track.keys.sortedBy { it.frame }
            val a=keys.lastOrNull { it.frame<=f } ?: keys.first()
            val b=keys.firstOrNull { it.frame>f } ?: a
            val t=if(track.interpolation==Interpolation.STEP || a.frame==b.frame)0f else ((f-a.frame)/(b.frame-a.frame)).coerceIn(0f,1f)
            val values=a.values.indices.map { i -> a.values[i]+(b.values[i]-a.values[i])*t }
            when(track.property) {
                TrackProperty.POSITION -> { val v=Vec2(values[0],values[1]); transform=transform.copy(position=if(clip.relative)base.transform.position+v else v) }
                TrackProperty.ROTATION -> transform=transform.copy(rotation=values[0]+if(clip.relative)base.transform.rotation else 0f)
                TrackProperty.SCALE -> { val v=Vec2(values[0],values[1]); transform=transform.copy(scale=if(clip.relative)Vec2(base.transform.scale.x*v.x,base.transform.scale.y*v.y) else v) }
                TrackProperty.COLOR -> sprite=sprite?.copy(tint=Color(values[0],values[1],values[2],values[3]))
                TrackProperty.SPRITE -> sprite=sprite?.copy(asset=null,assetId=a.assetId)
            }
        }
        eventSprite?.let { sprite=sprite?.copy(asset=null,assetId=it) }
        return current.copy(components=current.components.map { when(it) { is TransformComponent -> TransformComponent(transform); is SpriteComponent -> sprite ?: it; else -> it } })
    }
}

private fun latestSpriteAt(clip: AnimationClip,frame: Float): String? = clip.events.withIndex().filter { it.value.kind==AnimationEventKind.SET_SPRITE && it.value.frame<=frame }.maxWithOrNull(compareBy<IndexedValue<AnimationEvent>> { it.value.frame }.thenBy { it.index })?.value?.assetId
