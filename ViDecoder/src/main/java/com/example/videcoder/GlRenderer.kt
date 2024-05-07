package com.example.videcoder

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.videcoder.googleTestCode.TextureRender
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlRenderer: GLSurfaceView.Renderer {
    private val frameQueue: Queue<ByteBuffer> = LinkedList()
    private val frameRenderInterval: Long // Time gap between rendering frames in milliseconds
    private var lastRenderTime: Long = 0
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()


    private val vertexShaderCode = "attribute vec4 a_Position;" +
            "attribute vec2 a_TexCoord;" +
            "varying vec2 v_TexCoord;" +
            "void main() {" +
            "  gl_Position = a_Position;" +
            "  v_TexCoord = a_TexCoord;" +
            "}"

    // Define your fragment shader code here
    private val fragmentShaderCode = "precision mediump float;" +
            "uniform sampler2D u_Texture;" +
            "varying vec2 v_TexCoord;" +
            "void main() {" +
            "  gl_FragColor = texture2D(u_Texture, v_TexCoord);" +
            "}"

    private var frames_rendered = 0

    // Shader program handles
//    private var shaderProgram: Int = 0
//    private var positionHandle: Int = 0
//    private var texCoordHandle: Int = 0
//    private var textureUniformHandle: Int = 0
//    private var verticesBuffer: FloatBuffer
//    private var texCoordsBuffer: FloatBuffer
//    private var indicesBuffer: IntBuffer
    lateinit var mTextureRender: TextureRender

    val mContext: Context

    lateinit var mGlView: GLSurfaceView


    constructor(frameRenderInterval: Long, context: Context, glView: GLSurfaceView ) {
        this.frameRenderInterval = frameRenderInterval
        this.mContext = context
        this.mGlView = glView
//        this.verticesBuffer = ByteBuffer.allocateDirect(4 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
//        this.texCoordsBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
//        this.indicesBuffer = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        lastRenderTime = System.currentTimeMillis()
    }

    fun queueFrame(frameData: ByteBuffer) {
        synchronized(frameQueue) {
            "queued byte buffer...${frameData}".rlog()
            frameQueue.offer(frameData)
        }
    }

    var textureId = -1
    var program = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = compileAndLinkProgram(vertexShaderCode, fragmentShaderCode);

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        executor.scheduleAtFixedRate(::mRequestRender, 0, 33, TimeUnit.MILLISECONDS)
    }

    fun mRequestRender(){
        synchronized(frameQueue) {
            if (!frameQueue.isEmpty()) {
                mGlView.requestRender()
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Set viewport
        GLES20.glViewport(0, 0, width, height)
        // Additional setup if needed
    }

    override fun onDrawFrame(gl: GL10?) {
        "draw frame called...".rlog()
        // Clear the screen
        synchronized(frameQueue) {
            if (!frameQueue.isEmpty()) {
                val frameData = frameQueue.poll()
                if (frameData != null) {
                    "render bitch".rlog()
                    loadFrameIntoTexture(frameData)
                    renderTextureQuad()
                }
            }
        }
    }

    private fun loadFrameIntoTexture(frameBuffer: ByteBuffer) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            540,
            800,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            frameBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun renderTextureQuad() {
        // Use shader program
        GLES20.glUseProgram(program)

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Get attribute and uniform locations
        val positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
        val textureUniformHandle = GLES20.glGetUniformLocation(program, "u_Texture")

        // Set texture unit
        GLES20.glUniform1i(textureUniformHandle, 0)

        // Enable vertex and texture coordinate arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // Specify vertex and texture coordinate data
        val quadVertices = floatArrayOf(
            -1.0f, 1.0f,  // Top left
            -1.0f, -1.0f,  // Bottom left
            1.0f, -1.0f,  // Bottom right
            1.0f, 1.0f // Top right
        )
        val textureCoordinates = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        )
        val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVertices)
        vertexBuffer.position(0)
        val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(textureCoordinates.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureCoordinates)
        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun compileAndLinkProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    // Method to render a texture from pixel data
//    private fun renderTexture(pixelData: ByteBuffer, width: Int, height: Int) {
//        // Generate texture ID
//        val textureId = IntArray(1)
//        GLES20.glGenTextures(1, textureId, 0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
//
//        GLES20.glTexImage2D(
//            GLES20.GL_TEXTURE_2D,
//            0,
//            GLES20.GL_RGBA,
//            width,
//            height,
//            0,
//            GLES20.GL_RGBA,
//            GLES20.GL_UNSIGNED_BYTE,
//            pixelData
//        )
//
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
//
//        val vertices = floatArrayOf(
//            -1.0f, -1.0f, 0.0f, // bottom left
//            1.0f, -1.0f, 0.0f,  // bottom right
//            -1.0f, 1.0f, 0.0f,  // top left
//            1.0f, 1.0f, 0.0f    // top right
//        )
//
//        val texCoords = floatArrayOf(
//            0.0f, 1.0f, // bottom left
//            1.0f, 1.0f, // bottom right
//            0.0f, 0.0f, // top left
//            1.0f, 0.0f  // top right
//        )
//
//        val indices = intArrayOf(
//            0, 1, 2, // first triangle
//            2, 1, 3  // second triangle
//        )
//
//        // Use shader program for texture rendering
//        // Bind texture coordinates
//        // Bind texture
//        // Draw the quad
//        // Clean up
//
//        // Example:
//        // Bind shader program
//        // Bind texture coordinates
//        // Bind texture
//        // Draw the quad
//        // Clean up
//
//        // Bind shader program
//        GLES20.glUseProgram(shaderProgram)
//
//        // Bind texture coordinates
//        GLES20.glEnableVertexAttribArray(texCoordHandle)
//        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordsBuffer)
//
//        // Bind texture
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
//        GLES20.glUniform1i(textureUniformHandle, 0)
//
//        // Draw the quad
//        GLES20.glEnableVertexAttribArray(positionHandle)
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, verticesBuffer)
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_INT, indicesBuffer)
//
//        // Clean up
//        GLES20.glDisableVertexAttribArray(positionHandle)
//        GLES20.glDisableVertexAttribArray(texCoordHandle)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
//        GLES20.glUseProgram(0)
//        GLES20.glDeleteTextures(1, textureId, 0)
//    }
}