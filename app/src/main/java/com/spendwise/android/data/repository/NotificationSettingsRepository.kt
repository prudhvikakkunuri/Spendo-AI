package com.spendwise.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for automatic detection settings and user preferences.
 * Uses persistent SharedPreferences for zero-overhead, non-blocking synchronous checks
 * inside Android notification listener service callbacks.
 */
class NotificationSettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isDetectionPaused = MutableStateFlow(
        prefs.getBoolean(KEY_DETECTION_PAUSED, false)
    )
    val isDetectionPaused: StateFlow<Boolean> = _isDetectionPaused.asStateFlow()

    /**
     * Synchronous check used on the notification callback thread.
     */
    fun isDetectionPaused(): Boolean {
        return _isDetectionPaused.value
    }

    /**
     * Toggles detection pause state and persists it.
     */
    fun setDetectionPaused(paused: Boolean) {
        prefs.edit().putBoolean(KEY_DETECTION_PAUSED, paused).apply()
        _isDetectionPaused.value = paused
    }

    companion object {
        private const val PREFS_NAME = "spendwise_notification_settings"
        private const val KEY_DETECTION_PAUSED = "key_detection_paused"
    }
}
