package com.example.videcoder

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.videcoder.rlog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer() : GLSurfaceView.Renderer {

    private val triangleVertices = floatArrayOf(
        0.0f, 1.0f, 0.0f,  // top
        -1.0f, -1.0f, 0.0f,  // bottom left
        1.0f, -1.0f, 0.0f // bottom right
    )

    // Simple vertex shader code
    private val vertexShaderCode = """attribute vec4 vPosition;
void main() {
  gl_Position = vPosition;
}
"""

    // Simple fragment shader code
    private val fragmentShaderCode = """precision mediump float;
void main() {
  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
}
"""

    private val vertexBuffer: FloatBuffer

    private val COORDS_PER_VERTEX = 3 // 3 coordinates per vertex (x, y, z)

    private val vertexStride = COORDS_PER_VERTEX * 4

    // Shader program variables
    private var program = 0
    private var positionHandle = 0

    init {
        val bb: ByteBuffer = ByteBuffer.allocateDirect(triangleVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleVertices)
        vertexBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        "surface created".rlog()
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Create OpenGL program and link shaders

        // Create OpenGL program and link shaders
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // Get attribute handle

        // Get attribute handle
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")

        // Enable vertex array

        // Enable vertex array
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    }

    override fun onDrawFrame(gl: GL10?) {
        "on draw frame".rlog()
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program for rendering
        GLES20.glUseProgram(program);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}