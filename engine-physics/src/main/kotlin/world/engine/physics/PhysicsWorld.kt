package world.engine.physics

import org.jbox2d.callbacks.*
import org.jbox2d.collision.*
import org.jbox2d.collision.shapes.*
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.joints.*
import org.jbox2d.common.Vec2 as BoxVec
import world.engine.core.*
import world.engine.math.*
import kotlin.math.*

data class CollisionEvent(val a: String,val b: String,val sensor: Boolean,val entered: Boolean)
data class RayHit(val entity: String,val point: Vec2,val normal: Vec2,val fraction: Float)
/** Single-thread-owned JBox2D world. Engine pixels are converted at 100 pixels per metre. */
class PhysicsWorld(initial: Scene,private val onContact: (CollisionEvent)->Unit = {}) {
    companion object { const val PIXELS_PER_METRE=100f; const val STEP=1f/60f }
    private val world=World(box(initial.gravity))
    private val bodies=linkedMapOf<String,Body>()
    private val contactCounts=mutableMapOf<Triple<String,String,Boolean>,Int>()
    private val events=ArrayDeque<CollisionEvent>()
    private var grounded=emptySet<String>()
    val bodyCount get() = world.bodyCount
    val jointCount get() = world.jointCount
    init {
        initial.validated()
        require(initial.nodes.count { it.component<RigidBodyComponent>()!=null }<=512) { "Preview supports at most 512 rigid bodies" }
        require(initial.nodes.sumOf { it.component<RigidBodyComponent>()?.colliders?.size ?: 0 }<=4096) { "Too many colliders for preview" }
        val matrices=initial.worldMatrices()
        initial.nodes.forEach { n -> n.component<RigidBodyComponent>()?.let { config ->
            var parent=n.parent
            while(parent!=null) {
                val ancestor=initial.nodes.single { it.id==parent }; val m=matrices.getValue(ancestor.id).values
                val x=hypot(m[0],m[3]);val y=hypot(m[1],m[4]);val dot=m[0]*m[1]+m[3]*m[4]
                require(abs(x-y)<.001f*maxOf(1f,x) && abs(dot)<.001f*maxOf(1f,x*y) && m[0]*m[4]-m[1]*m[3]>0) { "Body ${n.name}: ancestors need uniform, non-reflected scale" }
                ancestor.component<AnimatorComponent>()?.let { a -> require(initial.clips.single { it.id==a.clipId }.tracks.none { it.property==TrackProperty.SCALE }) { "Cannot animate physics ancestor scale" } }
                parent=ancestor.parent
            }
            val matrix=matrices.getValue(n.id); val parentAngle=n.parent?.let { angle(matrices.getValue(it)) } ?: 0f
            val rotation=parentAngle+radians(n.transform.rotation)
            val definition=BodyDef().apply {
                type=when(config.kind) { BodyKind.STATIC->BodyType.STATIC;BodyKind.DYNAMIC->BodyType.DYNAMIC;BodyKind.KINEMATIC->BodyType.KINEMATIC }
                position.set(box(matrix.map(Vec2())));this.angle=rotation;gravityScale=config.gravityScale;fixedRotation=config.fixedRotation;bullet=config.bullet;linearDamping=config.linearDamping
                linearVelocity.set(box(config.velocity));angularVelocity=radians(config.angularVelocity)
            }
            val body=world.createBody(definition);body.userData=n.id;bodies[n.id]=body
            config.colliders.forEachIndexed { index,c ->
                fun point(p: Vec2): BoxVec = body.getLocalPoint(box(matrix.map(p)))
                fun polygon(vertices: List<Vec2>) { val shape=PolygonShape();shape.set(vertices.map(::point).toTypedArray(),vertices.size);fixture(body,shape,c,index) }
                fun circle(center: Vec2,radius: Float) {
                    val values=matrix.values;val sx=hypot(values[0],values[3]);val sy=hypot(values[1],values[4])
                    if(abs(sx-sy)<.0001f*maxOf(1f,sx)) {
                        val shape=CircleShape();shape.m_p.set(point(center));shape.m_radius=radius*sx/PIXELS_PER_METRE;fixture(body,shape,c,index)
                    } else polygon(List(8) { i -> val a=i*2*PI/8; center+Vec2(cos(a).toFloat()*radius,sin(a).toFloat()*radius) })
                }
                when(c.shape) {
                    ShapeKind.CIRCLE -> circle(c.offset,c.radius)
                    ShapeKind.CAPSULE -> {
                        val radius=c.size.x/2;val half=(c.size.y-c.size.x)/2
                        if(half>.001f) polygon(listOf(Vec2(-radius,-half),Vec2(radius,-half),Vec2(radius,half),Vec2(-radius,half)).map { it+c.offset })
                        circle(c.offset+Vec2(0f,half),radius);if(half>.001f)circle(c.offset+Vec2(0f,-half),radius)
                    }
                    else -> polygon(ColliderGeometry.polygons(c).single())
                }
            }
        } }
        initial.joints.forEach { createJoint(it,matrices) }
        world.setContactListener(object: ContactListener {
            override fun beginContact(contact: Contact) = queue(contact,true)
            override fun endContact(contact: Contact) = queue(contact,false)
            override fun preSolve(contact: Contact,oldManifold: Manifold) = Unit
            override fun postSolve(contact: Contact,impulse: ContactImpulse) = Unit
        })
    }
    private fun fixture(body: Body,shape: Shape,c: Collider,index: Int) {
        body.createFixture(FixtureDef().apply { this.shape=shape;density=c.density;friction=c.friction;restitution=c.restitution;isSensor=c.sensor;userData=index })
    }
    private fun queue(contact: Contact,begin: Boolean) {
        val ids=listOf(contact.fixtureA.body.userData as String,contact.fixtureB.body.userData as String).sorted()
        val key=Triple(ids[0],ids[1],contact.fixtureA.isSensor || contact.fixtureB.isSensor)
        val old=contactCounts[key] ?: 0;val count=(old+if(begin)1 else -1).coerceAtLeast(0)
        if(count==0)contactCounts.remove(key) else contactCounts[key]=count
        if((begin && old==0) || (!begin && old>0 && count==0))events.add(CollisionEvent(key.first,key.second,key.third,begin))
    }
    private fun createJoint(j: JointSpec,matrices: Map<String,Mat3>) {
        val a=bodies.getValue(j.bodyA);val b=bodies.getValue(j.bodyB)
        val pa=box(matrices.getValue(j.bodyA).map(j.anchorA));val pb=box(matrices.getValue(j.bodyB).map(j.anchorB))
        val axis=a.getWorldVector(BoxVec(j.axis.x,j.axis.y));axis.normalize()
        val definition: JointDef=when(j.kind) {
            JointKind.FIXED -> WeldJointDef().apply { initialize(a,b,pa);localAnchorB.set(b.getLocalPoint(pb)) }
            JointKind.DISTANCE,JointKind.SPRING -> DistanceJointDef().apply { initialize(a,b,pa,pb);length=j.length/PIXELS_PER_METRE;frequencyHz=if(j.kind==JointKind.SPRING)j.frequency else 0f;dampingRatio=j.damping }
            JointKind.REVOLUTE -> RevoluteJointDef().apply { initialize(a,b,pa);localAnchorB.set(b.getLocalPoint(pb));enableMotor=j.motor;motorSpeed=radians(j.motorSpeed);maxMotorTorque=j.maxMotorForce;enableLimit=j.limit;lowerAngle=radians(j.lower);upperAngle=radians(j.upper) }
            JointKind.PRISMATIC -> PrismaticJointDef().apply { initialize(a,b,pa,axis);localAnchorB.set(b.getLocalPoint(pb));enableMotor=j.motor;motorSpeed=j.motorSpeed/PIXELS_PER_METRE;maxMotorForce=j.maxMotorForce;enableLimit=j.limit;lowerTranslation=j.lower/PIXELS_PER_METRE;upperTranslation=j.upper/PIXELS_PER_METRE }
            JointKind.WHEEL -> WheelJointDef().apply { initialize(a,b,pa,axis);localAnchorB.set(b.getLocalPoint(pb));enableMotor=j.motor;motorSpeed=radians(j.motorSpeed);maxMotorTorque=j.maxMotorForce;frequencyHz=j.frequency;dampingRatio=j.damping }
            JointKind.ROPE -> RopeJointDef().apply { bodyA=a;bodyB=b;localAnchorA.set(a.getLocalPoint(pa));localAnchorB.set(b.getLocalPoint(pb));maxLength=j.length/PIXELS_PER_METRE }
        }
        definition.collideConnected=j.collideConnected;world.createJoint(definition)
    }
    fun velocity(id: String): Vec2 = bodies[id]?.linearVelocity?.let(::pixels) ?: Vec2()
    fun setVelocity(id: String,velocity: Vec2) { bodies[id]?.let { it.linearVelocity=box(velocity);it.isAwake=true } }
    fun impulse(id: String,impulse: Vec2) { bodies[id]?.let { it.applyLinearImpulse(box(impulse),it.worldCenter) } }
    fun isGrounded(id: String) = id in grounded
    fun step(scene: Scene): Scene {
        val matrices=scene.worldMatrices()
        scene.nodes.forEach { n -> bodies[n.id]?.let { body ->
            if(body.type!=BodyType.DYNAMIC) {
                val position=box(matrices.getValue(n.id).map(Vec2()));val rotation=radians(n.transform.rotation)+(n.parent?.let { angle(matrices.getValue(it)) } ?: 0f)
                val animated=generateSequence(n) { current -> current.parent?.let { id -> scene.nodes.find { it.id==id } } }.any { it.component<AnimatorComponent>()!=null }
                if(body.type==BodyType.STATIC)body.setTransform(position,rotation)
                else if(animated) { body.linearVelocity=position.sub(body.position).mul(1f/STEP);body.angularVelocity=(rotation-body.angle)/STEP }
            }
        } }
        world.step(STEP,8,3)
        val supported=mutableSetOf<String>();var contact=world.contactList
        while(contact!=null) {
            if(contact.isTouching && contact.isEnabled && !contact.fixtureA.isSensor && !contact.fixtureB.isSensor) {
                val manifold=WorldManifold();contact.getWorldManifold(manifold)
                if(manifold.normal.y<-.4f)supported.add(contact.fixtureA.body.userData as String)
                if(manifold.normal.y>.4f)supported.add(contact.fixtureB.body.userData as String)
            }
            contact=contact.next
        }
        grounded=supported
        while(events.isNotEmpty())onContact(events.removeFirst())
        val changed=scene.nodes.associateBy { it.id }.toMutableMap();val worlds=mutableMapOf<String,Mat3>()
        fun update(id: String): Mat3 = worlds.getOrPut(id) {
            var node=changed.getValue(id);val parent=node.parent?.let(::update) ?: Mat3.Identity
            bodies[id]?.let { body ->
                node=node.withComponent(TransformComponent(node.transform.copy(position=parent.inverse().map(pixels(body.position)),rotation=degrees(body.angle-angle(parent)))))
                changed[id]=node
            }
            parent*node.transform.matrix()
        }
        scene.nodes.forEach { update(it.id) }
        return scene.copy(nodes=scene.nodes.map { changed.getValue(it.id) })
    }
    fun debugLines(): List<DebugLine> = bodies.values.flatMap { body ->
        val lines=mutableListOf<DebugLine>();var fixture=body.fixtureList
        while(fixture!=null) {
            val color=if(fixture.isSensor)Color(.2f,.8f,1f) else if(body.type==BodyType.STATIC)Color(.8f,.8f,.8f) else Color(.3f,1f,.4f)
            val vertices=when(val shape=fixture.shape) {
                is PolygonShape -> (0 until shape.m_count).map { pixels(body.getWorldPoint(shape.m_vertices[it])) }
                is CircleShape -> List(24) { i -> val a=i*2*PI/24;pixels(body.getWorldPoint(shape.m_p.add(BoxVec(cos(a).toFloat()*shape.m_radius,sin(a).toFloat()*shape.m_radius)))) }
                else -> emptyList()
            }
            vertices.indices.forEach { i -> lines.add(DebugLine(vertices[i],vertices[(i+1)%vertices.size],color)) };fixture=fixture.next
        };lines
    } + buildList {
        var joint=world.jointList
        while(joint!=null) { val a=BoxVec();val b=BoxVec();joint.getAnchorA(a);joint.getAnchorB(b);add(DebugLine(pixels(a),pixels(b),Color(1f,.8f,.2f)));joint=joint.next }
    }
    fun query(rect: Rect): Set<String> {
        val hits=mutableSetOf<String>();val half=rect.size*.5f
        world.queryAABB(QueryCallback { fixture -> hits.add(fixture.body.userData as String);true },AABB(box(rect.center-half),box(rect.center+half)))
        return hits
    }
    fun raycast(from: Vec2,to: Vec2,includeSensors: Boolean=false): RayHit? {
        require((to-from).length()>.001f) { "Ray must have positive length" }
        var result: RayHit?=null
        world.raycast(RayCastCallback { fixture,point,normal,fraction ->
            if(fixture.isSensor && !includeSensors)-1f else { result=RayHit(fixture.body.userData as String,pixels(point),Vec2(normal.x,normal.y),fraction);fraction }
        },box(from),box(to))
        return result
    }
    private fun box(v: Vec2)=BoxVec(v.x/PIXELS_PER_METRE,v.y/PIXELS_PER_METRE)
    private fun pixels(v: BoxVec)=Vec2(v.x*PIXELS_PER_METRE,v.y*PIXELS_PER_METRE)
    private fun angle(m: Mat3)=atan2(m.values[3],m.values[0])
    private fun radians(degrees: Float)=degrees*(PI.toFloat()/180f)
    private fun degrees(radians: Float)=radians*(180f/PI.toFloat())
}
