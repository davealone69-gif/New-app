package com.threedoubled.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedoubled.app.data.AvatarModel

/**
 * Browse the avatar library (Female / Male / Cyborg) and pick a preset to edit.
 */
@Composable
fun ModelLibraryScreen(
    models: List<AvatarModel>,
    selectedId: String?,
    onSelect: (AvatarModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Text("Presets & Library", style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            items(models, key = { it.id }) { model ->
                Card(
                    onClick = { onSelect(model) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(model.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            model.category.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            FilterChip(
                                selected = model.id == selectedId,
                                onClick = { onSelect(model) },
                                label = { Text(if (model.id == selectedId) "Editing" else "Open") }
                            )
                        }
                    }
                }
            }
        }
    }
}
