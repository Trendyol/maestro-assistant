package com.maestro.common

import com.maestro.ide.service.model.TestStatus

/**
 * Simple event bus for broadcasting Maestro-related events across the plugin.
 *
 * This provides a decoupled way for components to communicate about:
 * - Test status changes
 * - File system events
 * - UI state updates
 */
object EventBus {
    private val listeners = mutableListOf<(MaestroEvent) -> Unit>()

    /**
     * Registers a listener for Maestro events
     * @param listener Function that will be called when events are sent
     */
    fun onEvent(listener: (MaestroEvent) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Broadcasts an event to all registered listeners
     * @param event The event to send to all listeners
     */
    fun sendEvent(event: MaestroEvent) {
        listeners.forEach { listener ->
            runCatching { listener(event) }
        }
    }

    /**
     * Removes all registered listeners (useful for testing)
     */
    fun clearListeners() {
        listeners.clear()
    }
}

/**
 * Base interface for all events that can be sent through the EventBus
 */
sealed interface MaestroEvent {

    /**
     * Event fired when a test's status changes
     * @param filePath Absolute path to the test file
     * @param status New status of the test
     */
    data class OnTestStatusChanged(
        val filePath: String,
        val status: TestStatus
    ) : MaestroEvent
}
