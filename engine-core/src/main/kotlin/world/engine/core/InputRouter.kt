package world.engine.core

/** Source values are combined per action; snapshots consume edges, not held state. Thread-safe. */
data class InputSnapshot(val values: Map<String,Float>,val pressed: Set<String>,val released: Set<String>) {
    fun value(action: String) = values[action] ?: 0f
}
class InputRouter(private val mapping: InputMap) {
    private val physical=mutableMapOf<Triple<InputSource,Int,Int>,Float>()
    private val touch=mutableMapOf<String,Float>()
    private var previous=emptySet<String>()
    @Synchronized fun physical(source: InputSource,code: Int,value: Float,device: Int=0) {
        require(value.isFinite()); val key=Triple(source,code,device)
        if(value==0f)physical.remove(key) else physical[key]=value.coerceIn(-1f,1f)
    }
    @Synchronized fun touch(action: String,value: Float) { require(value.isFinite()); touch[action]=value.coerceIn(-1f,1f) }
    @Synchronized fun releaseAll() { physical.clear(); touch.clear() }
    @Synchronized fun snapshot(): InputSnapshot {
        val values=mutableMapOf<String,Float>()
        mapping.bindings.forEach { b ->
            val raw=if(b.source==InputSource.TOUCH)touch[b.action] ?: 0f else physical.filterKeys { it.first==b.source && it.second==b.code }.values.sum()
            val filtered=if(b.source==InputSource.GAMEPAD_AXIS) { val a=kotlin.math.abs(raw); if(a<=b.deadZone)0f else kotlin.math.sign(raw)*(a-b.deadZone)/(1f-b.deadZone) } else raw
            values[b.action]=((values[b.action] ?: 0f)+filtered*b.scale)
        }
        values.replaceAll { _,v -> v.coerceIn(-1f,1f) }
        val active=values.filterValues { kotlin.math.abs(it)>.5f }.keys.toSet()
        val result=InputSnapshot(values.toMap(),active-previous,previous-active); previous=active; return result
    }
}
