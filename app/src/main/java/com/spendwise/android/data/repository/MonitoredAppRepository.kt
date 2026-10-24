package com.spendwise.android.data.repository

import com.spendwise.android.data.db.dao.MonitoredAppDao
import com.spendwise.android.data.db.entity.MonitoredAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository managing user-selected notification sources.
 *
 * Privacy Guarantees:
 * 1. SpendWise strictly maintains an allowlist of package names.
 * 2. An in-memory concurrent set provides zero-latency O(1) checks for incoming notifications
 *    without locking SQLite on the system notification callback thread.
 * 3. Any notification from an unselected package is immediately dropped in memory.
 */
class MonitoredAppRepository(
    private val monitoredAppDao: MonitoredAppDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
) {

    // Thread-safe in-memory cache for O(1) non-blocking checks in NotificationListenerService
    private val enabledPackagesCache = ConcurrentHashMap.newKeySet<String>()

    val allMonitoredAppsFlow: Flow<List<MonitoredAppEntity>> = monitoredAppDao.getAllMonitoredAppsFlow()

    init {
        // Populate cache and ensure default popular financial apps are initialized
        externalScope.launch {
            try {
                seedDefaultAppsIfEmpty()
                refreshEnabledPackagesCache()
                monitoredAppDao.getEnabledPackageNamesFlow().collect { enabledList ->
                    enabledPackagesCache.clear()
                    enabledPackagesCache.addAll(enabledList)
                }
            } catch (e: Exception) {
                android.util.Log.w("MonitoredAppRepo", "Failed to initialize monitored apps cache: ${e.message}")
            }
        }
    }

    /**
     * Non-blocking O(1) lookup used by NotificationListenerService on the callback thread.
     */
    fun isPackageAllowed(packageName: String): Boolean {
        return enabledPackagesCache.contains(packageName)
    }

    /**
     * Returns true if the package is in the active allowlist.
     */
    suspend fun isPackageAllowedSync(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = monitoredAppDao.getMonitoredApp(packageName)
        app?.isEnabled == true
    }

    suspend fun toggleAppEnabled(packageName: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        monitoredAppDao.updateEnabledState(packageName, isEnabled, System.currentTimeMillis())
        if (isEnabled) {
            enabledPackagesCache.add(packageName)
        } else {
            enabledPackagesCache.remove(packageName)
        }
    }

    suspend fun addApp(packageName: String, appName: String, isEnabled: Boolean = true) = withContext(Dispatchers.IO) {
        val entity = MonitoredAppEntity(
            packageName = packageName.trim(),
            appName = appName.trim(),
            isEnabled = isEnabled,
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        monitoredAppDao.insertOrUpdate(entity)
        if (isEnabled) {
            enabledPackagesCache.add(packageName.trim())
        }
    }

    suspend fun removeApp(packageName: String) = withContext(Dispatchers.IO) {
        monitoredAppDao.deleteApp(packageName)
        enabledPackagesCache.remove(packageName)
    }

    suspend fun refreshEnabledPackagesCache() = withContext(Dispatchers.IO) {
        val enabledList = monitoredAppDao.getEnabledPackageNames()
        enabledPackagesCache.clear()
        enabledPackagesCache.addAll(enabledList)
    }

    private suspend fun seedDefaultAppsIfEmpty() = withContext(Dispatchers.IO) {
        val count = monitoredAppDao.getCount()
        if (count == 0) {
            monitoredAppDao.insertDefaultApps(DEFAULT_KNOWN_FINANCIAL_APPS)
            refreshEnabledPackagesCache()
        }
    }

    companion object {
        /**
         * Popular Indian payment and banking packages pre-populated for user convenience.
         * The user has full control to toggle any of these on or off, or add other apps.
         */
        val DEFAULT_KNOWN_FINANCIAL_APPS = listOf(
            MonitoredAppEntity(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "Google Pay",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "net.one97.paytm",
                appName = "Paytm",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "com.cred.club",
                appName = "CRED",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "com.hdfcbank.android",
                appName = "HDFC Bank MobileBanking",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "com.csam.icici.bank.imobile",
                appName = "ICICI iMobile Pay",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "com.sbi.lotusintouch",
                appName = "YONO SBI",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "com.axis.mobile",
                appName = "Axis Mobile",
                isEnabled = true
            ),
            MonitoredAppEntity(
                packageName = "in.org.npci.upiapp",
                appName = "BHIM UPI",
                isEnabled = true
            )
        )
    }
}
