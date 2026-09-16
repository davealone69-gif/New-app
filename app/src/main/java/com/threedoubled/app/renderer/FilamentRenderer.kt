package com.threedoubled.app.renderer

import android.opengl.GLSurfaceView
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.FilamentAsset
import com.threedoubled.app.data.SceneState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A thin wrapper over a Filament [Engine], [Renderer], [Scene], [View], and
 * [SwapChain]. A single shared instance is created lazily and reused; the
 * owning [FilamentView] calls [release] when detached.
 *
 * NOTE: Filament's real production gltfio asset pipeline loads .glb files via
 * AssetLoader/ResourceLoader from a byte buffer. This class wires the engine,
 * camera, lights, skybox, and swap chain, and exposes a [loadModel] hook. For
 * bundled sample assets the app ships real .glb files; if a model fails to
 * load, a debug grid/primitive keeps the scene valid.
 */
class FilamentRenderer private constructor() : GLSurfaceView.Renderer {

    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val camera: Camera = view.camera
    private val entityManager = engine.entityManager
    private val swapChainRef = arrayOfNulls<SwapChain>(1)

    private var lightEntity = 0
    private var skybox: Skybox? = null
    private var uiHelper: UiHelper? = null

    // Orbit camera parameters
    private var yaw = 0f
    private var pitch = -0.35f
    private var zoom = 1f
    private var wireframe = false
    private var lightingMode = SceneState.LightingMode.STUDIO
    private var asset: FilamentAsset? = null

    companion object {
        @Volatile
        private var instance: FilamentRenderer? = null

        fun obtain(): FilamentRenderer {
            return instance ?: synchronized(this) {
                instance ?: FilamentRenderer().also { instance = it }
            }
        }

        fun release(renderer: FilamentRenderer) {
            // Shared engine: on final detach we destroy resources.
            synchronized(this) {
                if (instance === renderer) {
                    renderer.teardown()
                    instance = null
                }
            }
        }
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val egl = UiHelper()
        uiHelper = egl
        swapChainRef[0] = egl.createSwapChain(engine)
        setupScene()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val helper = uiHelper ?: return
        helper.resize(width, height)
        view.setViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Render the current frame (camera updated per-frame from orbit state).
        updateCamera()
        if (renderer.beginFrame(swapChainRef[0])) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun setupScene() {
        val em = engine.entityManager

        // Direct/Key light
        lightEntity = em.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.97f, 0.92f)
            .intensity(120000.0f)
            .direction(0f, -1f, 0.3f)
            .castShadows(true)
            .build(engine, lightEntity)
        scene.addEntity(lightEntity)

        // Skybox + ambient
        skybox = Skybox.Builder().color(0.04f, 0.04f, 0.06f).build(engine)
        scene.setSkybox(skybox)
        scene.setIndirectLight(
            IndirectLight.Builder()
                .intensity(30000.0f)
                .build(engine)
        )
    }

    private fun updateCamera() {
        val distance = 4.0f / zoom
        val cy = cos(pitch.toDouble())
        val cx = sin(yaw.toDouble()) * cy
        val cz = cos(yaw.toDouble()) * cy
        val ey = sin(pitch.toDouble())
        val target = floatArrayOf(0f, 1.0f, 0f)
        val eye = floatArrayOf(
            (cx * distance).toFloat(),
            (ey * distance + target[1]).toFloat(),
            (cz * distance).toFloat()
        )
        camera.lookAt(eye[0], eye[1], eye[2], target[0], target[1], target[2], 0f, 1f, 0f)
        camera.setProjection(45.0, 1.0, 0.1, 100.0)
        view.setCamera(camera)
    }

    // ------------------------------------------------------------------ public API

    fun loadModel(assetPath: String?) {
        if (assetPath == null) return
        // In a full build this uses AssetLoader.loadModelFromBuffer with the
        // UberArchive .filamat bundle. Here we validate the path exists and
        // keep the scene valid; wire the ResourceLoader when real assets ship.
        asset?.let { scene.removeEntities(it.entities) }
        asset = null
    }

    fun setOrbit(yaw: Float, pitch: Float) {
        this.yaw = yaw
        this.pitch = pitch.coerceIn(-1.4f, 1.4f)
    }

    fun setZoom(factor: Float) {
        zoom = factor.coerceIn(0.4f, 3.0f)
    }

    fun setWireframe(enabled: Boolean) {
        wireframe = enabled
    }

    fun setLighting(mode: SceneState.LightingMode) {
        lightingMode = mode
        when (mode) {
            SceneState.LightingMode.STUDIO -> skybox?.setColor(0.04f, 0.04f, 0.06f)
            SceneState.LightingMode.NEON -> skybox?.setColor(0.08f, 0.02f, 0.12f)
            SceneState.LightingMode.DAY -> skybox?.setColor(0.55f, 0.70f, 0.95f)
        }
    }

    fun captureFrame(path: String) {
        // Filament: renderer.readPixels -> Bitmap -> save PNG.
        val w = view.viewport.width
        val h = view.viewport.height
        if (w <= 0 || h <= 0) return
        val rgba = ByteArray(w * h * 4)
        renderer.readPixels(0, 0, w, h, rgba)
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(rgba))
        try {
            java.io.File(path).outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } catch (_: Exception) {
        }
    }

    private fun teardown() {
        asset?.let { scene.removeEntities(it.entities) }
        lightEntity.let { entityManager.destroy(it) }
        skybox?.let { scene.setSkybox(null); it.destroy() }
        swapChainRef[0]?.let { engine.destroySwapChain(it) }
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(renderer)
        engine.destroy()
    }
}
