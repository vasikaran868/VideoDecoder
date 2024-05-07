package com.example.videcoder

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.videcoder.gles.FullFrameRect
import com.example.videcoder.gles.Texture2dProgram
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10



/**
 * Renderer object for our GLSurfaceView.
 *
 *
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
class GraficaRenderer(
//    cameraHandler: DroidRenderer.CameraHandler,
    movieEncoder: TextureMovieEncoder, outputFile: File
) : GLSurfaceView.Renderer {
//    private val mCameraHandler: DroidRenderer.CameraHandler
    private val mVideoEncoder: TextureMovieEncoder
    private val mOutputFile: File
    private var mFullScreen: FullFrameRect? = null
    private val mSTMatrix = FloatArray(16)
    private var mTextureId: Int
    var mSurfaceTexture: SurfaceTexture? = null
    private var mRecordingEnabled: Boolean
    private var mRecordingStatus: Int
    private var mFrameCount: Int

    // width/height of the incoming camera preview frames
    private var mIncomingSizeUpdated: Boolean
    private var mIncomingWidth: Int
    private var mIncomingHeight: Int
    private var mCurrentFilter: Int
    private var mNewFilter: Int

    /**
     * Constructs CameraSurfaceRenderer.
     *
     *
     * @param cameraHandler Handler for communicating with UI thread
     * @param movieEncoder video encoder object
     * @param outputFile output file for encoded video; forwarded to movieEncoder
     */
    init {
//        mCameraHandler = cameraHandler
        mVideoEncoder = movieEncoder
        mOutputFile = outputFile
        mTextureId = -1
        mRecordingStatus = -1
        mRecordingEnabled = false
        mFrameCount = -1
        mIncomingSizeUpdated = false
        mIncomingHeight = -1
        mIncomingWidth = mIncomingHeight

        // We could preserve the old filter mode, but currently not bothering.
        mCurrentFilter = -1
        mNewFilter = 0
        "grafika renderer init".rlog()
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     *
     *
     * For best results, call this *after* disabling Camera preview.
     */
    fun notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture")
            mSurfaceTexture!!.release()
            mSurfaceTexture = null
        }
        if (mFullScreen != null) {
            mFullScreen!!.release(false) // assume the GLSurfaceView EGL context is about
            mFullScreen = null //  to be destroyed
        }
        mIncomingHeight = -1
        mIncomingWidth = mIncomingHeight
    }

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    fun changeRecordingState(isRecording: Boolean) {
        Log.d(
            TAG,
            "changeRecordingState: was $mRecordingEnabled now $isRecording"
        )
        mRecordingEnabled = isRecording
    }

    /**
     * Changes the filter that we're applying to the camera preview.
     */
    fun changeFilterMode(filter: Int) {
        mNewFilter = filter
    }

    /**
     * Updates the filter program.
     */
//    fun updateFilter() {
//        val programType: Texture2dProgram.ProgramType
//        var kernel: FloatArray? = null
//        var colorAdj = 0.0f
//        Log.d(TAG, "Updating filter to $mNewFilter")
//        when (mNewFilter) {
//            com.android.grafika.CameraCaptureActivity.FILTER_NONE -> programType =
//                Texture2dProgram.ProgramType.TEXTURE_EXT
//            com.android.grafika.CameraCaptureActivity.FILTER_BLACK_WHITE ->                 // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
//                // ROSE_COLORED_GLASSES, because the shader set the red channel to the B&W color
//                // and green/blue to zero.)
//                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW
//            com.android.grafika.CameraCaptureActivity.FILTER_BLUR -> {
//                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                kernel = floatArrayOf(
//                    1f / 16f, 2f / 16f, 1f / 16f,
//                    2f / 16f, 4f / 16f, 2f / 16f,
//                    1f / 16f, 2f / 16f, 1f / 16f
//                )
//            }
//            com.android.grafika.CameraCaptureActivity.FILTER_SHARPEN -> {
//                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                kernel = floatArrayOf(
//                    0f, -1f, 0f,
//                    -1f, 5f, -1f,
//                    0f, -1f, 0f
//                )
//            }
//            com.android.grafika.CameraCaptureActivity.FILTER_EDGE_DETECT -> {
//                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                kernel = floatArrayOf(
//                    -1f, -1f, -1f,
//                    -1f, 8f, -1f,
//                    -1f, -1f, -1f
//                )
//            }
//            com.android.grafika.CameraCaptureActivity.FILTER_EMBOSS -> {
//                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                kernel = floatArrayOf(
//                    2f, 0f, 0f,
//                    0f, -1f, 0f,
//                    0f, 0f, -1f
//                )
//                colorAdj = 0.5f
//            }
//            else -> throw RuntimeException("Unknown filter mode $mNewFilter")
//        }
//
//        // Do we need a whole new program?  (We want to avoid doing this if we don't have
//        // too -- compiling a program could be expensive.)
//        if (programType !== mFullScreen.getProgram().getProgramType()) {
//            mFullScreen.changeProgram(Texture2dProgram(programType))
//            // If we created a new program, we need to initialize the texture width/height.
//            mIncomingSizeUpdated = true
//        }
//
//        // Update the filter kernel (if any).
//        if (kernel != null) {
//            mFullScreen.getProgram().setKernel(kernel, colorAdj)
//        }
//        mCurrentFilter = mNewFilter
//    }

    /**
     * Records the size of the incoming camera preview frames.
     *
     *
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    fun setCameraPreviewSize(width: Int, height: Int) {
        Log.d(TAG, "setCameraPreviewSize")
        mIncomingWidth = width
        mIncomingHeight = height
        mIncomingSizeUpdated = true
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        Log.d(TAG, "onSurfaceCreated")
        "grafika renderer surface created".rlog()

        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = mVideoEncoder.isRecording()
        mRecordingStatus = if (mRecordingEnabled) {
            RECORDING_RESUMED
        } else {
            RECORDING_OFF
        }

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = FullFrameRect(
            Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT)
        )
        mTextureId = mFullScreen!!.createTextureObject()

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = SurfaceTexture(mTextureId)

        // Tell the UI thread to enable the camera preview.
//        mCameraHandler.sendMessage(
//            mCameraHandler.obtainMessage(
//                DroidRenderer.CameraHandler.MSG_SET_SURFACE_TEXTURE,
//                mSurfaceTexture
//            )
//        )
        mVideoEncoder.startRecording(
            TextureMovieEncoder.EncoderConfig(
                mOutputFile, 540, 800, 2000000, EGL14.eglGetCurrentContext()
            )
        )
        mVideoEncoder.setTextureId(mTextureId)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onDrawFrame(unused: GL10) {
        "grafika renderer draw frame called".rlog()
        if (VERBOSE) Log.d(
            TAG,
            "onDrawFrame tex=$mTextureId"
        )
        var showBox = false

        // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
        // was there before.
        mSurfaceTexture!!.updateTexImage()

        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
//        if (mRecordingEnabled) {
//            when (mRecordingStatus) {
//                RECORDING_OFF -> {
//                    Log.d(TAG, "START recording")
//                    // start recording
//                    mVideoEncoder.startRecording(
//                        TextureMovieEncoder.EncoderConfig(
//                            mOutputFile, 540, 800, 2000000, EGL14.eglGetCurrentContext()
//                        )
//                    )
//                    mRecordingStatus = RECORDING_ON
//                }
//                RECORDING_RESUMED -> {
//                    Log.d(TAG, "RESUME recording")
//                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext())
//                    mRecordingStatus = RECORDING_ON
//                }
//                RECORDING_ON -> {}
//                else -> throw RuntimeException("unknown status $mRecordingStatus")
//            }
//        } else {
//            when (mRecordingStatus) {
//                RECORDING_ON, RECORDING_RESUMED -> {
//                    // stop recording
//                    Log.d(TAG, "STOP recording")
//                    mVideoEncoder.stopRecording()
//                    mRecordingStatus = RECORDING_OFF
//                }
//                RECORDING_OFF -> {}
//                else -> throw RuntimeException("unknown status $mRecordingStatus")
//            }
//        }
//
//        // Set the video encoder's texture name.  We only need to do this once, but in the
//        // current implementation it has to happen after the video encoder is started, so
//        // we just do it here.
//        //
//        // TODO: be less lame.
//        mVideoEncoder.setTextureId(mTextureId)

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(mSurfaceTexture)
        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping")
            return
        }
        // Update the filter, if necessary.
//        if (mCurrentFilter != mNewFilter) {
//            updateFilter()
//        }
        if (mIncomingSizeUpdated) {
            mFullScreen?.program?.setTexSize(mIncomingWidth, mIncomingHeight)
            mIncomingSizeUpdated = false
        }

        // Draw the video frame.
        mSurfaceTexture!!.getTransformMatrix(mSTMatrix)
        mFullScreen?.drawFrame(mTextureId, mSTMatrix)

        // Draw a flashing box if we're recording.  This only appears on screen.
        showBox = mRecordingStatus == RECORDING_ON
        if (showBox && ++mFrameCount and 0x04 == 0) {
            drawBox()
        }
    }

    /**
     * Draws a red box in the corner.
     */
    private fun drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        GLES20.glScissor(0, 0, 100, 100)
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
    }

    companion object {
        private val TAG: String = "Debug Tag"
        private const val VERBOSE = false
        private const val RECORDING_OFF = 0
        private const val RECORDING_ON = 1
        private const val RECORDING_RESUMED = 2
    }
}