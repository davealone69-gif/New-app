package com.threedoubled.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Material & morph editor: sliders for metallic / roughness / emissive and
 * body morphs. Values here map onto the selected avatar's material params.
 */
@Composable
fun MaterialEditorScreen(
    modelName: String,
    modifier: Modifier = Modifier
) {
    var metallic by remember { mutableFloatStateOf(0.0f) }
    var roughness by remember { mutableFloatStateOf(0.6f) }
    var emissive by remember { mutableFloatStateOf(0.0f) }
    var bustSize by remember { mutableFloatStateOf(0.5f) }
    var waistSize by remember { mutableFloatStateOf(0.5f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Materials — $modelName", style = MaterialTheme.typography.titleLarge)
        SliderRow("Metallic", metallic) { metallic = it }
        SliderRow("Roughness", roughness) { roughness = it }
        SliderRow("Emissive / Glow", emissive) { emissive = it }
        SliderRow("Bust morph", bustSize) { bustSize = it }
        SliderRow("Waist morph", waistSize) { waistSize = it }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(String.format("%.2f", value), style = MaterialTheme.typography.bodySmall)
            }
            Slider(value = value, onValueChange = onChange)
        }
    }
}
