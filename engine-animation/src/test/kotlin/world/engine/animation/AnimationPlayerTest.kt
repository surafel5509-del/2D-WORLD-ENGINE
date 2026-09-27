package world.engine.animation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import world.engine.core.*
import world.engine.math.*

class AnimationPlayerTest {
    @Test fun pingPongBoundariesAndSeekDoNotDuplicateEvents() {
        val player=AnimationPlayer(AnimationClip(frames=3,fps=1,loop=LoopMode.PING_PONG,events=(0..2).map { AnimationEvent(it,"f$it") }))
        val events=mutableListOf<Int>();repeat(5){player.advance(1f){events.add(it.frame)}}
        assertEquals(listOf(0,1,2,1,0,1),events)
        player.seek(1f);val next=mutableListOf<Int>();player.advance(1f){next.add(it.frame)};assertEquals(listOf(2),next)
    }
    @Test fun multipleLoopEventsAreNotSkippedByLongFrames() {
        val clip=AnimationClip(frames=3,fps=10,events=(0..2).map { AnimationEvent(it,"f$it") });val player=AnimationPlayer(clip);val events=mutableListOf<Int>()
        player.advance(.65f){events.add(it.frame)};assertEquals(listOf(0,1,2,0,1,2,0),events)
    }
    @Test fun relativePositionAndStepInterpolation() {
        val node=Node().moved(Vec2(100f,50f))
        val track=AnimationTrack(TrackProperty.POSITION,listOf(AnimationKey(0,listOf(0f,0f)),AnimationKey(10,listOf(20f,40f))))
        val clip=AnimationClip(frames=11,tracks=listOf(track))
        assertEquals(Vec2(110f,70f),AnimationSampler.sample(node,clip,5f).transform.position)
        assertEquals(node.transform.position,AnimationSampler.sample(node,clip.copy(tracks=listOf(track.copy(interpolation=Interpolation.STEP))),5f).transform.position)
    }
    @Test fun libraryContains65StableValidatedClips() { val library=AnimationPresets.all();assertEquals(65,library.size);assertEquals(65,library.map { it.id }.distinct().size);assertEquals(library,AnimationPresets.all());library.forEach { it.validated() } }
}
