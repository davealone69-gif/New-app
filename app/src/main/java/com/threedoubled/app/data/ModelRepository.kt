package com.threedoubled.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads built-in sample avatars and user-imported GLB/GLTF assets.
 * Built-in models are shipped as raw assets; imported files are copied into
 * the app's files dir so Filament's asset loader can read them from disk.
 */
class ModelRepository(private val context: Context) {

    private val sampleAvatars: List<AvatarModel> by lazy {
        listOf(
            AvatarModel(
                id = "sample_female", name = "Nova",
                category = AvatarModel.Category.FEMALE,
                assetPath = "models/female.glb"
            ),
            AvatarModel(
                id = "sample_male", name = "Kael",
                category = AvatarModel.Category.MALE,
                assetPath = "models/male.glb"
            ),
            AvatarModel(
                id = "sample_cyborg", name = "Unit-7",
                category = AvatarModel.Category.CYBORG,
                assetPath = "models/cyborg.glb"
            )
        )
    }

    /** Returns the list of available avatars (bundled + any imported copies). */
    suspend fun loadLibrary(): List<AvatarModel> = withContext(Dispatchers.IO) {
        val imported = listAvatarsInFilesDir()
        sampleAvatars + imported
    }

    private fun listAvatarsInFilesDir(): List<AvatarModel> {
        val dir = context.filesDir
        return (dir.listFiles { f -> f.extension in setOf("glb", "gltf") } ?: emptyArray())
            .map { f ->
                AvatarModel(
                    id = "import_${f.name}",
                    name = f.nameWithoutExtension,
                    category = AvatarModel.Category.OTHER,
                    assetPath = f.absolutePath
                )
            }
    }

    /** Resolves a model to an on-disk path consumable by Filament's ModelViewer. */
    fun resolveAssetPath(model: AvatarModel): String? {
        if (model.assetPath == null) return null
        // Built-in assets live in raw assets; copy to cache so it has a file path.
        val cached = context.cacheDir.resolve(model.assetPath)
        if (!cached.exists()) {
            val raw = context.assets.open(model.assetPath)
            cached.parentFile?.mkdirs()
            cached.outputStream().use { out -> raw.copyTo(out) }
        }
        return cached.absolutePath
    }
}
