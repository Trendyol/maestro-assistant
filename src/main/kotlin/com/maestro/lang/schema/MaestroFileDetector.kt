package com.maestro.lang.schema

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper class to detect if a YAML file is a Maestro test file
 */
object MaestroFileDetector {
    private val LOG = Logger.getInstance(MaestroFileDetector::class.java)

    // ThreadLocal to prevent recursive calls
    private val isProcessing = ThreadLocal.withInitial { false }

    // Cache for file detection results to avoid repeated checks
    private val fileDetectionCache = ConcurrentHashMap<String, Boolean>()

    // Common Maestro root properties
    private val ROOT_PROPERTIES = listOf(
        "appId:",
        "onFlowStart:",
        "runFlow:",
        "env:"
    )

    // Common Maestro actions (with leading hyphen)
    private val MAESTRO_ACTIONS = listOf(
        "- tapOn",
        "- longTapOn",
        "- swipe",
        "- scroll",
        "- inputText",
        "- launchApp",
        "- assertVisible",
        "- waitForAnimationToEnd",
        "- sleep",
        "- waitForVisible",
        "- back",
        "- runScript"
    )

    // Maestro folder names to check in path
    private val MAESTRO_FOLDERS = listOf(
        "maestro",
        ".maestro"
    )

    /**
     * Check if the file is in a Maestro test folder
     */
    fun isInMaestroFolder(file: VirtualFile): Boolean {
        try {
            val path = file.path

            // Fast check if file path contains any Maestro folder pattern
            for (folderName in MAESTRO_FOLDERS) {
                if (path.contains("/$folderName/") || path.endsWith("/$folderName")) {
                    return true
                }
            }

            return false
        } catch (e: Exception) {
            LOG.debug("Error checking if file is in Maestro folder: ${e.message}")
            return false
        }
    }

    /**
     * Check if the file content matches Maestro test patterns.
     * This is more lenient than strict detection to allow for
     * auto-completion while creating new files.
     */
    fun isMaestroFile(content: String): Boolean {
        // Empty content check
        if (content.isBlank()) {
            return false
        }

        // Check for root properties
        for (property in ROOT_PROPERTIES) {
            if (content.contains(property)) {
                return true
            }
        }

        // Check for action commands
        for (action in MAESTRO_ACTIONS) {
            if (content.contains(action)) {
                return true
            }
        }

        // For new files or files in progress, we'll be more lenient
        // Simply check for Maestro keywords
        return content.contains("Maestro Flow") ||
                content.contains("# Maestro") ||
                content.contains("appId:") ||
                content.contains("tapOn") ||
                content.contains("launchApp") ||
                content.contains("runScript")
    }

    /**
     * Check if the file is a YAML file and has Maestro content or is in a Maestro folder
     */
    fun isMaestroFile(file: VirtualFile): Boolean {
        // Prevent recursion
        if (isProcessing.get()) {
            return false
        }

        try {
            isProcessing.set(true)

            // Check cache first
            val cacheKey = file.url
            if (fileDetectionCache.containsKey(cacheKey)) {
                return fileDetectionCache[cacheKey] ?: false
            }

            // Only process YAML files
            if (!isYamlFile(file)) {
                fileDetectionCache[cacheKey] = false
                return false
            }

            // Skip files that are too big, binary, or not readable
            if (!file.isValid || !file.exists() || file.isDirectory || file.length > 1024 * 1024) {
                fileDetectionCache[cacheKey] = false
                return false
            }

            // Check if the file is in a Maestro folder - if so, it's a Maestro file
            if (!isInMaestroFolder(file)) {
                fileDetectionCache[cacheKey] = false
                return false
            }

            // For small files, be more lenient to help with auto-completion
            if (file.length < 200 && !file.name.contains("config")) {
                // If it's a very small file, assume it could be a Maestro file in progress
                fileDetectionCache[cacheKey] = true
                return true
            }

            // Check file content for Maestro specific patterns directly from IO
            try {
                // Use direct file IO for local files to avoid VFS recursion
                if (file.isInLocalFileSystem) {
                    val path = file.path
                    val content = FileUtil.loadFile(java.io.File(path), "UTF-8")
                    val result = isMaestroFile(content)
                    fileDetectionCache[cacheKey] = result
                    return result
                } else {
                    // Carefully read file content with minimal VFS interaction
                    val result = file.inputStream.use {
                        val content = it.reader(Charsets.UTF_8).readText()
                        isMaestroFile(content)
                    }
                    fileDetectionCache[cacheKey] = result
                    return result
                }
            } catch (e: Exception) {
                LOG.debug("Error reading file content for Maestro detection: ${e.message}")
                fileDetectionCache[cacheKey] = false
                return false
            }
        } finally {
            isProcessing.set(false)
        }
    }

    /**
     * Safely checks if the file is a YAML file
     */
    private fun isYamlFile(file: VirtualFile): Boolean {
        try {
            val extension = file.extension?.lowercase()
            if (extension == "yaml" || extension == "yml") {
                return true
            }

            // Avoid accessing fileType as it may cause recursion
            return false
        } catch (e: Exception) {
            LOG.debug("Error checking if file is YAML: ${e.message}")
            return false
        }
    }

    /**
     * Clear the cache - call this when project is closed or settings change
     */
    fun clearCache() {
        fileDetectionCache.clear()
    }
}