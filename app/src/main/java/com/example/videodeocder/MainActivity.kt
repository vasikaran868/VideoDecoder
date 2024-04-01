package com.example.videodeocder

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.AppCompatButton
import com.example.videcoder.DroidRenderer
import com.example.videcoder.rlog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val renderer = findViewById<DroidRenderer>(R.id.renderer)
        val previewBtn = findViewById<AppCompatButton>(R.id.previewBtn)
        val releaseBtn = findViewById<AppCompatButton>(R.id.releaseBtn)
        val videoUri =
            Uri.parse("android.resource://" + this.packageName + "/" + R.raw.video_sample)
        "video uri...${videoUri}".rlog()
        previewBtn.setOnClickListener {
            renderer.renderPreview()
        }

    }
}