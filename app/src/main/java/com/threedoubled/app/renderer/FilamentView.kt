package com.threedoubled.app.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.threedoubled.app.data.SceneState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Wraps the shared [FilamentRenderer] (Engine/Renderer/Scene/View/SwapChain)
 * behind a [GLSurfaceView] so Filament renders into the Compose app.
 *
 * The Filament engine is heavyweight and must be created/used from one thread,
 * so a single shared instance is managed by [FilamentRenderer].
 */
class FilamentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = FilamentRenderer.obtain()

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        preserveEGLContextOnPause = true
    }

    /** Loads a GLB/GLTF asset path into the scene and updates the renderer. */
    fun loadModel(assetPath: String?) {
        renderer.loadModel(assetPath)
    }

    fun setRotation(yaw: Float, pitch: Float) {
        renderer.setOrbit(yaw, pitch)
    }

    fun setZoom(factor: Float) {
        renderer.setZoom(factor)
    }

    fun setWireframe(enabled: Boolean) {
        renderer.setWireframe(enabled)
    }

    fun setLighting(mode: SceneState.LightingMode) {
        renderer.setLighting(mode)
    }

    /** Captures the current rendered frame as a PNG to the given path. */
    fun captureFrame(path: String) {
        renderer.captureFrame(path)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        FilamentRenderer.release(renderer)
    }
}
