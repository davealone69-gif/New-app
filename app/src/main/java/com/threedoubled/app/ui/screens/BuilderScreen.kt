package com.threedoubled.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.threedoubled.app.data.AvatarModel
import com.threedoubled.app.data.ModelRepository
import com.threedoubled.app.data.SceneState
import com.threedoubled.app.ui.viewmodel.StudioViewModel

/**
 * Main studio screen: 3D viewport + bottom control pills + model library.
 */
@Composable
fun BuilderScreen(
    viewModel: StudioViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val selected = state.selected
    val context = LocalContext.current
    val repository = remember { ModelRepository(context.applicationContext) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            SceneViewport(
                assetPath = selected?.let { repository.resolveAssetPath(it) },
                scene = state.scene,
                onRotate = viewModel::setRotation,
                onZoom = viewModel::setZoom,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay: current avatar name
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Text(
                    text = selected?.name ?: "No model",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Bottom control pill bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.scene.lightingMode == SceneState.LightingMode.STUDIO,
                        onClick = { viewModel.setLighting(SceneState.LightingMode.STUDIO) },
                        label = { Text("☀ Studio") }
                    )
                    FilterChip(
                        selected = state.scene.lightingMode == SceneState.LightingMode.NEON,
                        onClick = { viewModel.setLighting(SceneState.LightingMode.NEON) },
                        label = { Text("☠ Neon") }
                    )
                    FilterChip(
                        selected = state.scene.wireframe,
                        onClick = { viewModel.setWireframe(!state.scene.wireframe) },
                        label = { Text("⟐ Wire") }
                    )
                }
            }
        }

        // Library list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("Model Library", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(state.library, key = { it.id }) { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selected?.id == model.id,
                            onClick = { viewModel.select(model) },
                            label = { Text(model.name) }
                        )
                    }
                }
            }
        }
    }
}
