package com.example.videcoder.googleTestCode

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.example.videcoder.GlRenderer
import com.example.videcoder.rlog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10

class OutputSurface(val renderer: GlRenderer, val glView: GLSurfaceView) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLSurface = EGL14.EGL_NO_SURFACE
    var mSurfaceTexture: SurfaceTexture? = null
    var rMaskSurfaceTexture: SurfaceTexture? = null

    /**
     * Returns the Surface that we draw onto.
     */
    var surface: Surface? = null
    var maskSurface: Surface? = null
    private val mFrameSyncObject = Object()// guards mFrameAvailable
    private var mFrameAvailable = false
    private var mTextureRender: TextureRender? = null
    private var frameCount = 0
    private var mFrameAvailableCount = 0
    private val TOTAL_SURFACES = 2

    /**
     * Creates an OutputSurface backed by a pbuffer with the specifed dimensions.  The new
     * EGL context and surface will be made current.  Creates a Surface that can be passed
     * to MediaCodec.configure().
     */
//    constructor(width: Int, height: Int) {
//        require(!(width <= 0 || height <= 0))
//        eglSetup(width, height)
//        makeCurrent()
//        setup()
//    }
    init {
        setup()
    }
    /**
     * Creates an OutputSurface using the current EGL context (rather than establishing a
     * new one).  Creates a Surface that can be passed to MediaCodec.configure().
     */
//    constructor() {
//        setup()
//    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private fun setup() {
        mTextureRender = TextureRender()
        mTextureRender!!.surfaceCreated()
//        // Even if we don't access the SurfaceTexture after the constructor returns, we
//        // still need to keep a reference to it.  The Surface doesn't retain a reference
//        // at the Java level, so if we don't either then the object can get GCed, which
//        // causes the native finalizer to run.
//        if (VERBOSE) Log.d(TAG, "textureID=" + mTextureRender!!.textureId)
        mSurfaceTexture = SurfaceTexture(mTextureRender!!.textureId)
        rMaskSurfaceTexture = SurfaceTexture(mTextureRender!!.maskTextureId)
        // This doesn't work if OutputSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, OutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        val l = Looper.myLooper()
        "otu surface looper...${l}".rlog()

        mSurfaceTexture!!.setOnFrameAvailableListener(this, Handler(Looper.getMainLooper()))
        rMaskSurfaceTexture!!.setOnFrameAvailableListener(this, Handler(Looper.getMainLooper()))
        surface = Surface(mSurfaceTexture)
        maskSurface = Surface(rMaskSurfaceTexture)
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private fun eglSetup(width: Int, height: Int) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }
        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                mEGLDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
        }
        // Configure context for OpenGL ES 2.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        mEGLContext = EGL14.eglCreateContext(
            mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        checkEglError("eglCreateContext")
        if (mEGLContext == null) {
            throw RuntimeException("null context")
        }
        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        mEGLSurface = EGL14.eglCreatePbufferSurface(
            mEGLDisplay,
            configs[0], surfaceAttribs, 0
        )
        checkEglError("eglCreatePbufferSurface")
        if (mEGLSurface == null) {
            throw RuntimeException("surface was null")
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    fun release() {
        if (mEGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        surface!!.release()
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLSurface = EGL14.EGL_NO_SURFACE
        mTextureRender = null
        surface = null
        mSurfaceTexture = null
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Replaces the fragment shader.
     */
    fun changeFragmentShader(fragmentShader: String?) {
        mTextureRender!!.changeFragmentShader(fragmentShader)
//        glRenderer.changeFragmentShader(fragmentShader)
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    fun awaitNewImage() {
//        String b = Looper.myLooper().to;
//        Log.v("Debug Tag", "creation looper" + b);
        val TIMEOUT_MS = 5000
        synchronized(mFrameSyncObject) {
            while (mFrameAvailableCount < TOTAL_SURFACES) {
                try {
                    // Wait for onFrameAvailable() to signal us. Use a timeout to avoid stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS.toLong())
                    if (mFrameAvailableCount < TOTAL_SURFACES) {
                        // Timeout occurred or not all SurfaceTextures have signaled.
                        throw RuntimeException("Surface frame wait timed out or not all SurfaceTextures have signaled.")
                    }
                } catch (ie: InterruptedException) {
                    // shouldn't happen
                    throw RuntimeException(ie)
                }
            }
            // Reset the count for the next iteration
            mFrameAvailableCount = 0
        }
        // Latch the data.
        mTextureRender?.checkGlError("before updateTexImage")
        mSurfaceTexture?.updateTexImage()
        rMaskSurfaceTexture?.updateTexImage()
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    fun drawImage() {
        mTextureRender!!.drawFrame(mSurfaceTexture, rMaskSurfaceTexture)
        "getting bitmap".rlog()
        val buf = ByteBuffer.allocateDirect(540 * 800 * 4)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        GLES20.glReadPixels(0, 0, 540, 800, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
        buf.rewind()
        renderer.queueFrame(buf)
    }

    override fun onFrameAvailable(st: SurfaceTexture) {
//        if (VERBOSE) Log.d(TAG, "new frame available")
        frameCount++
        Log.v("Debug Tag", "frame $frameCount available called..${st.hashCode()}")
        synchronized(mFrameSyncObject) {
            mFrameAvailableCount++
            if (mFrameAvailableCount >= TOTAL_SURFACES) {
                // Both SurfaceTextures have signaled, notify the waiting thread.
                mFrameSyncObject.notifyAll()
            }
        }
    }

    /**
     * Checks for EGL errors.
     */
    private fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }

    companion object {
        private const val TAG = "Debug Tag"
        private const val VERBOSE = true
    }

    override fun onSurfaceCreated(p0: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        "on surface called called".rlog()
    }

    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
    }

    override fun onDrawFrame(p0: GL10?) {
        "on draw frameee called".rlog()
    }
}