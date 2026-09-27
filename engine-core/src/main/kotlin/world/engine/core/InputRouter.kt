package world.engine.core

/** Source values are combined per action; snapshots consume buffered edges, not held state. */
data class InputSnapshot(val values: Map<String,Float>,val pressed: Set<String>,val released: Set<String>) {
    fun value(action: String) = values[action] ?: 0f
}
class InputRouter(private val mapping: InputMap) {
    private val physical=mutableMapOf<Triple<InputSource,Int,Int>,Float>()
    private val touch=mutableMapOf<String,Float>()
    private var active=emptySet<String>()
    private val pressed=mutableSetOf<String>()
    private val released=mutableSetOf<String>()
    private var values=emptyMap<String,Float>()
    @Synchronized fun physical(source: InputSource,code: Int,value: Float,device: Int=0) {
        if(!value.isFinite())return
        val key=Triple(source,code,device)
        if(value==0f)physical.remove(key) else physical[key]=value.coerceIn(-1f,1f)
        resolve()
    }
    @Synchronized fun touch(action: String,value: Float) {
        if(!value.isFinite() || mapping.bindings.none { it.source==InputSource.TOUCH && it.action==action })return
        if(value==0f)touch.remove(action) else touch[action]=value.coerceIn(-1f,1f)
        resolve()
    }
    /** Focus/pause cancellation releases held sources and cannot inject a stale press on resume. */
    @Synchronized fun releaseAll() { physical.clear();touch.clear();pressed.clear();resolve() }
    /** Device disconnect cancels only that device, preserving other held controls. */
    @Synchronized fun releaseDevice(device: Int) {
        val removed=physical.keys.filter { it.third==device }.toSet()
        val affected=mapping.bindings.filter { b -> removed.any { it.first==b.source && it.second==b.code } }.map { it.action }.toSet()
        physical.keys.removeAll(removed);resolve();pressed.removeAll(affected-active)
    }
    private fun resolve() {
        val next=mutableMapOf<String,Float>()
        mapping.bindings.forEach { b ->
            val raw=if(b.source==InputSource.TOUCH)touch[b.action] ?: 0f else physical.filterKeys { it.first==b.source && it.second==b.code }.values.sum()
            val filtered=if(b.source==InputSource.GAMEPAD_AXIS) { val a=kotlin.math.abs(raw);if(a<=b.deadZone)0f else kotlin.math.sign(raw)*(a-b.deadZone)/(1f-b.deadZone) } else raw
            next[b.action]=(next[b.action] ?: 0f)+filtered*b.scale
        }
        next.replaceAll { _,v -> v.coerceIn(-1f,1f) }
        val held=next.filterValues { kotlin.math.abs(it)>.5f }.keys.toSet()
        pressed.addAll(held-active);released.addAll(active-held);active=held;values=next.toMap()
    }
    @Synchronized fun snapshot(): InputSnapshot {
        val result=InputSnapshot(values,pressed.toSet(),released.toSet());pressed.clear();released.clear();return result
    }
}
