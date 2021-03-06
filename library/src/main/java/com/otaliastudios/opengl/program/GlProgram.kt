package com.otaliastudios.opengl.program


import android.opengl.GLES20
import com.otaliastudios.opengl.core.Egloo
import com.otaliastudios.opengl.draw.GlDrawable

/**
 * Base class for a program that accepts a vertex and a fragment shader in the constructor.
 * The program will be created automatically and released when [release] is called.
 *
 * Subclasses are required to do two things - typically, during the [onPreDraw] callback:
 * 1 Inspect the [GlDrawable] properties:
 *   - [GlDrawable.vertexArray]
 *   - [GlDrawable.coordsPerVertex]
 *   - [GlDrawable.vertexStride]
 *   These should be passed to the vertex shader.
 * 2 Pass the MVP matrix to the vertex shader as well.
 *
 * The vertex shader should then use the two to compute the gl_Position.
 */
abstract class GlProgram(
        @Suppress("MemberVisibilityCanBePrivate") val vertexShader: String,
        @Suppress("MemberVisibilityCanBePrivate") val fragmentShader: String
) {

    protected var handle = createProgram()
        private set

    // Creates a program with given vertex shader and pixel shader.
    private fun createProgram(): Int {
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        if (pixelShader == 0) throw RuntimeException("Could not load fragment shader")
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        if (vertexShader == 0) throw RuntimeException("Could not load vertex shader")

        val program = GLES20.glCreateProgram()
        Egloo.checkGlError("glCreateProgram")
        if (program == 0) {
            throw RuntimeException("Could not create program")
        }
        GLES20.glAttachShader(program, vertexShader)
        Egloo.checkGlError("glAttachShader")
        GLES20.glAttachShader(program, pixelShader)
        Egloo.checkGlError("glAttachShader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val message = "Could not link program: " + GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException(message)
        }
        return program
    }

    @Suppress("unused")
    open fun release() {
        if (handle != -1) {
            GLES20.glDeleteProgram(handle)
            handle = -1
        }
    }

    @JvmOverloads
    fun draw(drawable: GlDrawable,
             modelViewProjectionMatrix: FloatArray = drawable.modelMatrix) {
        Egloo.checkGlError("draw start")
        GLES20.glUseProgram(handle)
        Egloo.checkGlError("glUseProgram")

        onPreDraw(drawable, modelViewProjectionMatrix)
        onDraw(drawable)
        onPostDraw(drawable)

        GLES20.glUseProgram(0)
        Egloo.checkGlError("draw end")
    }

    protected open fun onPreDraw(drawable: GlDrawable, modelViewProjectionMatrix: FloatArray) {}

    protected open fun onDraw(drawable: GlDrawable) {
        drawable.draw()
    }

    protected open fun onPostDraw(drawable: GlDrawable) {}

    protected fun getAttribHandle(name: String) = GlHandle.getAttrib(handle, name)

    protected fun getUniformHandle(name: String) = GlHandle.getUniform(handle, name)

    companion object {
        @Suppress("unused")
        internal val TAG = GlProgram::class.java.simpleName

        // Compiles the given shader, returns a handle.
        private fun loadShader(shaderType: Int, source: String): Int {
            val shader = GLES20.glCreateShader(shaderType)
            Egloo.checkGlError("glCreateShader type=$shaderType")
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val message = "Could not compile shader $shaderType: ${GLES20.glGetShaderInfoLog(shader)} source: $source"
                GLES20.glDeleteShader(shader)
                throw RuntimeException(message)
            }
            return shader
        }
    }
}