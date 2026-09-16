package com.threedoubled.app.renderer

import android.view.SurfaceView
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.threedoubled.app.data.MaterialParams
import com.threedoubled.app.data.SceneState
import java.nio.ByteBuffer

/**
 * Wraps Filament's official [ModelViewer] (from `filament-utils-android`),
 * which owns the Engine / Scene / View / Renderer, camera manipulator, gesture
 * handling, IBL defaults and glTF loading.
 *
 * Responsibilities here: render loop, directional lighting presets, live PBR
 * material parameter application, and model load/reset.
 */
class SceneController(val surfaceView: SurfaceView) {

    val modelViewer = ModelViewer(surfaceView)
    private val renderLoop = RenderLoop { frameTimeNanos -> modelViewer.render(frameTimeNanos) }
    private var fillLight: Int = 0

    init {
        val view = modelViewer.view
        view.antiAliasing = View.AntiAliasing.FXAA
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply { enabled = true }
        view.bloomOptions = view.bloomOptions.apply { enabled = true }
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = true }

        setupFillLight()
        setLighting(SceneState.LightingMode.STUDIO)
    }

    private fun setupFillLight() {
        val engine = modelViewer.engine
        fillLight = engine.entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.62f, 0.72f, 1.0f)
            .intensity(25_000f)
            .direction(0.6f, -0.35f, 0.6f)
            .castShadows(false)
            .build(engine, fillLight)
        modelViewer.scene.addEntity(fillLight)
    }

    fun start() = renderLoop.start()
    fun stop() = renderLoop.stop()

    fun hasModel(): Boolean = modelViewer.asset != null

    fun loadModel(bytes: ByteBuffer) {
        modelViewer.destroyModel()
        modelViewer.loadModelGlb(bytes)
        modelViewer.transformToUnitCube()
    }

    fun resetCamera() {
        if (modelViewer.asset != null) modelViewer.resetToDefaultState()
    }

    /** Applies live PBR adjustments to every primitive of the loaded asset. */
    fun applyMaterial(params: MaterialParams) {
        val asset = modelViewer.asset ?: return
        val renderableManager = modelViewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!renderableManager.hasComponent(entity)) continue
            val instance = renderableManager.getInstance(entity)
            val primitives = renderableManager.getPrimitiveCount(instance)
            for (i in 0 until primitives) {
                val mi: MaterialInstance = renderableManager.getMaterialInstanceAt(instance, i)
                setFloat(mi, "metallicFactor", params.metallic)
                setFloat(mi, "roughnessFactor", params.roughness)
                setFloat3(mi, "emissiveFactor", params.glow, params.glow * 0.55f, params.glow * 0.18f)
            }
        }
    }

    fun setLighting(mode: SceneState.LightingMode) {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        scene.skybox?.let { engine.destroySkybox(it) }
        val skybox = when (mode) {
            SceneState.LightingMode.STUDIO -> Skybox.Builder().color(0.030f, 0.030f, 0.050f).build(engine)
            SceneState.LightingMode.NEON -> Skybox.Builder().color(0.070f, 0.020f, 0.130f).build(engine)
            SceneState.LightingMode.DAY -> Skybox.Builder().color(0.450f, 0.620f, 0.920f).build(engine)
        }
        scene.skybox = skybox

        val (keyIntensity, fillIntensity) = when (mode) {
            SceneState.LightingMode.STUDIO -> 110_000f to 25_000f
            SceneState.LightingMode.NEON -> 70_000f to 62_000f
            SceneState.LightingMode.DAY -> 160_000f to 80_000f
        }
        // ModelViewer exposes its key light as an entity id, not a manager.
        val lm = engine.lightManager
        if (lm != null) {
            if (lm.hasComponent(modelViewer.light)) {
                lm.setIntensity(lm.getInstance(modelViewer.light), keyIntensity)
            }
            if (lm.hasComponent(fillLight)) {
                lm.setIntensity(lm.getInstance(fillLight), fillIntensity)
            }
        }
    }

    fun destroy() {
        renderLoop.stop()
        try {
            modelViewer.destroyModel()
        } catch (_: Throwable) {
        }
        val engine = modelViewer.engine
        val lm = engine.lightManager
        if (lm != null && fillLight != 0 && lm.hasComponent(fillLight)) {
            engine.entityManager.destroy(fillLight)
        }
    }

    private fun setFloat(mi: MaterialInstance, name: String, value: Float) {
        try {
            mi.setParameter(name, value)
        } catch (_: Throwable) {
        }
    }

    private fun setFloat3(mi: MaterialInstance, name: String, a: Float, b: Float, c: Float) {
        try {
            mi.setParameter(name, floatArrayOf(a, b, c))
        } catch (_: Throwable) {
        }
    }
}
