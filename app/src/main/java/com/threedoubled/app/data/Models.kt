package com.threedoubled.app.data

/**
 * Domain model for a 3D avatar / scene.
 * Kept UI-agnostic so both the Compose layer and the Filament renderer can use it.
 */
data class AvatarModel(
    val id: String,
    val name: String,
    val category: Category,
    val assetPath: String? = null,
    val morphTargets: Map<String, Float> = emptyMap(),
    val skinTone: Int = 0,
    val materials: Map<String, MaterialParams> = emptyMap()
) {
    enum class Category { FEMALE, MALE, CYBORG, OTHER }
}

data class MaterialParams(
    val baseColor: Long = 0xFFFFFFFF,
    val metallic: Float = 0.0f,
    val roughness: Float = 0.6f,
    val emissiveColor: Long = 0xFF000000
)

data class SceneState(
    val rotationY: Float = 0f,
    val rotationX: Float = 0f,
    val zoom: Float = 1f,
    val wireframe: Boolean = false,
    val lightingMode: LightingMode = LightingMode.STUDIO
) {
    enum class LightingMode { STUDIO, NEON, DAY }
}
