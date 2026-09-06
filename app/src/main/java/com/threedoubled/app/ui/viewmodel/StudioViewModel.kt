package com.threedoubled.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.threedoubled.app.data.AvatarModel
import com.threedoubled.app.data.ModelRepository
import com.threedoubled.app.data.SceneState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudioUiState(
    val library: List<AvatarModel> = emptyList(),
    val selected: AvatarModel? = null,
    val scene: SceneState = SceneState(),
    val isLoading: Boolean = false
)

class StudioViewModel(
    private val repository: ModelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val lib = repository.loadLibrary()
            _state.value = StudioUiState(
                library = lib,
                selected = lib.firstOrNull(),
                isLoading = false
            )
        }
    }

    fun select(model: AvatarModel) {
        _state.value = _state.value.copy(selected = model)
    }

    fun setRotation(yaw: Float, pitch: Float) {
        _state.value = _state.value.copy(scene = _state.value.scene.copy(rotationY = yaw, rotationX = pitch))
    }

    fun setZoom(zoom: Float) {
        _state.value = _state.value.copy(scene = _state.value.scene.copy(zoom = zoom))
    }

    fun setWireframe(on: Boolean) {
        _state.value = _state.value.copy(scene = _state.value.scene.copy(wireframe = on))
    }

    fun setLighting(mode: SceneState.LightingMode) {
        _state.value = _state.value.copy(scene = _state.value.scene.copy(lightingMode = mode))
    }

    companion object {
        fun factory(repository: ModelRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StudioViewModel(repository) as T
            }
    }
}
