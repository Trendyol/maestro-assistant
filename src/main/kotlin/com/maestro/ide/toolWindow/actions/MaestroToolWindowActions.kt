package com.maestro.ide.toolWindow.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.maestro.common.icon.MaestroIcons
import com.maestro.ide.service.MaestroTestService

/**
 * Factory for creating toolbar actions used in the Maestro test tool window.
 *
 * This factory provides actions for:
 * - Refreshing the test list
 * - Running selected tests
 * - Stopping running tests
 */
class MaestroToolWindowActions(
    project: Project,
    private val onRefresh: () -> Unit,
    private val onRunSelected: () -> Unit,
    private val onStop: () -> Unit,
    private val canRunTests: () -> Boolean
) {

    private val testService = MaestroTestService.getInstance(project)

    /**
     * Creates the refresh tests action
     */
    fun createRefreshAction(): AnAction {
        return object : AnAction("Refresh Tests", "Refresh test list", MaestroIcons.REFRESH_ICON) {
            override fun actionPerformed(e: AnActionEvent) {
                onRefresh()
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }
    }

    /**
     * Creates the run selected tests action
     */
    fun createRunSelectedAction(): AnAction {
        return object : AnAction(
            "Run Selected Tests",
            "Run selected Maestro tests sequentially",
            MaestroIcons.RUN_ICON
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                onRunSelected()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = !testService.isTestRunnerActive && canRunTests()
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }
    }

    /**
     * Creates the stop tests action
     */
    fun createStopAction(): AnAction {
        return object : AnAction("Stop Tests", "Stop running tests", MaestroIcons.STOP_ICON) {
            override fun actionPerformed(e: AnActionEvent) {
                onStop()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = testService.isTestRunnerActive
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }
    }
}