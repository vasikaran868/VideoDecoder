package com.example.videodeocder

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.videcoder.DroidRenderer
import com.example.videcoder.VideoDecoder
import com.example.videcoder.mask
import com.example.videcoder.rlog
import java.io.File

private const val REQUEST_PERMISSION_CODE = 123

class MainActivity : AppCompatActivity() {

    lateinit var renderer: DroidRenderer
    lateinit var texture: TextureView
    lateinit var videoUri: Uri
    lateinit var maskUri: Uri
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        renderer = findViewById<DroidRenderer>(R.id.renderer)
        texture = findViewById<TextureView>(R.id.tt)
        val previewBtn = findViewById<AppCompatButton>(R.id.previewBtn)
        val releaseBtn = findViewById<AppCompatButton>(R.id.releaseBtn)
        videoUri =
            Uri.parse("android.resource://" + this.packageName + "/" + R.raw.content_video)
        maskUri =
            Uri.parse("android.resource://" + this.packageName + "/" + R.raw.mask_video)
        "video uri...${videoUri}...${maskUri}".rlog()
        previewBtn.setOnClickListener {
            if (hasWritePermission()){
                renderer.renderPreview(lifecycleScope, maskUri, videoUri, texture)

//                val downloadsDir: File =
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//
//                if (!downloadsDir.exists()) {
//                    downloadsDir.mkdirs() // Create the directory if it doesn't exist
//                }
//                val file = File(downloadsDir, "output7.mp4")
//                VideoDecoder(this, file).let {
//                    it.trying(videoUri)
//                }
            } else {
                requestWritePermission()
            }
        }

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
                renderer.renderPreview(lifecycleScope,maskUri, videoUri, texture)
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}