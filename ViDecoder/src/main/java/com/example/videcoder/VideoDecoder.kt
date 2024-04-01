package com.example.videcoder

import android.content.Context
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.view.Surface
import java.nio.ByteBuffer

class VideoDecoder(val context: Context) {

    private var decoder: MediaCodec? = null
    private val info = MediaCodec.BufferInfo()

    fun decode(uri: Uri, surface: Surface){
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        val format = selectTrack(extractor)

        decoder = format.getString(MediaFormat.KEY_MIME)
            ?.let { MediaCodec.createDecoderByType(it) }
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        decoder?.setCallback( object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
                val inputBuffer = codec.getInputBuffer(inputBufferId)
                val rounds = extractor.readSampleData(inputBuffer!!, 0)

                if (rounds > 0) {
                    val currentT = extractor.sampleTime
                    codec.queueInputBuffer(inputBufferId, 0, rounds, 0, 0)
                    extractor.advance()
                } else {
                    // EOS
                    codec.queueInputBuffer(
                        inputBufferId,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                outputBufferId: Int,
                info: MediaCodec.BufferInfo
            ) {
                val buffer = codec.getOutputBuffer(outputBufferId)!!
                // Convert the video frame to a bitmap
                val videoBitmap = buffer.toBitmap(width, height)

                // Draw the translucent red overlay bitmap on top of the video frame bitmap
//                val canvas = Canvas(videoBitmap)
//                val paint = Paint()
//                paint.colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
//                paint.alpha = 128 // Set transparency level (0-255), adjust as needed
//                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                surface?.lockCanvas(null)?.apply {
                    // Clear canvas if necessary
                    drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                    // Draw the combined frame
                    drawBitmap(videoBitmap, 0f, 0f, null)
                    // Unlock the canvas
                    surface.unlockCanvasAndPost(this)
                }

                codec.releaseOutputBuffer(outputBufferId, true)
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
            }

            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
            }

        })
        decoder?.configure(format, null, null, 0)
        decoder?.start()
        "output format..${decoder!!.outputFormat}".rlog()

//        val inputBuffers = decoder!!.inputBuffers
//        var isEOS = false
//        val timeoutUs = 10000L
//
//        while (!isEOS) {
//            val inIndex = decoder!!.dequeueInputBuffer(timeoutUs)
////            "in index...${inIndex}".rlog()
//            if (inIndex >= 0) {
//                val buffer = inputBuffers[inIndex]
//                val sampleSize = extractor.readSampleData(buffer, 0)
//                if (sampleSize < 0) {
//                    decoder!!.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
//                    isEOS = true
//                } else {
//                    decoder!!.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
//                    extractor.advance()
//                }
//            }
//            val outIndex = decoder!!.dequeueOutputBuffer(info, timeoutUs)
//            "out index...${outIndex}".rlog()
//            when (outIndex) {
//                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> decoder!!.outputBuffers
//                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
//                    // Subsequent data will conform to new format
//                }
//                MediaCodec.INFO_TRY_AGAIN_LATER -> {
//                    // No output available yet
//                }
//                else -> {
//                    val buffer = decoder!!.outputBuffers[outIndex]
//                    // Use buffer content here if needed
//                    decoder!!.releaseOutputBuffer(outIndex, true)
////                    val buffer = decoder!!.getOutputBuffer(outIndex)!!
////                    // Convert the video frame to a bitmap
////                    val videoBitmap = buffer.toBitmap(width, height)
////
////                    // Draw the translucent red overlay bitmap on top of the video frame bitmap
////                    val canvas = Canvas(videoBitmap)
////                    val paint = Paint()
////                    paint.colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
////                    paint.alpha = 128 // Set transparency level (0-255), adjust as needed
////                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
////
////                    surface?.lockCanvas(null)?.apply {
////                        // Clear canvas if necessary
////                        drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
////                        // Draw the combined frame
////                        drawBitmap(videoBitmap, 0f, 0f, null)
////                        // Unlock the canvas
////                        surface.unlockCanvasAndPost(this)
////                    }
////
////                    decoder!!.releaseOutputBuffer(outIndex, true)
//                }
//            }
//        }

    }

    fun ByteBuffer.toBitmap(width: Int, height: Int): Bitmap {
        // Allocate an array to hold the RGBA values for each pixel
        val argbArray = IntArray(width * height)

        // Rewind the buffer
        rewind()

        // Iterate through each pixel in the buffer
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Calculate the index of the YUV components for the current pixel
                val yIndex = y * width + x
                val uIndex = width * height + (y / 2) * width + (x / 2) * 2
                val vIndex = uIndex + 1

                // Extract YUV components
                val yValue = get(yIndex).toInt() and 0xFF
                val uValue = get(uIndex).toInt() and 0xFF
                val vValue = get(vIndex).toInt() and 0xFF

                // Perform YUV to RGB conversion
                val r = yValue + 1.402 * (vValue - 128)
                val g = yValue - 0.344 * (uValue - 128) - 0.714 * (vValue - 128)
                val b = yValue + 1.772 * (uValue - 128)

                // Clamp RGB values to the range [0, 255]
                val red = r.coerceIn(0.0, 255.0).toInt()
                val green = g.coerceIn(0.0, 255.0).toInt()
                val blue = b.coerceIn(0.0, 255.0).toInt()

                // Combine RGB values into a single pixel
                argbArray[y * width + x] = Color.rgb(red, green, blue)
            }
        }

        // Create a bitmap from the ARGB array
        return Bitmap.createBitmap(argbArray, width, height, Bitmap.Config.ARGB_8888)
    }

//    fun ByteBuffer.toBitmap(width: Int, height: Int): Bitmap {
//        val arr = ByteArray(this.remaining())
//        this.get(arr)
//        "byte array size..${arr}".rlog()
////        val argbArray = IntArray(width * height)
////        rewind()
////
////        // Calculate the size of each plane (Y, U, V)
////        val ySize = width * height
////        val uvSize = ySize / 4
////
////        // Check if there's enough data in the buffer
////        if (remaining() < ySize + 2 * uvSize) {
////            throw IllegalArgumentException("Not enough data in ByteBuffer to create a bitmap")
////        }
////
////        // YUV planar format: YYYYYYYY UU VV
////        // Iterate over each pixel and convert YUV to RGB
////        for (i in 0 until height) {
////            for (j in 0 until width) {
////                val yIndex = i * width + j
////                val uvIndex = ySize + (i / 2) * width + (j / 2) * 2
////
////                val y = get().toInt() and 0xFF
////                val u = get(uvIndex).toInt() and 0xFF
////                val v = get(uvIndex + 1).toInt() and 0xFF
////
////                // YUV to RGB conversion
////                var r = y + (1.370705 * (v - 128)).toInt()
////                var g = y - (0.698001 * (v - 128) + 0.337633 * (u - 128)).toInt()
////                var b = y + (1.732446 * (u - 128)).toInt()
////
////                // Clamp RGB values to 0-255 range
////                r = r.coerceIn(0, 255)
////                g = g.coerceIn(0, 255)
////                b = b.coerceIn(0, 255)
////
////                // Combine RGB values into a single pixel
////                argbArray[yIndex] = Color.rgb(r, g, b)
////            }
////        }
////
////        // Create a bitmap from the ARGB array
//        return BitmapFactory.decodeByteArray(arr, width, height)
//    }

    private fun applyGrayscaleFilter(buffer: ByteBuffer) {
        // Get the YUV data from the input buffer
        val yuvBytes = ByteArray(buffer.remaining())
        buffer.get(yuvBytes)
        "yuv bytes size...${yuvBytes.size}".rlog()

        // Initialize variables for YUV components
        var y: Int
        var u: Int
        var v: Int

        // Loop through each pixel in YUV format
        for (i in 0 until yuvBytes.size step 3) {
            // Get YUV components for the current pixel
            y = yuvBytes[i].toInt() and 0xFF
            u = yuvBytes[i + 1].toInt() and 0xFF
            v = yuvBytes[i + 2].toInt() and 0xFF

            // Convert YUV to RGB
            val r = (1.164 * (y - 16) + 1.596 * (v - 128)).toInt()
            val g = (1.164 * (y - 16) - 0.813 * (v - 128) - 0.391 * (u - 128)).toInt()
            val b = (1.164 * (y - 16) + 2.018 * (u - 128)).toInt()

            // Apply grayscale filter to RGB values
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            // Clamp the values to ensure they are within the valid range [0, 255]
            val grayPixel = gray.coerceIn(0, 255)

            // Convert the grayscale value back to YUV format
            yuvBytes[i] = grayPixel.toByte()
            yuvBytes[i + 1] = ((128 + 0.5 * (b - 128) + 0.418688 * (r - 128) + 0.081312 * (g - 128)).toInt()).toByte()
            yuvBytes[i + 2] = ((128 + 0.5 * (r - 128) - 0.168736 * (g - 128) - 0.331264 * (b - 128)).toInt()).toByte()
        }

        // Update the original buffer with the modified YUV values
        buffer.clear()
        buffer.put(yuvBytes)
    }

    private fun selectTrack(extractor: MediaExtractor): MediaFormat {
        "extractor track count...${extractor.trackCount}".rlog()
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            "extractor mime type...${mime}".rlog()
            if (mime?.startsWith("video/") == true) {
                extractor.selectTrack(i)
                return format
            }
        }
        throw RuntimeException("No video track found in the file")
    }

    fun releaseDecoder() {
        decoder?.stop()
        decoder?.release()
    }
}