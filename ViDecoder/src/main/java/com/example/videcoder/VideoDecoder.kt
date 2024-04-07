package com.example.videcoder

import android.content.Context
import android.graphics.*
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class VideoDecoder(val context: Context) {

    private var contentDecoder: MediaCodec? = null
    private var maskDecoder: MediaCodec? = null
    private val info = MediaCodec.BufferInfo()
    val sharedFlow1 = MutableSharedFlow<FrameData>()
    val sharedFlow2 = MutableSharedFlow<FrameData>()

    val sharedFlow1Buffer = HashMap<Long, FrameData>()
    val sharedFlow2Buffer = HashMap<Long, FrameData>()

    val maskedFlow = MutableSharedFlow<FrameData>()

    init {
        GlobalScope.launch {
            sharedFlow1.collect { data ->
                "content flow data..${data.info.presentationTimeUs}".rlog()
                sharedFlow2Buffer[data.info.presentationTimeUs]?.let { mergedData ->
                    maskedFlow.emit(FrameData(applyMask(data.image, mergedData.image), data.info))
                    sharedFlow2Buffer.remove(data.info.presentationTimeUs)
                } ?: run {
                    sharedFlow1Buffer[data.info.presentationTimeUs] = data
                }
            }
        }
        GlobalScope.launch {
            sharedFlow2.collect { data ->
                "mask flow data..${data.info.presentationTimeUs}".rlog()
                sharedFlow1Buffer[data.info.presentationTimeUs]?.let { mergedData ->
                    maskedFlow.emit(FrameData(applyMask(mergedData.image, data.image), data.info))
                    sharedFlow1Buffer.remove(data.info.presentationTimeUs)
                } ?: run {
                    sharedFlow2Buffer[data.info.presentationTimeUs] = data
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun decode(uri: Uri, surface: Surface, coroutineScope: CoroutineScope){
        val contentDataExtractor = MediaExtractor()
        contentDataExtractor.setDataSource(context, uri, null)
        val contentFormat = selectTrack(contentDataExtractor)
        contentDecoder = contentFormat.getString(MediaFormat.KEY_MIME)
            ?.let { MediaCodec.createDecoderByType(it) }
        val width = contentFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = contentFormat.getInteger(MediaFormat.KEY_HEIGHT)
        contentDecoder?.setCallback( object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
                val inputBuffer = codec.getInputBuffer(inputBufferId)
                val sampleSize = contentDataExtractor.readSampleData(inputBuffer!!, 0)
                if (sampleSize > 0) {
                    val presentationTime = contentDataExtractor.sampleTime
                    codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTime, 0)
                    contentDataExtractor.advance()
                } else {
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
//                val buffer = codec.getOutputBuffer(outputBufferId)!!
//                "content frame info...${info.presentationTimeUs}".rlog()
                coroutineScope.launch {
                    "content output coroutine launched...${info.presentationTimeUs}".rlog()
                    codec.getOutputImage(outputBufferId)?.let {
                        "content output got image...${info.presentationTimeUs}".rlog()
                        yuv420ToBitmap(it)?.let { bMap ->
                            "content output got bitmap...${info.presentationTimeUs}".rlog()
                            maskedFlow.emit(
                                FrameData(
                                    bMap,
                                    info
                                )
                            )
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)

                }
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
            }

            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
            }

        })
        contentDecoder?.configure(contentFormat, null, null, 0)
        contentDecoder?.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun decodeMaskVideo(maskUri: Uri, surface: Surface, coroutineScope: CoroutineScope){
        val maskDataExtractor = MediaExtractor()
        maskDataExtractor.setDataSource(context, maskUri, null)
        val maskFormat = selectTrack(maskDataExtractor)
        maskDecoder = maskFormat.getString(MediaFormat.KEY_MIME)
            ?.let { MediaCodec.createDecoderByType(it) }
        maskDecoder?.setCallback( object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
                val inputBuffer = codec.getInputBuffer(inputBufferId)
                val sampleSize = maskDataExtractor.readSampleData(inputBuffer!!, 0)
                if (sampleSize > 0) {
                    val presentationTime = maskDataExtractor.sampleTime
                    codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTime, 0)
                    maskDataExtractor.advance()
                } else {
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
                "mask frame info...${info.presentationTimeUs}".rlog()
                coroutineScope.launch {
                    "mask output coroutine launched...${info.presentationTimeUs}".rlog()
                    codec.getOutputImage(outputBufferId)?.let {
                        "mask output got image...${info.presentationTimeUs}".rlog()
                        yuv420ToBitmap(it)?.let { bMap ->
                            "mask output got bitmap...${info.presentationTimeUs}".rlog()
                            maskedFlow.emit(
                                FrameData(
                                    bMap,
                                    info
                                )
                            )
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)
                }
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
            }

            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
            }

        })
        maskDecoder?.configure(maskFormat, null, null, 0)
        maskDecoder?.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun selectTrack(extractor: MediaExtractor): MediaFormat {
        "extractor track count...${extractor.trackCount}".rlog()
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            "track..$i...format...${format.keys}...${format.features}".rlog()
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                extractor.selectTrack(i)
                return format
            }
        }
        throw RuntimeException("No video track found in the file")
    }

    fun releaseDecoder() {
        contentDecoder?.stop()
        contentDecoder?.release()
    }
}

data class FrameData(
    val image: Bitmap,
    val info: MediaCodec.BufferInfo
)