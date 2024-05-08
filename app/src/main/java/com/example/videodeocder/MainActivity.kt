package com.example.videodeocder

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.videcoder.*
import com.example.videcoder.googleTestCode.ExtractDecodeEditEncodeMuxTest
import com.example.videcoder.googleTestCode.InputSurface
import com.example.videcoder.googleTestCode.OutputSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

private const val REQUEST_PERMISSION_CODE = 123

class MainActivity : AppCompatActivity() {

    lateinit var renderer: ImageView
    lateinit var texture: LinearLayoutCompat
    lateinit var videoUri: Uri
    lateinit var maskUri: Uri
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        renderer = findViewById<ImageView>(R.id.renderer)
        texture = findViewById<LinearLayoutCompat>(R.id.tt)
//        texture.mSetRenderer()
        val previewBtn = findViewById<AppCompatButton>(R.id.previewBtn)
        val releaseBtn = findViewById<AppCompatButton>(R.id.releaseBtn)
        videoUri =
            Uri.parse("android.resource://" + this.packageName + "/" + R.raw.content_video)
//        maskUri =
//            Uri.parse("android.resource://" + this.packageName + "/" + com.example.videcoder.R.raw.mask_video)
//        "video uri...${videoUri}...${maskUri}".rlog()
        previewBtn.setOnClickListener {
            try {
                val glView = MyGlSurfaceView(this@MainActivity)
                glView.setEGLContextClientVersion(2)
                val renderer = GlRenderer(30, this, glView)
                glView.setRenderer(renderer)
                glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                glView.layoutParams =  ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                texture.addView(glView)


                val downloadsDir: File =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs() // Create the directory if it doesn't exist
                }
                val file = File(downloadsDir, "output38.mp4")
                ExtractDecodeEditEncodeMuxTest().apply {
                    setSize(540, 800)
                    setSource(com.example.videcoder.R.raw.content_video)
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
                    lifecycleScope.launch(Dispatchers.Default) {
                        val inputSurfaceReference: AtomicReference<Surface> = AtomicReference<Surface>();
                        val mVideoEncoder = ExtractDecodeEditEncodeMuxTest.createVideoEncoder(
                            videoCodecInfo, outputVideoFormat, inputSurfaceReference
                        )
                        val inputSurface = InputSurface(inputSurfaceReference.get())
                        inputSurface.makeCurrent()
                        val outputSurface = OutputSurface(renderer, glView)
                        "output surface..${outputSurface.surface.hashCode()}".rlog()
                        extractDecodeEditEncodeMux(
                            this@MainActivity,
                            Uri.parse("android.resource://" + "com.example.videodeocder" + "/" + com.example.videcoder.R.raw.content_video),
                            Uri.parse("android.resource://" + "com.example.videodeocder" + "/" + com.example.videcoder.R.raw.mask_video),
                            outputSurface,
                            inputSurface,
                            inputSurfaceReference,
                            mVideoEncoder
                        );

                    }
//                    "input surface..${inputSurfaceReference.get().hashCode()}".rlog()
//                    val glView = MyGlSurfaceView(this@MainActivity)
//////                    val inputSurfaceReference: AtomicReference<Surface> = AtomicReference<Surface>();
//////                    var inputSurface: InputSurface? = null
//////                    var outputSurface: Out
//                    glView.mSetRenderer(
//                        GlRenderer(
//                            onSurfaceCreated = { render, eContext ->
//
//                            }
//                        )
//                    )
//                    glView.layoutParams =  ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                    texture.addView(glView)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }


//            texture.mSetRenderer()
//            texture.setRenderer(MyRenderer())
//            val renderer = MyRenderer()
//            texture.setEGLContextClientVersion(2)
//            texture.preserveEGLContextOnPause = true
//            texture.setRenderer(renderer)

//            if (hasWritePermission()){
//                renderer.renderPreview(lifecycleScope, maskUri, videoUri, texture)
//
////                val downloadsDir: File =
////                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
////
////                if (!downloadsDir.exists()) {
////                    downloadsDir.mkdirs() // Create the directory if it doesn't exist
////                }
////                val file = File(downloadsDir, "output7.mp4")
////                VideoDecoder(this, file).let {
////                    it.trying(videoUri)
////                }
//            } else {
//                requestWritePermission()
//            }
        }

    }

    override fun onResume() {
        super.onResume()
        texture.visibility = View.VISIBLE
    }

    // Check if the app has permission to write to external storage
    private fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request permission to write to external storage
    private fun requestWritePermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf<String>(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_PERMISSION_CODE
        )
    }

    // Handle permission request result
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to create file
//                renderer.renderPreview(lifecycleScope,maskUri, videoUri, texture)
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}