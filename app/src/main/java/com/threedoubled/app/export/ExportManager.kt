package com.threedoubled.app.export

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * Coordinates writing exported artifacts (PNG / GLB / MP4 / GIF) to app-
 * external storage so the system Gallery/media scanner can index them.
 * Returns the output [Uri] for sharing / saving.
 */
class ExportManager(private val context: Context) {

    private val exportDir: File
        get() {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            return File(base, "3DoubleD")
        }

    /** Ensures the export directory exists and returns it. */
    fun ensureDir(): File {
        exportDir.mkdirs()
        return exportDir
    }

    fun pngPath(name: String = "avatar_${System.currentTimeMillis()}.png"): String =
        File(ensureDir(), name).absolutePath

    fun glbPath(name: String = "avatar_${System.currentTimeMillis()}.glb"): String =
        File(ensureDir(), name).absolutePath

    fun mp4Path(name: String = "render_${System.currentTimeMillis()}.mp4"): String =
        File(ensureDir(), name).absolutePath

    fun gifPath(name: String = "anim_${System.currentTimeMillis()}.gif"): String =
        File(ensureDir(), name).absolutePath

    /** Builds a content [Uri] for a file under our export dir (for sharing). */
    fun contentUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
