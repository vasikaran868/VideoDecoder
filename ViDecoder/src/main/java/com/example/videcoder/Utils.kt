package com.example.videcoder

import android.graphics.*
import android.media.Image
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import androidx.core.math.MathUtils.clamp
import androidx.core.util.Preconditions.checkArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

fun String.rlog(){
    Log.v("Debug Tag", this)
}

fun ByteBuffer.toBitmap(width: Int, height: Int): Bitmap {
    val argbArray = IntArray(width * height)
    rewind()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val yIndex = y * width + x
            val uIndex = width * height + (y / 2) * width + (x / 2) * 2
            val vIndex = uIndex + 1
            val yValue = get(yIndex).toInt() and 0xFF
            val uValue = get(uIndex).toInt() and 0xFF
            val vValue = get(vIndex).toInt() and 0xFF
            val r = yValue + 1.402 * (vValue - 128)
            val g = yValue - 0.344 * (uValue - 128) - 0.714 * (vValue - 128)
            val b = yValue + 1.772 * (uValue - 128)
            val red = r.coerceIn(0.0, 255.0).toInt()
            val green = g.coerceIn(0.0, 255.0).toInt()
            val blue = b.coerceIn(0.0, 255.0).toInt()
            argbArray[y * width + x] = Color.rgb(red, green, blue)
        }
    }
    return Bitmap.createBitmap(argbArray, width, height, Bitmap.Config.ARGB_8888)
}

private fun applyGrayscaleFilter(buff: ByteBuffer) {
    buff.position(0)
    val yuvBytes = ByteArray(buff.remaining())
    buff.get(yuvBytes)
    for (i in 0 until yuvBytes.size / 3) {
        var y = yuvBytes[i * 3].toInt() and 0xFF
        var u = yuvBytes[i * 3 + 1].toInt() and 0xFF
        var v = yuvBytes[i * 3 + 2].toInt() and 0xFF
        val gray = (0.299 * y + 0.587 * u + 0.114 * v).toInt().coerceIn(0, 255)
        yuvBytes[i * 3] = gray.toByte()
        yuvBytes[i * 3 + 1] = gray.toByte()
        yuvBytes[i * 3 + 2] = gray.toByte()
    }

    buff.clear()
    buff.put(yuvBytes)
}

//fun displayFramesOnSurface(frames: List<Pair<MediaCodec.BufferInfo,ByteBuffer>>, width: Int, height: Int, surface: Surface) {
//    // Create a MediaCodec instance
//    val codec = MediaCodec.createDecoderByType("video/avc")
//
//    // Configure the codec with the desired format and surface
//    val format = MediaFormat.createVideoFormat("video/avc", width, height)
//    var index = 0
//    "buffer list...${frames}".rlog()
//    GlobalScope.launch(Dispatchers.IO) {
//        frames.forEach {
//            val buffer = it.second
//            // Render the output buffer onto the surface
//            val b = ByteBuffer.allocate(buffer.capacity())
//            b.put(buffer)
//            //                applyGrayscaleFilter(b)
//            "2222  frame info...${buffer.capacity()}...${info.flags}...size...${info.size}...offset...${info.offset}...${info.presentationTimeUs}".rlog()
//
//            val videoBitmap = Bitmap.createBitmap(buffer.asIntBuffer().array(), width, height, Bitmap.Config.ARGB_8888)
//            surface?.lockCanvas(null)?.apply {
//                // Clear canvas if necessary
//                drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
//                // Draw the combined frame
//                drawBitmap(videoBitmap, null, RectF(0f,0f, this.width.toFloat(), this.height.toFloat()), null)
//                // Unlock the canvas
//                surface.unlockCanvasAndPost(this)
//            }
//            delay(30)
//        }
//    }
//}

fun Image.imageToBitmap(): Bitmap? {
    // Get the image dimensions
    val width = width
    val height = height

    // Create a byte buffer to hold the image data
    val planes = planes
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    // Create a Bitmap from the byte array using BitmapFactory
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

////                 Convert the video frame to a bitmap
//                val b = ByteBuffer.allocate(buffer.capacity())
//                b.put(buffer)
////                applyGrayscaleFilter(b)
//                val videoBitmap = b.toBitmap(width, height)
//                "frame info...${buffer.capacity()}...${info.flags}...size...${info.size}...offset...${info.offset}...${info.presentationTimeUs}".rlog()
//                surface?.lockCanvas(null)?.apply {
//                    // Clear canvas if necessary
//                    drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
//                    // Draw the combined frame
//                    drawBitmap(videoBitmap, null, RectF(0f,0f, this.width.toFloat(), this.height.toFloat()), null)
//                    // Unlock the canvas
//                    surface.unlockCanvasAndPost(this)
//                }


fun yuv420ToBitmap(image: Image): Bitmap? {
//    checkArgument(
//        image.format == ImageFormat.YUV_420_888,
//        "Only YUV_420_888 image format supported."
//    )
    val imageWidth = image.width
    val imageHeight = image.height
    // ARGB array needed by Bitmap static factory method I use below.
    val argbArray = IntArray(imageWidth * imageHeight)
    val yBuffer = image.planes[0].buffer
    yBuffer.position(0)

    // A YUV Image could be implemented with planar or semi planar layout.
    // A planar YUV image would have following structure:
    // YYYYYYYYYYYYYYYY
    // ................
    // UUUUUUUU
    // ........
    // VVVVVVVV
    // ........
    //
    // While a semi-planar YUV image would have layout like this:
    // YYYYYYYYYYYYYYYY
    // ................
    // UVUVUVUVUVUVUVUV   <-- Interleaved UV channel
    // ................
    // This is defined by row stride and pixel strides in the planes of the
    // image.

    // Plane 1 is always U & plane 2 is always V
    // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
    val uBuffer = image.planes[1].buffer
    uBuffer.position(0)
    val vBuffer = image.planes[2].buffer
    vBuffer.position(0)

    // The U/V planes are guaranteed to have the same row stride and pixel
    // stride.
    val uvRowStride = image.planes[1].rowStride
    val uvPixelStride = image.planes[1].pixelStride
    var r: Int
    var g: Int
    var b: Int
//    var yValue: Int
    var uValue: Int
    var vValue: Int
    for (y in 0 until imageHeight - 2) {
        for (x in 0 until imageWidth - 2) {
            val yIndex = y * imageWidth + x
            // Y plane should have positive values belonging to [0...255]
            val yValue = yBuffer[yIndex].toInt() and 0xff
            val uvx = x / 2
            val uvy = y / 2
            // U/V Values are subsampled i.e. each pixel in U/V chanel in a
            // YUV_420 image act as chroma value for 4 neighbouring pixels
            val uvIndex = uvy * uvRowStride + uvx * uvPixelStride

            // U/V values ideally fall under [-0.5, 0.5] range. To fit them into
            // [0, 255] range they are scaled up and centered to 128.
            // Operation below brings U/V values to [-128, 127].
            uValue = (uBuffer[uvIndex].toInt() and 0xff) - 128
            vValue = (vBuffer[uvIndex].toInt() and 0xff) - 128

            // Compute RGB values per formula above.
            r = (yValue + 1.370705f * vValue).toInt()
            g = (yValue - 0.698001f * vValue - 0.337633f * uValue).toInt()
            b = (yValue + 1.732446f * uValue).toInt()
            r = clamp(r, 0, 255)
            g = clamp(g, 0, 255)
            b = clamp(b, 0, 255)
            // Use 255 for alpha value, no transparency. ARGB values are
            // positioned in each byte of a single 4 byte integer
            // [AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB]
            argbArray[yIndex] = 255 shl 24 or (r and 255 shl 16) or (g and 255 shl 8) or (b and 255)
        }
    }
    return Bitmap.createBitmap(
        argbArray, imageWidth, imageHeight, Bitmap.Config.ARGB_8888
    )
}

fun applyMask(contentBitmap: Bitmap, maskingBitmap: Bitmap): Bitmap {
    "content bitmap...${contentBitmap.width}*${contentBitmap.height}....mask bitmap...${maskingBitmap.width}*${maskingBitmap.height}".rlog()
    val maskedBitmap = Bitmap.createBitmap(contentBitmap.width, contentBitmap.height, Bitmap.Config.ARGB_8888)
    for (y in 0 until contentBitmap.height) {
        for (x in 0 until contentBitmap.width) {
            val contentPixel = contentBitmap.getPixel(x, y)
            val amplifiedGray = (Color.red(maskingBitmap.getPixel(x, y)).toFloat() - 127.5f) * 1.2f + 127.5f
            val adjustedGray = amplifiedGray.toInt().coerceIn(0, 255)
            val maskedColor = Color.argb(Color.red(maskingBitmap.getPixel(x, y)), Color.red(contentPixel), Color.green(contentPixel), Color.blue(contentPixel))
            maskedBitmap.setPixel(x, y, maskedColor)
        }
    }

    return maskedBitmap
}



//        codec.setCallback( object : MediaCodec.Callback() {
//            override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
////                val inputBufferIndex = codec.dequeueInputBuffer(-1)
//                frames.getOrNull(index)?.let{ frame ->
//                    val inputBuffer = codec.getInputBuffer(inputBufferId)
//                    inputBuffer?.clear()
//                    inputBuffer?.put(frame.second)
//                    index++
//                    // Queue the input buffer
//                    codec.queueInputBuffer(inputBufferId, 0, frame.second.capacity(), frame.first.presentationTimeUs, frame.first.flags)
//                } ?: run {
//                    codec.queueInputBuffer(
//                        inputBufferId,
//                        0,
//                        0,
//                        0,
//                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
//                    )
//                }
////                if (inputBufferIndex >= 0) {
////                    // Get the input buffer and fill it with the frame data
////
////                }
//
//            }
//
//            override fun onOutputBufferAvailable(
//                codec: MediaCodec,
//                outputBufferId: Int,
//                info: MediaCodec.BufferInfo
//            ) {
//                val buffer = codec.getOutputBuffer(outputBufferId)!!
//                // Render the output buffer onto the surface
//                val b = ByteBuffer.allocate(buffer.capacity())
//                b.put(buffer)
//                //                applyGrayscaleFilter(b)
//                "2222  frame info...${buffer.capacity()}...${info.flags}...size...${info.size}...offset...${info.offset}...${info.presentationTimeUs}".rlog()
//
//                val videoBitmap = b.toBitmap(width, height)
//                surface?.lockCanvas(null)?.apply {
//                    // Clear canvas if necessary
//                    drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
//                    // Draw the combined frame
//                    drawBitmap(videoBitmap, null, RectF(0f,0f, this.width.toFloat(), this.height.toFloat()), null)
//                    // Unlock the canvas
//                    surface.unlockCanvasAndPost(this)
//                }
//                codec.releaseOutputBuffer(outputBufferId, true)
//            }
//
//            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
//            }
//
//            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
//
//            }
//
//        })


//        codec.configure(format, null, null, 0)
//        codec.start()
//        for (frame in frames) {
//            val inputBufferId = decoder?.dequeueInputBuffer(-1)
//            if (inputBufferId != null && inputBufferId >= 0) {
//                val inputBuffer = decoder?.getInputBuffer(inputBufferId)
//                inputBuffer?.clear()
//                inputBuffer?.put(frame.second)
//                decoder?.queueInputBuffer(inputBufferId, 0, frame.second.remaining(), 0, 0)
//            }
//
//            val outputBufferId = decoder?.dequeueOutputBuffer(frame.first, -1)
//            if (outputBufferId != null && outputBufferId >= 0) {
//                val outputBuffer = decoder?.getOutputBuffer(outputBufferId)
//                outputBuffer?.let {
//                    val canvas = surface.lockCanvas(null)
//                    canvas?.drawBitmap(it.toBitmap(width, height), null , RectF(0f,0f, width.toFloat(), height.toFloat()), null)
//                    surface.unlockCanvasAndPost(canvas)
//                }
//                decoder?.releaseOutputBuffer(outputBufferId, true)
//            }
//        }
