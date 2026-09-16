package com.threedoubled.app.export

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.Image

/**
 * Encodes a list of ARGB [Bitmap] frames into an H.264 MP4 using MediaCodec's
 * flexible-YUV input [Image] path and MediaMuxer.
 */
class Mp4Encoder(
    private val outputPath: String,
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 24
) {

    fun encode(frames: List<Bitmap>) {
        if (frames.isEmpty()) return
        val w = width and 0xfffffffe
        val h = height and 0xfffffffe

        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, w, h).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        var framesQueued = 0
        val bufferInfo = MediaCodec.BufferInfo()
        val frameDurationUs = 1_000_000L / frameRate
        val frameSize = w * h * 3 / 2

        fun drain(endOfStream: Boolean) {
            while (true) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) return
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIndex >= 0 -> {
                        val buf = codec.getOutputBuffer(outIndex)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (buf != null && bufferInfo.size > 0 && muxerStarted) {
                            buf.position(bufferInfo.offset)
                            buf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, buf, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        try {
            for (bitmap in frames) {
                val inIndex = codec.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val image: Image? = codec.getInputImage(inIndex)
                    if (image != null) {
                        fillYuv(image, bitmap)
                        val pts = framesQueued * frameDurationUs
                        codec.queueInputBuffer(inIndex, 0, frameSize, pts, 0)
                        framesQueued++
                    } else {
                        codec.queueInputBuffer(inIndex, 0, 0, framesQueued * frameDurationUs, 0)
                    }
                }
                drain(false)
            }
            // End of stream
            val inIndex = codec.dequeueInputBuffer(10_000L)
            if (inIndex >= 0) {
                codec.queueInputBuffer(
                    inIndex, 0, 0,
                    framesQueued * frameDurationUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            drain(true)
        } finally {
            try {
                codec.stop()
            } catch (_: Throwable) {
            }
            codec.release()
            if (muxerStarted) {
                try {
                    muxer.stop()
                } catch (_: Throwable) {
                }
            }
            muxer.release()
        }
    }

    private fun fillYuv(image: Image, bitmap: Bitmap) {
        val w = image.width
        val h = image.height
        val scaled = if (bitmap.width != w || bitmap.height != h) {
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else {
            bitmap
        }
        val argb = IntArray(w * h)
        scaled.getPixels(argb, 0, w, 0, 0, w, h)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixStride = uPlane.pixelStride

        for (j in 0 until h) {
            for (i in 0 until w) {
                val c = argb[j * w + i]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff
                val y = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                yBuf.put(j * yRowStride + i * yPixStride, y.toByte())
            }
        }

        val uvHeight = h / 2
        val uvWidth = w / 2
        for (j in 0 until uvHeight) {
            for (i in 0 until uvWidth) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                for (dj in 0 until 2) {
                    for (di in 0 until 2) {
                        val c = argb[(j * 2 + dj) * w + (i * 2 + di)]
                        rSum += (c shr 16) and 0xff
                        gSum += (c shr 8) and 0xff
                        bSum += c and 0xff
                    }
                }
                val r = rSum / 4
                val g = gSum / 4
                val b = bSum / 4
                val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val index = j * uvRowStride + i * uvPixStride
                uBuf.put(index, u.toByte())
                vBuf.put(index, v.toByte())
            }
        }
    }
}
