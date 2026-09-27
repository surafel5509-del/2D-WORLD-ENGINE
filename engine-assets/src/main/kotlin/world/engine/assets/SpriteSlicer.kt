package world.engine.assets

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable data class SpriteFrame(val name: String,val x: Int,val y: Int,val width: Int,val height: Int) {
    fun validate(imageWidth: Int,imageHeight: Int) {
        require(name.isNotBlank() && name.length<=96)
        require(x>=0 && y>=0 && width>0 && height>0 && x.toLong()+width<=imageWidth && y.toLong()+height<=imageHeight) { "Frame must be inside the image" }
    }
}
object SpriteSlicer {
    /** Full cells only; reject grids that would discard edge pixels. */
    fun grid(width: Int,height: Int,cellWidth: Int,cellHeight: Int): List<SpriteFrame> {
        require(cellWidth>0 && cellHeight>0 && width%cellWidth==0 && height%cellHeight==0) { "Cell dimensions must divide the image exactly" }
        require((width/cellWidth).toLong()*(height/cellHeight) in 1..256) { "Use 1–256 frames" }
        return (0 until height step cellHeight).flatMap { y -> (0 until width step cellWidth).map { x -> SpriteFrame("Frame ${y/cellHeight*(width/cellWidth)+x/cellWidth+1}",x,y,cellWidth,cellHeight) } }
    }
    /** Four-connected nontransparent islands; opaque sheets require grid/manual regions. */
    fun auto(bitmap: Bitmap): List<SpriteFrame> {
        val w=bitmap.width; val h=bitmap.height
        val pixels=IntArray(w*h); bitmap.getPixels(pixels,0,w,0,0,w,h)
        val seen=BooleanArray(pixels.size); val queue=IntArray(pixels.size); val result=mutableListOf<SpriteFrame>()
        for(start in pixels.indices) {
            if(seen[start] || (pixels[start] ushr 24)==0) continue
            require(result.size<256) { "More than 256 islands; use grid/manual slicing" }
            var head=0; var tail=0; queue[tail++]=start; seen[start]=true
            var left=w; var right=0; var top=h; var bottom=0
            fun enqueue(i: Int) { if(!seen[i] && (pixels[i] ushr 24)!=0) { seen[i]=true; queue[tail++]=i } }
            while(head<tail) {
                val i=queue[head++]; val x=i%w; val y=i/w
                left=minOf(left,x); right=maxOf(right,x); top=minOf(top,y); bottom=maxOf(bottom,y)
                if(x>0)enqueue(i-1); if(x<w-1)enqueue(i+1); if(y>0)enqueue(i-w); if(y<h-1)enqueue(i+w)
            }
            result.add(SpriteFrame("Part ${result.size+1}",left,top,right-left+1,bottom-top+1))
        }
        return result.sortedWith(compareBy<SpriteFrame> { it.y }.thenBy { it.x })
    }
}
