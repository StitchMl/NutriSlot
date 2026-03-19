package it.lagioiaproductions.nutrislot.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.data.water.WaterPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WaterTrackerViewModel(
    private val repository: WaterPreferencesRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureCurrentDay()
        }
    }

    val uiState: StateFlow<WaterTrackerUiState> = repository.preferencesFlow
        .map { stored ->
            WaterTrackerUiState(
                targetMl = stored.targetMl,
                consumedMl = stored.consumedMl,
                remindersEnabled = stored.remindersEnabled,
                reminderIntervalMinutes = stored.reminderIntervalMinutes,
                containerPresets = stored.containerPresets
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WaterTrackerUiState()
        )

    fun addWater(amountMl: Int, saveAsPresetIfMissing: Boolean) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            repository.addConsumedMl(amountMl, saveAsPresetIfMissing)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repository.resetConsumedMl()
        }
    }

    fun updateGoal(targetMl: Int) {
        if (targetMl < 0) return
        viewModelScope.launch {
            repository.setTargetMl(targetMl)
        }
    }

    fun clearGoal() {
        viewModelScope.launch {
            repository.clearGoal()
        }
    }

    fun updateReminder(enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            repository.setReminderConfig(enabled, intervalMinutes)
        }
    }

    fun addContainerPreset(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            repository.addContainerPreset(amountMl)
        }
    }

    fun removeContainerPreset(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            repository.removeContainerPreset(amountMl)
        }
    }
}

class WaterTrackerViewModelFactory(
    private val repository: WaterPreferencesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterTrackerViewModel::class.java)) {
            return WaterTrackerViewModel(repository) as T
        }
        error("Unsupported ViewModel class: ${modelClass.name}")
    }
}