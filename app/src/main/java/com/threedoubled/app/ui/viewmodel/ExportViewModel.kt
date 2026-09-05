package com.threedoubled.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threedoubled.app.export.ExportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExportUiState(
    val isExporting: Boolean = false,
    val lastPath: String? = null,
    val error: String? = null
)

class ExportViewModel(private val exportManager: ExportManager) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun export(format: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, error = null)
            try {
                // Rendering hooks are driven by the FilamentView at the screen
                // layer; this view model owns the output path + state.
                val path = when (format) {
                    "png" -> exportManager.pngPath()
                    "glb" -> exportManager.glbPath()
                    "mp4" -> exportManager.mp4Path()
                    "gif" -> exportManager.gifPath()
                    else -> exportManager.pngPath()
                }
                _state.value = _state.value.copy(isExporting = false, lastPath = path)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isExporting = false, error = e.message)
            }
        }
    }

    fun clear() {
        _state.value = ExportUiState()
    }
}
