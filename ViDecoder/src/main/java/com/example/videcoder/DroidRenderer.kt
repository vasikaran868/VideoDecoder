package com.example.videcoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
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

    fun renderPreview(){
        try {
            val videoUri = Uri.parse("android.resource://com.example.videodeocder/2131689472")
            videoDecoder.decode(videoUri, this.holder.surface)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun decode(extractor: MediaExtractor) {

    }


    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        videoDecoder.releaseDecoder()
    }


}