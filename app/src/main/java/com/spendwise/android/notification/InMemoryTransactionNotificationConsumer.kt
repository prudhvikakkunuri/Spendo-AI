package com.spendwise.android.notification

import com.spendwise.android.notification.model.RawNotificationPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory testable stub for Phase 4 notification access infrastructure.
 * Records received notification metadata in memory without persisting sensitive notification bodies.
 */
class InMemoryTransactionNotificationConsumer : TransactionNotificationConsumer {

    private val _receivedPayloads = MutableStateFlow<List<RawNotificationPayload>>(emptyList())
    val receivedPayloads: StateFlow<List<RawNotificationPayload>> = _receivedPayloads.asStateFlow()

    private val payloadList = CopyOnWriteArrayList<RawNotificationPayload>()

    override suspend fun onNotificationReceived(payload: RawNotificationPayload) {
        payloadList.add(payload)
        _receivedPayloads.value = payloadList.toList()
    }

    fun getReceivedCount(): Int = payloadList.size

    fun getLatestPayload(): RawNotificationPayload? = payloadList.lastOrNull()

    fun clear() {
        payloadList.clear()
        _receivedPayloads.value = emptyList()
    }
}
