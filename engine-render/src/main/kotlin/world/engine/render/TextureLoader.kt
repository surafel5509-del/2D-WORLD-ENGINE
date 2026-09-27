package world.engine.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30.*
import android.opengl.GLUtils
import java.io.File

/** Textures belong to one GL context. All calls must run on its render thread. */
class TextureLoader(private val report: (String) -> Unit) {
    private val cache=mutableMapOf<String,Int>()
    var white=0; private set
    private var missing=0
    private var bytesUsed=0L
    fun initialize() {
        cache.clear(); bytesUsed=0
        val solid=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888); solid.eraseColor(-1)
        white=upload(solid); solid.recycle()
        val check=Bitmap.createBitmap(intArrayOf(0xffff00ff.toInt(),0xff202020.toInt(),0xff202020.toInt(),0xffff00ff.toInt()),2,2,Bitmap.Config.ARGB_8888)
        missing=upload(check); check.recycle()
    }
    fun clear() { cache.values.filter { it!=missing }.distinct().forEach { glDeleteTextures(1,intArrayOf(it),0) }; cache.clear(); bytesUsed=0 }
    fun get(root: File?, path: String?): Int {
        if(path==null) return white
        return cache.getOrPut(path) {
            try {
                require(root!=null)
                val file=File(root,path)
                require(file.canonicalPath.startsWith(root.canonicalPath+File.separator))
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                BitmapFactory.decodeFile(file.path,bounds)
                require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) { "Missing or oversized texture: $path" }
                val bytes=bounds.outWidth.toLong()*bounds.outHeight*4
                require(bytesUsed+bytes<=64L*1024*1024) { "64 MiB texture budget exhausted; use smaller images" }
                val bitmap=BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inPremultiplied=false }) ?: error("Cannot decode $path")
                try { upload(bitmap).also { bytesUsed+=bytes } } finally { bitmap.recycle() }
            } catch(e: Exception) { report("Texture $path: ${e.message}. Showing checkerboard; re-import the image."); missing }
        }
    }
    private fun upload(bitmap: Bitmap): Int {
        val id=IntArray(1); glGenTextures(1,id,0); glBindTexture(GL_TEXTURE_2D,id[0])
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0)
        return id[0]
    }
}
