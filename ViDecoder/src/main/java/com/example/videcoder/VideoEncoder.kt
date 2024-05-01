package com.example.videcoder

import android.media.*
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer


private const val MIME_TYPE = "video/avc"
private const val FRAME_RATE = 30
private const val BIT_RATE = 2000000 // Adjust as needed
private const val I_FRAME_INTERVAL = 5

class VideoEncoder(val outputfile:File) {

    var contentEncoder: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var muxerStarted = false
    private var trackIndex = -1
    var bufferInfo: MediaCodec.BufferInfo? = null



    @RequiresApi(Build.VERSION_CODES.Q)
    fun encode(width: Int, height: Int, frames: MutableList<OutputBufferData>, mediaFormat: MediaFormat){
        val codecInfo: MediaCodecInfo = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.find { it.supportedTypes.contains(MIMETYPE_VIDEO_AVC) }!!
        val format = MediaFormat.createVideoFormat(
            MIMETYPE_VIDEO_AVC,
            width,
            height
        )
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 7000000)
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30)
        try {
            contentEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
            "encoder name...${contentEncoder?.name}".rlog()
        } catch (e: IOException) {
            "error creating encoder".rlog()
            throw RuntimeException(e)
        }
        contentEncoder?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        contentEncoder?.start()
        "start media muxer...".rlog()
        startMuxer()

        for (data in frames){
            val bufferIndex = contentEncoder?.dequeueInputBuffer(10000)
            if (bufferIndex != null) {
                if (bufferIndex >= 0) {
                    val inputBuffer = contentEncoder?.getInputBuffer(bufferIndex)
                    inputBuffer?.put(data.buffer)
                    "encode input buffer..${inputBuffer?.capacity()}".rlog()
                    contentEncoder?.queueInputBuffer(bufferIndex, 0, data.buffer.capacity(), data.info.presentationTimeUs, 0)
                }
            }
            val outputBufferId = contentEncoder?.dequeueOutputBuffer(data.info,10000);
            if (outputBufferId != null) {
                if (outputBufferId >= 0) {
                    val outputBuffer = contentEncoder?.getOutputBuffer(outputBufferId);
                    val  bufferFormat = contentEncoder?.getOutputFormat(outputBufferId);
                    "output buffer format...${outputBuffer?.capacity()}...${bufferFormat}...${bufferFormat?.keys?.joinToString { "$it , " }}".rlog()
                    if (outputBuffer != null) {
                        mediaMuxer?.writeSampleData(trackIndex, outputBuffer, data.info)
                    }
                    contentEncoder?.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }
        contentEncoder?.stop()
        contentEncoder?.release()

    }

    private fun startMuxer() {
        mediaMuxer = MediaMuxer(outputfile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val format = contentEncoder!!.outputFormat
        "encode output format...${format}".rlog()
        trackIndex = mediaMuxer?.addTrack(format)!!
        "track index...${trackIndex}".rlog()
        mediaMuxer?.start()
    }

    fun releaseEncoder() {
        contentEncoder?.stop()
        contentEncoder?.release()
    }

}