package com.threedoubled.app.export

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal animated GIF89a encoder. Frames are quantised to a fixed 3-3-2
 * (256-colour) palette, then LZW-compressed per the GIF spec.
 */
class GifEncoder(
    private val outputPath: String,
    private val frameDelayCentis: Int = 4
) {

    fun encode(frames: List<Bitmap>) {
        if (frames.isEmpty()) return
        val width = frames[0].width
        val height = frames[0].height

        FileOutputStream(outputPath).use { out ->
            out.write("GIF89a".toByteArray(Charsets.US_ASCII))
            writeShort(out, width)
            writeShort(out, height)
            out.write(0xF7) // global color table, 256 entries
            out.write(0)
            out.write(0)
            writeGlobalColorTable(out)

            // Netscape looping extension
            out.write(0x21); out.write(0xFF); out.write(0x0B)
            out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
            out.write(0x03); out.write(0x01); writeShort(out, 0); out.write(0x00)

            for (frame in frames) {
                val indexed = indexFrame(frame, width, height)
                // Graphic control extension
                out.write(0x21); out.write(0xF9); out.write(0x04)
                out.write(0x04) // disposal = 1, no transparency
                writeShort(out, frameDelayCentis)
                out.write(0x00)
                out.write(0x00)
                // Image descriptor
                out.write(0x2C)
                writeShort(out, 0); writeShort(out, 0)
                writeShort(out, width); writeShort(out, height)
                out.write(0x00)
                // LZW data
                out.write(0x08) // min code size
                writeLzw(out, indexed)
                out.write(0x00) // block terminator
            }
            out.write(0x3B) // trailer
        }
    }

    private fun writeShort(out: FileOutputStream, value: Int) {
        out.write(value and 0xff)
        out.write((value shr 8) and 0xff)
    }

    private fun writeGlobalColorTable(out: FileOutputStream) {
        for (i in 0 until 256) {
            val r3 = (i shr 5) and 0x07
            val g3 = (i shr 2) and 0x07
            val b2 = i and 0x03
            out.write(r3 * 255 / 7)
            out.write(g3 * 255 / 7)
            out.write(b2 * 255 / 3)
        }
    }

    private fun indexFrame(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val out = ByteArray(width * height)
        for (i in argb.indices) {
            val c = argb[i]
            val r = ((c shr 16) and 0xff) * 7 / 255
            val g = ((c shr 8) and 0xff) * 7 / 255
            val b = (c and 0xff) * 3 / 255
            out[i] = ((r shl 5) or (g shl 2) or b).toByte()
        }
        return out
    }

    private fun writeLzw(out: FileOutputStream, pixels: ByteArray) {
        val minCodeSize = 8
        val clearCode = 1 shl minCodeSize        // 256
        val endCode = clearCode + 1              // 257

        var codeSize = minCodeSize + 1
        var nextCode = endCode + 1
        val dictionary = HashMap<String, Int>()

        val block = ByteArrayOutputStream()
        var bitValue = 0
        var bitCount = 0

        fun flushBytes() {
            while (bitCount >= 8) {
                block.write(bitValue and 0xff)
                bitValue = bitValue shr 8
                bitCount -= 8
            }
        }

        fun emitBlockIfFull() {
            // GIF sub-blocks are at most 255 bytes each.
            while (block.size() >= 255) {
                val bytes = block.toByteArray()
                out.write(255)
                out.write(bytes, 0, 255)
                block.reset()
                if (bytes.size > 255) block.write(bytes, 255, bytes.size - 255)
            }
        }

        fun writeCode(code: Int) {
            bitValue = bitValue or (code shl bitCount)
            bitCount += codeSize
            flushBytes()
            emitBlockIfFull()
        }

        writeCode(clearCode)

        var prefix = "" + pixels[0].toInt().toChar()
        for (i in 1 until pixels.size) {
            val k = pixels[i].toInt().toChar()
            val joined = prefix + k
            if (dictionary.containsKey(joined)) {
                prefix = joined
            } else {
                writeCode(dictionary[prefix] ?: (prefix[0].code))
                dictionary[joined] = nextCode
                nextCode++
                if (nextCode == (1 shl codeSize)) {
                    if (codeSize < 12) {
                        codeSize++
                    } else {
                        writeCode(clearCode)
                        dictionary.clear()
                        codeSize = minCodeSize + 1
                        nextCode = endCode + 1
                    }
                }
                prefix = "" + k
            }
        }
        writeCode(dictionary[prefix] ?: (prefix[0].code))
        writeCode(endCode)

        // Flush remaining bits
        if (bitCount > 0) {
            block.write(bitValue and 0xff)
        }
        if (block.size() > 0) {
            val bytes = block.toByteArray()
            out.write(bytes.size)
            out.write(bytes)
        }
    }
}
