package com.maestro.common.extension

import com.intellij.openapi.project.Project
import com.maestro.ide.service.model.MaestroTest
import java.io.File

/**
 * Extension functions for IntelliJ Project to simplify common operations.
 */

/**
 * Converts an absolute file path to a relative path from the project root.
 *
 * @param filePath Absolute file path to convert
 * @return Relative path from project root, or original path if not under project
 */
fun Project.getRelativePath(filePath: String): String {
    val projectBasePath = basePath ?: return filePath
    return if (filePath.startsWith(projectBasePath)) {
        filePath.substring(projectBasePath.length + 1)
    } else {
        filePath
    }
}

/**
 * Discovers all Maestro test files in the project.
 *
 * Searches for YAML files within the .maestro directory and creates
 * MaestroTest instances for each discovered test file.
 *
 * @return List of all Maestro tests found in the project
 */
fun Project.getAllTests(): List<MaestroTest> {
    val projectPath = basePath ?: return emptyList()
    val maestroDirectory = File(projectPath, ".maestro")

    // Return empty list if .maestro directory doesn't exist
    if (!maestroDirectory.exists() || !maestroDirectory.isDirectory) {
        return emptyList()
    }

    val projectBase = File(projectPath)

    return maestroDirectory.walk()
        .filter { file -> file.isFile && file.extension == "yaml" }
        .map { file -> MaestroTest(file.relativeTo(projectBase).path) }
        .toList()
}
