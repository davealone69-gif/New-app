package com.threedoubled.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.threedoubled.app.data.SceneState
import com.threedoubled.app.renderer.FilamentView

/**
 * Hosts the [FilamentView] GLSurfaceView inside Compose, wires orbit/zoom
 * gestures to the renderer, and applies the scene state (wireframe, lighting).
 */
@Composable
fun SceneViewport(
    assetPath: String?,
    scene: SceneState,
    onRotate: (yaw: Float, pitch: Float) -> Unit,
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var yaw by remember { mutableFloatStateOf(scene.rotationY) }
    var pitch by remember { mutableFloatStateOf(scene.rotationX) }
    var zoom by remember { mutableFloatStateOf(scene.zoom) }

    // Keep the latest scene value for the effect without restarting it.
    val currentScene by rememberUpdatedState(scene)

    val filamentView = remember { FilamentView(context) }

    // Load the selected model whenever the asset path changes.
    DisposableEffect(assetPath) {
        filamentView.loadModel(assetPath)
        onDispose { }
    }

    // Apply wireframe + lighting from scene state.
    DisposableEffect(scene.wireframe, scene.lightingMode) {
        filamentView.setWireframe(scene.wireframe)
        filamentView.setLighting(scene.lightingMode)
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            // FilamentView handles engine release on detach.
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { filamentView },
            modifier = Modifier
                .fillMaxSize()
                .background(com.threedoubled.app.ui.theme.BgDark)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yaw += dragAmount.x * 0.01f
                        pitch += dragAmount.y * 0.01f
                        filamentView.setRotation(yaw, pitch)
                        onRotate(yaw, pitch)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.4f, 3.0f)
                        filamentView.setZoom(zoom)
                        onZoom(zoom)
                    }
                }
        )
    }
}
