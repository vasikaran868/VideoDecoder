package com.example.videcoder

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class DroidRenderer: SurfaceView, SurfaceHolder.Callback {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    lateinit var mediaPlayer: MediaPlayer
    val videoDecoder = VideoDecoder(context)

    init {
        this.holder.addCallback(this)
    }



    fun playUri(uri: Uri){
//        val controller = MediaController(context)
//        controller.setMediaPlayer(this)
//        this.setVideoURI(uri)
//        this.requestFocus()
//        this.seekTo(0)
//        this.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun renderPreview(){
        try {
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            val maskVideoUri = Uri.parse("android.resource://com.example.videodeocder/2131689473")
            val contentVideoUri = Uri.parse("android.resource://com.example.videodeocder/2131689472")
            coroutineScope.launch {
                videoDecoder.decode(contentVideoUri, this@DroidRenderer.holder.surface, coroutineScope)
            }
            coroutineScope.launch {
                videoDecoder.decodeMaskVideo(maskVideoUri, this@DroidRenderer.holder.surface, coroutineScope)
            }
            coroutineScope.launch {
                videoDecoder.maskedFlow.collect{ fData ->
                    val bMap = fData.image
                    "bitmap width...${bMap.width}...height...${bMap.height}".rlog()
                    this@DroidRenderer.holder.surface?.lockCanvas(null)?.apply {
                        // Clear canvas if necessary
                        drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)

                        drawColor(Color.CYAN)
                        // Draw the combined frame
                        drawBitmap( bMap , null, RectF(0f,0f, this.width.toFloat(), this.height.toFloat()), null)
                        // Unlock the canvas
                        this@DroidRenderer.holder.surface.unlockCanvasAndPost(this)
                    }
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
    }


}