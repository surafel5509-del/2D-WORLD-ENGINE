package world.engine.animation

import world.engine.core.*
import java.util.UUID

/** 65 editable transform/color clips. These do not pretend to be hand-drawn sprite-frame artwork. */
object AnimationPresets {
    private val actions=listOf("Idle","Walk","Run","Jump","Attack","Hurt","Death","Roll","Dash","Shoot","Reload","Climb","Swim")
    fun all(): List<AnimationClip> = listOf("Humanoid","Robot","Animal","Vehicle","Creature").flatMapIndexed { archetype,type ->
        actions.map { action ->
            val amount=1f+archetype*.15f
            fun track(p: TrackProperty,vararg values: List<Float>) = AnimationTrack(p,values.mapIndexed { i,v -> AnimationKey(i*11/(values.size-1),v) })
            val tracks=when(action) {
                "Idle" -> listOf(track(TrackProperty.SCALE,listOf(1f,1f),listOf(1f,1.025f*amount.coerceAtMost(1.1f)),listOf(1f,1f)))
                "Walk","Run" -> listOf(track(TrackProperty.ROTATION,listOf(-4f*amount),listOf(4f*amount),listOf(-4f*amount)),track(TrackProperty.POSITION,listOf(0f,0f),listOf(0f,if(action=="Run")8f else 3f),listOf(0f,0f)))
                "Jump" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(0f,36f*amount),listOf(0f,0f)))
                "Attack" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(-25f*amount),listOf(30f*amount),listOf(0f)))
                "Hurt" -> listOf(track(TrackProperty.COLOR,listOf(1f,1f,1f,1f),listOf(1f,.15f,.15f,1f),listOf(1f,1f,1f,1f)))
                "Death" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(90f)),track(TrackProperty.COLOR,listOf(1f,1f,1f,1f),listOf(1f,1f,1f,0f)))
                "Roll" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(360f)))
                "Dash" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(80f*amount,0f)))
                "Shoot" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(-6f*amount,0f),listOf(0f,0f)))
                "Reload" -> listOf(track(TrackProperty.ROTATION,listOf(0f),listOf(20f),listOf(0f)))
                "Climb" -> listOf(track(TrackProperty.POSITION,listOf(0f,0f),listOf(0f,16f*amount)))
                else -> listOf(track(TrackProperty.ROTATION,listOf(-10f),listOf(10f),listOf(-10f)))
            }
            AnimationClip(id=UUID.nameUUIDFromBytes("world.preset.$type.$action.v1".toByteArray()).toString(),name="$type $action",fps=if(action=="Run")24 else 12,frames=12,loop=if(action in listOf("Idle","Walk","Run","Swim","Climb"))LoopMode.LOOP else LoopMode.ONCE,tracks=tracks,
                events=if(action in listOf("Attack","Shoot","Jump"))listOf(AnimationEvent(5,action.lowercase())) else emptyList()).validated()
        }
    }
}
