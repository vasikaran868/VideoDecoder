package com.example.videodeocder

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import com.example.videcoder.DroidRenderer
import com.example.videcoder.rlog

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val renderer = findViewById<DroidRenderer>(R.id.renderer)
        val previewBtn = findViewById<AppCompatButton>(R.id.previewBtn)
        val releaseBtn = findViewById<AppCompatButton>(R.id.releaseBtn)
        val videoUri =
            Uri.parse("android.resource://" + this.packageName + "/" + R.raw.content_video)
        val videoUr =
            Uri.parse("android.resource://" + this.packageName + "/" + R.raw.mask_video)
        "video uri...${videoUri}...${videoUr}".rlog()
        previewBtn.setOnClickListener {
            renderer.renderPreview(videoUr, videoUri)
        }

    }
}