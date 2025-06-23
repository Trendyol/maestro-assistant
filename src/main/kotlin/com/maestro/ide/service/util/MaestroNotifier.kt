package com.maestro.ide.service.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.maestro.common.extension.invokeLater

/**
 * Utility class for displaying user notifications in the Maestro plugin.
 *
 * This class provides a centralized way to show different types of notifications
 * to the user, ensuring consistent notification handling across the plugin.
 */
class MaestroNotifier(private val project: Project) {

    companion object {
        private const val NOTIFICATION_GROUP_ID = "Maestro.Tests"
    }

    /**
     * Displays an error notification to the user.
     *
     * @param title The notification title
     * @param content The notification message content
     */
    fun showError(title: String, content: String) {
        showNotification(title, content, NotificationType.ERROR)
    }
    
    /**
     * Displays an informational notification to the user.
     *
     * @param title The notification title
     * @param content The notification message content
     */
    fun showInfo(title: String, content: String) {
        showNotification(title, content, NotificationType.INFORMATION)
    }
    
    /**
     * Displays a warning notification to the user.
     *
     * @param title The notification title
     * @param content The notification message content
     */
    fun showWarning(title: String, content: String) {
        showNotification(title, content, NotificationType.WARNING)
    }

    /**
     * Creates and displays a notification balloon in the IDE.
     *
     * @param title The notification title
     * @param content The notification message content
     * @param type The type of notification (error, warning, info)
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        invokeLater {
            // Create a notification using the simpler API that doesn't require pre-registration
            val notification = Notification(
                NOTIFICATION_GROUP_ID,
                title,
                content,
                type
            )

            // Show the notification in the IDE
            Notifications.Bus.notify(notification, project)
        }
    }
}
