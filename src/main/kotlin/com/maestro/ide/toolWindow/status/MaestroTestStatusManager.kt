package com.maestro.ide.toolWindow.status

import com.maestro.ide.service.MaestroTestService
import com.maestro.ide.service.model.MaestroTest
import com.maestro.ide.service.model.TestStatus
import javax.swing.JLabel

/**
 * Manages the status display for the Maestro test tool window.
 *
 * This class handles:
 * - Updating status labels with test counts
 * - Providing readable status messages
 * - Tracking test execution states
 */
class MaestroTestStatusManager(
    private val statusLabel: JLabel,
    private val testService: MaestroTestService
) {

    /**
     * Updates the status label with current test counts and execution state
     */
    fun updateStatusLabel(allTests: List<MaestroTest>) {
        val queuedCount = allTests.count { it.status == TestStatus.QUEUED }
        val runningCount = allTests.count { it.status == TestStatus.RUNNING }
        val passedCount = allTests.count { it.status == TestStatus.PASSED }
        val failedCount = allTests.count { it.status == TestStatus.FAILED }

        val statusText = buildStatusText(queuedCount, runningCount, passedCount, failedCount)
        statusLabel.text = statusText
    }

    /**
     * Builds the status text based on test counts
     */
    private fun buildStatusText(queuedCount: Int, runningCount: Int, passedCount: Int, failedCount: Int): String {
        val statusText = StringBuilder()

        // Show running tests information
        if (testService.isTestRunnerActive) {
            statusText.append("Running tests: ")

            if (runningCount > 0) {
                statusText.append("$runningCount running, ")
            }

            if (queuedCount > 0) {
                statusText.append("$queuedCount queued, ")
            }
        }

        // Show completed tests information
        if (passedCount > 0) {
            statusText.append("$passedCount passed, ")
        }

        if (failedCount > 0) {
            statusText.append("$failedCount failed")
        } else if (statusText.isNotEmpty()) {
            // Remove trailing comma and space if present
            statusText.setLength(statusText.length - 2)
        } else if (!testService.isTestRunnerActive) {
            statusText.append("Ready")
        }

        return statusText.toString()
    }
}