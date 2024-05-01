package com.example.videcoder

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresApi
import com.example.videcoder.googleTestCode.ExtractDecodeEditEncodeMuxTest
import com.example.videcoder.googleTestCode.InputSurface
import com.example.videcoder.googleTestCode.OutputSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference


class DroidRenderer: SurfaceView, SurfaceHolder.Callback {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    lateinit var mediaPlayer: MediaPlayer
    val videoDecoder: VideoDecoder
    var videoEncoder : VideoEncoder
    val file: File

    init {
        this.holder.addCallback(this)
        val downloadsDir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs() // Create the directory if it doesn't exist
        }
        file = File(downloadsDir, "output21.mp4")
        videoEncoder = VideoEncoder(file)
        videoDecoder = VideoDecoder(context, file)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun renderPreview(scope : CoroutineScope, maskVideoUri: Uri, contentVideoUri: Uri, textureView: TextureView){
        try {
            scope.launch(Dispatchers.IO) {
                ExtractDecodeEditEncodeMuxTest().apply {
                    setSize(540, 800)
                    setSource(R.raw.content_video)
                    setCopyVideo()
                    setCopyAudio()
                    setOutputFile(file)
                    val videoCodecInfo =
                        ExtractDecodeEditEncodeMuxTest.selectCodec(
                            "video/avc" // H.264 Advanced Video Coding
                        )
                    val outputVideoFormat = MediaFormat.createVideoFormat(
                        "video/avc", // H.264 Advanced Video Coding
                        540,
                        800
                    )
                    outputVideoFormat.setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    outputVideoFormat.setInteger(
                        MediaFormat.KEY_BIT_RATE, 2000000 // 2Mbps

                    )
                    outputVideoFormat.setInteger(
                        MediaFormat.KEY_FRAME_RATE,30
                    )
                    outputVideoFormat.setInteger(
                        MediaFormat.KEY_I_FRAME_INTERVAL, 10 // 10 seconds between I-frames

                    )
                    val inputSurfaceReference:  AtomicReference<Surface> = AtomicReference<Surface>();
                    val mVideoEncoder = ExtractDecodeEditEncodeMuxTest.createVideoEncoder(
                        videoCodecInfo, outputVideoFormat, inputSurfaceReference
                    )
                    "input surface..${inputSurfaceReference.get().hashCode()}".rlog()
                    val inputSurface = InputSurface(inputSurfaceReference.get())
                    inputSurface.makeCurrent()
                    val outputSurface = OutputSurface()
                    outputSurface.changeFragmentShader(ExtractDecodeEditEncodeMuxTest.FRAGMENT_SHADER)
                    "output surface..${outputSurface.surface.hashCode()}".rlog()
                    "output surface..${this@DroidRenderer.holder.surface}".rlog()
                    extractDecodeEditEncodeMux(
                        context,
                        contentVideoUri,
                        this@DroidRenderer.holder.surface,
                        outputSurface,
                        inputSurface,
                        inputSurfaceReference,
                        mVideoEncoder
                    );
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun decode(extractor: MediaExtractor) {

    }


    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(a: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        videoDecoder.releaseDecoder()
        videoEncoder.releaseEncoder()
    }


}