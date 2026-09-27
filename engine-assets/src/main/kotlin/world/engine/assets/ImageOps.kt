package world.engine.assets

import android.graphics.*
import java.io.InputStream

/** Bounded, deterministic bitmap operations. Source bitmaps are never mutated. */
data class TextureEdit(val operation: String, val x: Int=0, val y: Int=0, val width: Int=64, val height: Int=64, val color: Int=android.graphics.Color.WHITE)
object ImageOps {
    fun decode(input: InputStream): Bitmap {
        val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
        while(true) { val n=input.read(buffer); if(n<0)break; require(out.size()+n<=16*1024*1024) { "Image exceeds 16 MiB" }; out.write(buffer,0,n) }
        val bytes=out.toByteArray(); val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth>0 && options.outHeight>0) { "Unsupported or corrupt image" }
        options.inSampleSize=1
        while((options.outWidth.toLong()+options.inSampleSize-1)/options.inSampleSize>2048 || (options.outHeight.toLong()+options.inSampleSize-1)/options.inSampleSize>2048) options.inSampleSize*=2
        options.inJustDecodeBounds=false
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("Image decode failed")
    }
    fun apply(source: Bitmap,edit: TextureEdit): Bitmap = when(edit.operation) {
        "Crop" -> {
            SpriteFrame("Crop",edit.x,edit.y,edit.width,edit.height).validate(source.width,source.height)
            Bitmap.createBitmap(source,edit.x,edit.y,edit.width,edit.height)
        }
        "Resize" -> { bounds(edit.width,edit.height); Bitmap.createScaledBitmap(source,edit.width,edit.height,false) }
        "Rotate 90°" -> Bitmap.createBitmap(source,0,0,source.width,source.height,Matrix().apply { postRotate(90f) },false)
        "Flip X", "Flip Y" -> Bitmap.createBitmap(source,0,0,source.width,source.height,Matrix().apply { postScale(if(edit.operation=="Flip X")-1f else 1f,if(edit.operation=="Flip Y")-1f else 1f) },false)
        "Tile 2×2" -> {
            bounds(source.width*2,source.height*2)
            Bitmap.createBitmap(source.width*2,source.height*2,Bitmap.Config.ARGB_8888).also { target ->
                val canvas=Canvas(target); repeat(2) { y -> repeat(2) { x -> canvas.drawBitmap(source,(x*source.width).toFloat(),(y*source.height).toFloat(),null) } }
            }
        }
        "Colorize" -> {
            val red=Color.red(edit.color)/255f; val green=Color.green(edit.color)/255f; val blue=Color.blue(edit.color)/255f; val alpha=Color.alpha(edit.color)/255f
            Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888).also { target ->
                val paint=Paint().apply { colorFilter=ColorMatrixColorFilter(ColorMatrix(floatArrayOf(red,0f,0f,0f,0f,0f,green,0f,0f,0f,0f,0f,blue,0f,0f,0f,0f,0f,alpha,0f))) }
                Canvas(target).drawBitmap(source,0f,0f,paint)
            }
        }
        else -> error("Unknown image operation")
    }
    private fun bounds(width: Int,height: Int) { require(width in 1..2048 && height in 1..2048) { "Use dimensions from 1 to 2048" } }
}
