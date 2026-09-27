package world.engine.assets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import world.engine.core.*
import world.engine.math.Transform
import world.engine.math.Vec2
import kotlin.math.*

/** Original geometric pixel-art layers. Runtime output is dedicated to CC0; no external art. */
data class ProceduralPart(val name: String,val bitmap: Bitmap,val x: Int,val y: Int) {
    fun node(assetId: String,parent: String,width: Int,height: Int) = Node(name=name,parent=parent,components=listOf(
        TransformComponent(Transform(Vec2(x+bitmap.width/2f-width/2f,height/2f-y-bitmap.height/2f))),
        SpriteComponent(size=Vec2(bitmap.width.toFloat(),bitmap.height.toFloat()),assetId=assetId)))
}
object ProceduralAssets {
    val categories=linkedMapOf("Character" to 30,"Enemy" to 30,"NPC" to 20,"Animal" to 20,"Vehicle" to 25,"Building" to 40,"Nature" to 30,"Prop" to 40,"Texture" to 100)
    fun recipes() = categories.flatMap { (category,count) -> (1..count).map { "$category:$it" } }
    fun layers(recipe: String): List<ProceduralPart>? {
        val category=recipe.substringBefore(':'); val index=recipe.substringAfter(':').toInt()
        require(index in 1..(categories[category] ?: 0)) { "Unknown procedural recipe" }
        if(category=="Texture")return null
        val parts=mutableListOf<ProceduralPart>()
        val main=Color.HSVToColor(floatArrayOf(((index*37+(category.hashCode() and 255))%360).toFloat(),.55f,.8f))
        val dark=Color.rgb(25,32,45); val light=Color.rgb(190,224,238)
        fun part(name: String,x: Int,y: Int,w: Int,h: Int,color: Int,oval: Boolean=false) {
            val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap); val paint=Paint().apply { this.color=color; isAntiAlias=false }
            if(oval)canvas.drawOval(0f,0f,w.toFloat(),h.toFloat(),paint) else canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint)
            parts.add(ProceduralPart(name,bitmap,x,y))
        }
        when(category) {
            "Vehicle" -> when(index%4) {
                0 -> {
                    part("Rear wheel",25,44,14,19,dark,true); part("Front wheel",25,1,14,19,dark,true)
                    part("Frame",28,16,8,34,main); part("Fuel tank",22,20,20,17,main,true)
                    part("Seat",25,36,14,13,dark); part("Handlebars",13,15,38,4,light)
                    part("Headlight",28,10,8,7,Color.YELLOW); part("Footrest",17,40,30,3,light)
                }
                2 -> {
                    part("Cargo bed",13,25,38,35,main); part("Cab",16,3,32,24,main)
                    part("Windshield",20,7,24,9,light); part("Door left",14,17,5,9,dark); part("Door right",45,17,5,9,dark)
                    for(y in listOf(9,34,48)) { part("Wheel left $y",8,y,6,12,dark); part("Wheel right $y",50,y,6,12,dark) }
                    part("Cargo",18,31,28,22,Color.rgb(125,89,42)); part("Bumper",14,0,36,3,light)
                    part("Light left",17,2,5,3,Color.YELLOW); part("Light right",42,2,5,3,Color.YELLOW)
                }
                3 -> {
                    part("Track left",7,10,12,48,dark); part("Track right",45,10,12,48,dark)
                    part("Hull",16,10,32,46,main); part("Turret",21,21,22,24,main,true)
                    part("Gun barrel",29,0,6,30,light); part("Hatch",26,29,12,10,dark,true)
                    for(y in 12..52 step 8) { part("Tread left $y",7,y,12,2,light); part("Tread right $y",45,y,12,2,light) }
                }
                else -> {
                val width=26+index%5*2
                part("Body",32-width/2,8,width,48,main)
                part("Interior",24,22,16,23,dark)
                part("Front window",24,18,16,8,light); part("Rear window",24,39,16,7,light)
                part("Door left",17,27,5,15,main); part("Door right",42,27,5,15,main)
                part("Wheel front left",12,17,7,12,dark); part("Wheel front right",45,17,7,12,dark)
                part("Wheel rear left",12,40,7,12,dark); part("Wheel rear right",45,40,7,12,dark)
                part("Headlight left",21,8,6,4,Color.YELLOW); part("Headlight right",37,8,6,4,Color.YELLOW)
                part("Mirror left",10,28,7,3,light); part("Mirror right",47,28,7,3,light)
                part("Front bumper",19,5,26,3,dark); part("Rear bumper",19,56,26,3,dark)
                part("Spoiler",17,51,30,3,light)
                }
            }
            "Character","Enemy","NPC" -> {
                val offset=index%4
                part("Left leg",22,41,8,17,dark); part("Right leg",34,41,8,17,dark)
                part("Torso",20-offset,23,24+offset*2,22,main)
                part("Left arm",12-offset,24,8,20,main); part("Right arm",44+offset,24,8,20,main)
                part("Head",22,5,20,19,if(category=="Enemy")main else Color.rgb(170+index%7*10,120+index%9*7,90+index%8*8),category=="Enemy")
                part("Hair or helmet",20,3,24,6,dark)
                part("Left eye",26,13,3,3,light); part("Right eye",35,13,3,3,light)
                if(index%2==0)part("Belt",19,37,26,3,light)
                if(index%3==0)part("Tool",52,32,4,25,light)
            }
            "Animal" -> {
                part("Body",11,24,40,22,main,true)
                part("Head",39,13,19,23,main,true); part("Ear",42,5,8,17,dark,true)
                part("Front leg",40,40,6,17,dark); part("Rear leg",16,40,6,17,dark)
                part("Tail",3,17+index%8,8,23,main); part("Eye",49,21,3,3,light)
            }
            "Building" -> when(index%5) {
                2 -> {
                    part("Road",0,0,64,64,dark); part("Sidewalk left",0,0,10,64,light); part("Sidewalk right",54,0,10,64,light)
                    for(y in 2..56 step 18)part("Lane marking $y",30,y,4,10,Color.YELLOW)
                }
                3 -> {
                    part("Water",0,0,64,64,Color.rgb(35,110,180)); part("Bridge deck",12,0,40,64,main)
                    part("Rail left",10,0,4,64,light); part("Rail right",50,0,4,64,light)
                    for(y in 0..56 step 8)part("Plank $y",14,y,36,2,dark)
                }
                4 -> {
                    part("Tower",10,0,44,64,main)
                    for(y in 5..45 step 13)for(x in listOf(16,28,40))part("Window $x $y",x,y,7,8,light)
                    part("Entry",26,53,12,11,dark)
                }
                else -> {
                part("Wall",8,24,48,36,main)
                val roof=Bitmap.createBitmap(60,25,Bitmap.Config.ARGB_8888)
                val path=android.graphics.Path().apply { moveTo(0f,25f); lineTo(30f,(index%5).toFloat()); lineTo(60f,25f); close() }
                Canvas(roof).drawPath(path,Paint().apply { color=dark }); parts.add(ProceduralPart("Roof",roof,2,0))
                part("Door",26,39,12,21,dark); part("Window left",12,32,10,12,light); part("Window right",42,32,10,12,light)
                if(index%2==0)part("Chimney",45,4,7,20,dark)
                }
            }
            "Nature" -> {
                if(index%3==0) { part("Rock",8,20,48,37,main,true); part("Highlight",16,23,22,8,light,true) }
                else { part("Trunk",27,28,10,34,Color.rgb(106,72,39)); part("Canopy",6,3,52,43,Color.rgb(30+index*3,100+index*3,40),true); part("Leaves",14,1,31,27,main,true) }
            }
            "Prop" -> {
                when(index%4) {
                    0 -> { part("Crate",8,8,48,48,main); part("Brace top",8,10,48,6,dark); part("Brace bottom",8,48,48,6,dark); part("Brace middle",27,8,8,48,light) }
                    1 -> { part("Barrel",14,7,36,50,main,true); part("Band top",15,17,34,5,dark); part("Band bottom",15,42,34,5,dark) }
                    2 -> { part("Table",6,18,52,10,main); part("Leg left",10,28,6,30,dark); part("Leg right",48,28,6,30,dark) }
                    else -> { part("Handle",27,35,8,24,main); part("Tool head",12,12,40,25,light); part("Fastener",29,19,5,5,dark) }
                }
            }
        }
        return parts
    }
    fun render(recipe: String): Bitmap {
        val bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888)
        val layers=layers(recipe)
        if(layers!=null) {
            val canvas=Canvas(bitmap)
            try { layers.forEach { canvas.drawBitmap(it.bitmap,it.x.toFloat(),it.y.toFloat(),null) } } finally { layers.forEach { it.bitmap.recycle() } }
        } else {
            // Integer-frequency periodic fields guarantee tileable patterns; mirrored edge samples match.
            val index=recipe.substringAfter(':').toInt(); val hue=index*43%360
            val pixels=IntArray(4096) { i ->
                val x=i%64; val y=i/64
                val value=(.5+.2*cos(2*PI*(index%5+1)*x/63)+.2*cos(2*PI*(index%7+1)*y/63)).toFloat()
                Color.HSVToColor(floatArrayOf(hue.toFloat(),.2f+(index%6)*.1f,value.coerceIn(.1f,1f)))
            }
            bitmap.setPixels(pixels,0,64,0,0,64,64)
        }
        return bitmap
    }
}
