package com.example.videcoder

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.GLSurfaceView
import com.example.videcoder.googleTestCode.TextureRender
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlRenderer(val onSurfaceCreated: (GlRenderer, EGLContext) -> Unit): GLSurfaceView.Renderer {

    public lateinit var mSurfaceTexture: SurfaceTexture
    public lateinit var mTextureRender: TextureRender
    public var mTextureId: Int? = null
    private val mSTMatrix = FloatArray(16)
    init {
        "hiii".rlog()
    }

    fun changeFragmentShader(fragmentShader: String?) {
        mTextureRender.changeFragmentShader(fragmentShader)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        "surface created...".rlog()
        mTextureRender = TextureRender()
        mTextureRender.surfaceCreated()
        mTextureId = mTextureRender.textureId
        mSurfaceTexture = SurfaceTexture(mTextureRender.textureId)
        onSurfaceCreated(this, EGL14.eglGetCurrentContext())
    }

    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
    }

    override fun onDrawFrame(p0: GL10?) {
//        mTextureRender.checkGlError("before updateTexImage")
//        mSurfaceTexture.updateTexImage()
        mTextureRender.drawFrame(mSurfaceTexture)
    }
}