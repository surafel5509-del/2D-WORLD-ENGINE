package world.engine.render

import android.opengl.GLES30.*
import world.engine.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** GLES 3 streaming batch. Consecutive sprites sharing a texture use one draw call. GL-thread only. */
class SpriteBatch {
    private val data=ByteBuffer.allocateDirect(6*8*4*1024).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var program=0
    private var vao=0
    private var vbo=0
    private var texture=0
    private var vertices=0
    private var primitive=GL_TRIANGLES
    private lateinit var camera: Camera2D
    private var width=1
    private var height=1
    fun initialize() {
        val vertex=compile(GL_VERTEX_SHADER,"""#version 300 es
            layout(location=0) in vec2 position;
            layout(location=1) in vec2 uv;
            layout(location=2) in vec4 color;
            out vec2 texCoord;
            out vec4 tint;
            void main(){ gl_Position=vec4(position,0.0,1.0); texCoord=uv; tint=color; }
        """.trimIndent())
        val fragment=compile(GL_FRAGMENT_SHADER,"""#version 300 es
            precision mediump float;
            in vec2 texCoord;
            in vec4 tint;
            uniform sampler2D image;
            out vec4 outputColor;
            void main(){ outputColor=texture(image,texCoord)*tint; }
        """.trimIndent())
        program=glCreateProgram(); glAttachShader(program,vertex); glAttachShader(program,fragment); glLinkProgram(program)
        val ok=IntArray(1); glGetProgramiv(program,GL_LINK_STATUS,ok,0)
        check(ok[0]!=0) { glGetProgramInfoLog(program) }
        glDeleteShader(vertex); glDeleteShader(fragment)
        val ids=IntArray(1); glGenVertexArrays(1,ids,0); vao=ids[0]; glGenBuffers(1,ids,0); vbo=ids[0]
        glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferData(GL_ARRAY_BUFFER,data.capacity()*4,null,GL_DYNAMIC_DRAW)
        glEnableVertexAttribArray(0); glVertexAttribPointer(0,2,GL_FLOAT,false,32,0)
        glEnableVertexAttribArray(1); glVertexAttribPointer(1,2,GL_FLOAT,false,32,8)
        glEnableVertexAttribArray(2); glVertexAttribPointer(2,4,GL_FLOAT,false,32,16)
    }
    private fun compile(type: Int, source: String): Int {
        val shader=glCreateShader(type); glShaderSource(shader,source); glCompileShader(shader)
        val ok=IntArray(1); glGetShaderiv(shader,GL_COMPILE_STATUS,ok,0)
        check(ok[0]!=0) { glGetShaderInfoLog(shader) }; return shader
    }
    fun begin(camera: Camera2D, width: Int, height: Int) {
        this.camera=camera; this.width=width; this.height=height; vertices=0; data.clear()
        glUseProgram(program); glBindVertexArray(vao); glActiveTexture(GL_TEXTURE0)
        glUniform1i(glGetUniformLocation(program,"image"),0)
    }
    fun draw(id: Int, transform: Transform, size: Vec2, color: Color) = drawMatrix(id,transform.matrix(),size,color)
    fun drawMatrix(id: Int, matrix: Mat3, size: Vec2, color: Color) {
        if(texture!=id || data.remaining()<48 || primitive!=GL_TRIANGLES) flush()
        primitive=GL_TRIANGLES;texture=id
        val corners=listOf(Vec2(-size.x/2,-size.y/2),Vec2(size.x/2,-size.y/2),Vec2(size.x/2,size.y/2),Vec2(-size.x/2,size.y/2))
        val uv=listOf(Vec2(0f,1f),Vec2(1f,1f),Vec2(1f,0f),Vec2(0f,0f))
        for(i in intArrayOf(0,1,2,0,2,3)) {
            val p=camera.worldToClip(matrix.map(corners[i]),width,height)
            data.put(p.x).put(p.y).put(uv[i].x).put(uv[i].y).put(color.r).put(color.g).put(color.b).put(color.a); vertices++
        }
    }
    /** Physics overlays use actual GL_LINES, sharing the shader/VBO with sprites. */
    fun line(id: Int,start: Vec2,end: Vec2,color: Color) {
        if(texture!=id || data.remaining()<16 || primitive!=GL_LINES)flush()
        texture=id;primitive=GL_LINES
        listOf(start,end).forEach { point -> val p=camera.worldToClip(point,width,height)
            data.put(p.x).put(p.y).put(0f).put(0f).put(color.r).put(color.g).put(color.b).put(color.a);vertices++
        }
    }
    fun flush() {
        if(vertices==0) return
        data.flip(); glBindTexture(GL_TEXTURE_2D,texture); glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferSubData(GL_ARRAY_BUFFER,0,data.remaining()*4,data); glDrawArrays(primitive,0,vertices)
        data.clear(); vertices=0
    }
}
