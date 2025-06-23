package com.maestro.lang.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.maestro.ide.completion.MaestroCompletionContributor
import com.maestro.lang.schema.MaestroFileDetector

/**
 * Service for managing Maestro-related resources for the project.
 * Handles cleanup on project close to prevent memory leaks.
 */
class MaestroProjectListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        MaestroCompletionContributor.clearCache()
        MaestroFileDetector.clearCache()
    }
}