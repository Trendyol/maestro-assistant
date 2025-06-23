package com.maestro.common.icon

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Centralized icon definitions for the Maestro plugin UI.
 *
 * This object provides access to all icons used throughout the plugin,
 * ensuring consistent icon usage and easy maintenance.
 */
object MaestroIcons {

    // Action icons for toolbar and menus
    val REFRESH_ICON: Icon = IconLoader.getIcon("/icons/refresh.svg", MaestroIcons::class.java)
    val RUN_ICON: Icon = IconLoader.getIcon("/icons/run.svg", MaestroIcons::class.java)
    val STOP_ICON: Icon = IconLoader.getIcon("/icons/stop.svg", MaestroIcons::class.java)

    /**
     * Loads an icon from the plugin resources
     * @param path Path to the icon file relative to resources root
     * @return The loaded icon, or a default icon if loading fails
     */
    private fun loadIcon(path: String): Icon {
        return try {
            IconLoader.getIcon(path, MaestroIcons::class.java)
        } catch (e: Exception) {
            // Return a default icon or handle the error appropriately
            IconLoader.getIcon("/icons/default.svg", MaestroIcons::class.java)
        }
    }
}
