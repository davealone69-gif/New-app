package com.threedoubled.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Animation + export: pick a loop (Idle / Walk / Turn), preview, and export
 * the result as MP4 or GIF. The actual frame capture is driven by the viewport
 * renderer; this screen exposes the controls and output status.
 */
@Composable
fun AnimationScreen(
    onExport: (String) -> Unit,
    exporting: Boolean,
    lastPath: String?,
    modifier: Modifier = Modifier
) {
    val loops = listOf("Idle", "Walk", "Turn")
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Animation & Export", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            loops.forEachIndexed { i, loop ->
                FilterChip(
                    selected = selected == i,
                    onClick = { selected = i },
                    label = { Text(loop) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { onExport("mp4") }, enabled = !exporting) {
                Text("Export MP4")
            }
            OutlinedButton(onClick = { onExport("gif") }, enabled = !exporting) {
                Text("Export GIF")
            }
        }

        if (exporting) {
            Text(
                "Rendering…",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        lastPath?.let {
            Text(
                "Saved: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
