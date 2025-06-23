package com.maestro.ide.service.model

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Represents the execution status of a Maestro test.
 *
 * Each status has an associated icon for UI display and provides
 * utility methods for checking test state categories.
 */
enum class TestStatus(val icon: Icon) {

    /** Test has not been executed yet */
    NOT_RUN(
        icon = IconLoader.getIcon("/icons/test-not-run.svg", TestStatus::class.java)
    ),

    /** Test is queued for execution but not yet started */
    QUEUED(
        icon = IconLoader.getIcon("/icons/test-running.svg", TestStatus::class.java)
    ),

    /** Test is currently executing */
    RUNNING(
        icon = IconLoader.getIcon("/icons/test-running.svg", TestStatus::class.java)
    ),

    /** Test executed successfully without errors */
    PASSED(
        icon = IconLoader.getIcon("/icons/test-passed.svg", TestStatus::class.java)
    ),

    /** Test execution failed or encountered errors */
    FAILED(
        icon = IconLoader.getIcon("/icons/test-failed.svg", TestStatus::class.java)
    );

    /**
     * Checks if this status represents an active test (queued or running)
     */
    val isActive: Boolean
        get() = this == QUEUED || this == RUNNING

    /**
     * Checks if this status represents a completed test (passed or failed)
     */
    val isCompleted: Boolean
        get() = this == PASSED || this == FAILED

    /**
     * Checks if this status represents a successful test
     */
    val isSuccessful: Boolean
        get() = this == PASSED

    /**
     * Gets a human-readable display name for this status
     */
    val displayName: String
        get() = when (this) {
            NOT_RUN -> "Not Run"
            QUEUED -> "Queued"
            RUNNING -> "Running"
            PASSED -> "Passed"
            FAILED -> "Failed"
        }
}
