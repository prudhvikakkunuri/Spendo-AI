package com.spendwise.android.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.android.data.db.entity.MonitoredAppEntity
import com.spendwise.android.data.repository.MonitoredAppRepository
import com.spendwise.android.data.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonitoredAppsUiState(
    val apps: List<MonitoredAppEntity> = emptyList(),
    val isDetectionPaused: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * ViewModel managing monitored app allowlist and master pause/resume switch.
 */
class MonitoredAppsViewModel(
    private val monitoredAppRepository: MonitoredAppRepository,
    private val settingsRepository: NotificationSettingsRepository
) : ViewModel() {

    val apps: StateFlow<List<MonitoredAppEntity>> = monitoredAppRepository.allMonitoredAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isDetectionPaused: StateFlow<Boolean> = settingsRepository.isDetectionPaused

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            monitoredAppRepository.toggleAppEnabled(packageName, isEnabled)
        }
    }

    fun setDetectionPaused(paused: Boolean) {
        settingsRepository.setDetectionPaused(paused)
    }

    fun addCustomApp(packageName: String, appName: String) {
        if (packageName.isBlank() || appName.isBlank()) return
        viewModelScope.launch {
            monitoredAppRepository.addApp(
                packageName = packageName.trim(),
                appName = appName.trim(),
                isEnabled = true
            )
        }
    }

    fun deleteApp(packageName: String) {
        viewModelScope.launch {
            monitoredAppRepository.removeApp(packageName)
        }
    }

    companion object {
        fun provideFactory(
            monitoredAppRepository: MonitoredAppRepository,
            settingsRepository: NotificationSettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MonitoredAppsViewModel(monitoredAppRepository, settingsRepository) as T
            }
        }
    }
}
