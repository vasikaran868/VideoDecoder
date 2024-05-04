package com.example.videcoder

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Environment
import android.util.AttributeSet
import android.view.Surface
import com.example.videcoder.googleTestCode.ExtractDecodeEditEncodeMuxTest
import com.example.videcoder.googleTestCode.InputSurface
import com.example.videcoder.googleTestCode.OutputSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

class MyGlSurfaceView(context: Context): GLSurfaceView(context) {

//    constructor(context: Context,private val mRenderer: Renderer) : super(context) {
//    }

//    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
//    }


    init {
//        mSetRenderer()
    }

    fun mSetRenderer(mRenderer: GlRenderer){
        "hello".rlog()
        setEGLContextClientVersion(2)
//        preserveEGLContextOnPause = true
        setRenderer(mRenderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    }

}