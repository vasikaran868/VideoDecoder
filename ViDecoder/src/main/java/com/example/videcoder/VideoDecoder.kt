package com.example.videcoder

import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.example.videcoder.googleTestCode.ExtractDecodeEditEncodeMuxTest
import com.example.videcoder.googleTestCode.InputSurface
import com.example.videcoder.googleTestCode.OutputSurface
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

private const val MIME_TYPE = "video/avc"
class VideoDecoder(val context: Context, val outputfile: File) {

    private var contentDecoder: MediaCodec? = null
    private var maskDecoder: MediaCodec? = null
    private val info = MediaCodec.BufferInfo()
    val sharedFlow1 = MutableSharedFlow<abc>()
    val sharedFlow2 = MutableSharedFlow<def>()

    val sharedFlow1Buffer = HashMap<Long, abc>()
    val sharedFlow2Buffer = HashMap<Long, def>()

    val maskedFlow = MutableSharedFlow<FrameImageData>()

    var contentEncoder: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var trackIndex = -1

    init {
        GlobalScope.launch {
            sharedFlow1.collect { data ->
                "content flow data..${data.info.presentationTimeUs}".rlog()
                sharedFlow2Buffer[data.info.presentationTimeUs]?.let { mergedData ->
                    maskedFlow.emit(FrameImageData(data.imageData , mergedData.imageData , data.info))
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
                    maskedFlow.emit(FrameImageData(mergedData.imageData, data.imageData, mergedData.info))
                    sharedFlow1Buffer.remove(data.info.presentationTimeUs)
                } ?: run {
                    sharedFlow2Buffer[data.info.presentationTimeUs] = data
                }
            }
        }
    }

    fun trying(contentVideoUri: Uri){


        ExtractDecodeEditEncodeMuxTest().apply {
//                setSize(540,800)
//                setOutputFile(file)
//                setCopyVideo()
//            val videoCodecInfo =
//                ExtractDecodeEditEncodeMuxTest.selectCodec(
//                    "video/avc" // H.264 Advanced Video Coding
//                )
//            val outputVideoFormat = MediaFormat.createVideoFormat(
//                "video/avc" // H.264 Advanced Video Coding
//                ,
//                540,
//                800
//            )
//            // Set some properties. Failing to specify some of these can cause the MediaCodec
//            // configure() call to throw an unhelpful exception.
//            // Set some properties. Failing to specify some of these can cause the MediaCodec
//            // configure() call to throw an unhelpful exception.
//            outputVideoFormat.setInteger(
//                MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
//            )
//            outputVideoFormat.setInteger(
//                MediaFormat.KEY_BIT_RATE, 2000000 // 2Mbps
//
//            )
//            outputVideoFormat.setInteger(
//                MediaFormat.KEY_FRAME_RATE, 15 // 15fps
//
//            )
//            outputVideoFormat.setInteger(
//                MediaFormat.KEY_I_FRAME_INTERVAL, 10 // 10 seconds between I-frames
//
//            )
//            val inputSurfaceReference = AtomicReference<Surface>()
//            val videoEncoder = ExtractDecodeEditEncodeMuxTest.createVideoEncoder(
//                videoCodecInfo, outputVideoFormat, inputSurfaceReference
//            )
//            val inputSurface = InputSurface(inputSurfaceReference.get())
//            inputSurface.makeCurrent()
//            // Create a MediaCodec for the decoder, based on the extractor's format.
//            // Create a MediaCodec for the decoder, based on the extractor's format.
//            val outputSurface = OutputSurface()
//            outputSurface.changeFragmentShader(ExtractDecodeEditEncodeMuxTest.FRAGMENT_SHADER)
//            testExtractDecodeEditEncodeMuxQVGA(outputfile, context, contentVideoUri)
        }
    }

    val bufferList = mutableListOf<OutputBufferData>()

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun decode(uri: Uri, surface: Surface, coroutineScope: CoroutineScope, onComplete: (width: Int, height: Int, frames: MutableList<OutputBufferData>, mediaFormat: MediaFormat) -> Unit){
        val contentDataExtractor = MediaExtractor()
        contentDataExtractor.setDataSource(context, uri, null)
        val contentFormat = selectTrack(contentDataExtractor)
        showMediaFormat(contentFormat)
        try {
            contentEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
            "encoder name...${contentEncoder?.name}".rlog()
        } catch (e: IOException) {
            "error creating encoder".rlog()
            throw RuntimeException(e)
        }
        contentFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        contentFormat.setInteger(MediaFormat.KEY_BIT_RATE, 7000000)
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        contentFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30)
        contentEncoder?.setCallback( object : MediaCodec.Callback(){
            override fun onInputBufferAvailable(p0: MediaCodec, p1: Int) {
            }

            override fun onOutputBufferAvailable(
                eCodec: MediaCodec,
                bufferId: Int,
                eInfo: MediaCodec.BufferInfo
            ) {
                val outputBuffer = eCodec.getOutputBuffer(bufferId)
                "encode output buffer...${eInfo.size}".rlog()
                val ab = ByteBuffer.allocate(outputBuffer!!.capacity())
                ab.put(outputBuffer)
                if (outputBuffer != null) {
                    mediaMuxer?.writeSampleData(trackIndex, ab, info)
                }
//                eCodec.releaseOutputBuffer(bufferId, false)
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
                TODO("Not yet implemented")
            }

            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
                "output changed...${showMediaFormat(p1)}"
            }

        })
        contentEncoder?.configure(contentFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val mSurface = MediaCodec.createPersistentInputSurface()
        contentEncoder?.setInputSurface(mSurface)
        startMuxer()
        contentEncoder?.start()
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
                    "sample size...${sampleSize}...${presentationTime}...${inputBuffer.capacity()}".rlog()
                    codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTime, 0)
                    contentDataExtractor.advance()
                } else {
                    codec.queueInputBuffer(
                        inputBufferId,
                        0,
                        0,
                        -1,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
//                    onComplete(width, height, bufferList, contentFormat)
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                outputBufferId: Int,
                info: MediaCodec.BufferInfo
            ) {
                val buffer = codec.getOutputBuffer(outputBufferId)!!
                "content frame info...${info.presentationTimeUs}..${info.flags}...${info.size}".rlog()
                val b = ByteBuffer.allocate(buffer.capacity())
                b.put(buffer)
                codec.releaseOutputBuffer(outputBufferId, true)

//                if (info.flags != 4){
//                    bufferList.add(OutputBufferData(b, info))
//                    codec.releaseOutputBuffer(outputBufferId, false)
//                } else {
//                    codec.releaseOutputBuffer(outputBufferId, false)
//                    releaseDecoder()
//                    onComplete(width, height, bufferList, contentFormat)
//                }
//                coroutineScope.launch {
//                    "content output coroutine launched...${info.presentationTimeUs}".rlog()
//                    codec.getOutputImage(outputBufferId)?.let {
//                        "content output got image...${info.presentationTimeUs}".rlog()
//                        sharedFlow1.emit(
//                            abc(
//                                ContentImageData(
//                                    it.width,
//                                    it.height,
//                                    yBuffer = ByteBuffer.allocate(it.planes[0].buffer.capacity()).apply { put(it.planes[0].buffer) },
//                                    uBuffer = ByteBuffer.allocate(it.planes[1].buffer.capacity()).apply { put(it.planes[1].buffer) },
//                                    vBuffer = ByteBuffer.allocate(it.planes[2].buffer.capacity()).apply { put(it.planes[2].buffer) },
//                                    uvRowStride = it.planes[1].rowStride,
//                                    uvPixelStride = it.planes[1].pixelStride
//                                ),
//                                info
//                            )
//                        )
////                        yuv420ToBitmap(it)?.let { bMap ->
////                            "content output got bitmap...${info.presentationTimeUs}".rlog()
////                        }
//                    }
//                    codec.releaseOutputBuffer(outputBufferId, false)
//
//                }
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
            }

            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
            }

        })
        contentDecoder?.configure(contentFormat, mSurface, null, 0)
        "decode output format...${contentDecoder?.outputFormat?.let { showMediaFormat(it) }}".rlog()
        contentDecoder?.start()

//        while (true){
//            val outputBufferId = contentEncoder?.dequeueOutputBuffer(info,10000);
//            if (outputBufferId != null) {
//                if (outputBufferId >= 0) {
//                    val outputBuffer = contentEncoder?.getOutputBuffer(outputBufferId);
//                    val  bufferFormat = contentEncoder?.getOutputFormat(outputBufferId);
//                    "output buffer format...${outputBuffer?.capacity()}...${bufferFormat}...${bufferFormat?.keys?.joinToString { "$it , " }}".rlog()
//                    if (outputBuffer != null) {
//                        mediaMuxer?.writeSampleData(trackIndex, outputBuffer, info)
//                    }
//                    contentEncoder?.releaseOutputBuffer(outputBufferId, false);
//                }
//            }
//        }
    }

    private fun startMuxer() {
        mediaMuxer = MediaMuxer(outputfile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val format = contentEncoder!!.outputFormat
        "encode output format...${format}".rlog()
        trackIndex = mediaMuxer?.addTrack(format)!!
        "track index...${trackIndex}".rlog()
        mediaMuxer?.start()
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
//                val buffer = codec.getOutputBuffer(outputBufferId)!!
                "mask frame info...${info.presentationTimeUs}".rlog()
//                val b = ByteBuffer.allocate(buffer.capacity())
//                b.put(buffer)
                coroutineScope.launch {
                    "mask output coroutine launched...${info.presentationTimeUs}".rlog()
                    codec.getOutputImage(outputBufferId)?.let {
                        "mask output got image...${it.format}".rlog()
                        sharedFlow2.emit(
                            def(
                                MaskImageData(
                                    it.width,
                                    it.height,
                                    yBuffer = ByteBuffer.allocate(it.planes[0].buffer.capacity()).apply { put(it.planes[0].buffer) },
                                    uvRowStride = it.planes[1].rowStride,
                                    uvPixelStride = it.planes[1].pixelStride
                                ),
                                info
                            )
                        )
//                        yuv420ToBitmap(it)?.let { bMap ->
//                            "mask output got bitmap...${info.presentationTimeUs}".rlog()
//                        }
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

data class OutputBufferData(
    val buffer: ByteBuffer,
    val info: MediaCodec.BufferInfo

)

data class ImageData(
    val img: Image,
    val info: MediaCodec.BufferInfo
)

data class FrameImageData(
    val contentImageData: ContentImageData,
    val maskImageData: MaskImageData,
    val info: MediaCodec.BufferInfo
)

data class abc(
    val imageData: ContentImageData,
    val info: MediaCodec.BufferInfo
)

data class def(
    val imageData: MaskImageData,
    val info: MediaCodec.BufferInfo
)


data class ContentImageData(
    val imageWidth: Int,
    val imageHeight: Int,
    val uvRowStride: Int,
    val uvPixelStride: Int,
    val yBuffer: ByteBuffer,
    val uBuffer: ByteBuffer,
    val vBuffer: ByteBuffer
)

data class MaskImageData(
    val imageWidth: Int,
    val imageHeight: Int,
    val yBuffer: ByteBuffer,
    val uvRowStride: Int,
    val uvPixelStride: Int
)