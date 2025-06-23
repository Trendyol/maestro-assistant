package com.maestro.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.maestro.common.icon.MaestroIcons
import com.maestro.ide.service.MaestroTestService
import com.maestro.lang.schema.MaestroFileDetector

/**
 * Action to run a Maestro test file from context menu (right-click)
 */
class RunMaestroTestAction :
    AnAction("Run Maestro Test", "Run the selected Maestro test file", MaestroIcons.RUN_ICON),
    DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project
        val virtualFile = getTargetFile(e)

        // Enable the action if we have a valid project and the file is a Maestro test
        val enabled = project != null && virtualFile != null && MaestroFileDetector.isMaestroFile(virtualFile)
        presentation.isEnabledAndVisible = enabled

        // Update the text based on the file name and add an icon
        if (enabled) {
            presentation.text = "Run Maestro Test"
            presentation.icon = MaestroIcons.RUN_ICON
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = getTargetFile(e) ?: return

        // Get the test path relative to the project
        val projectPath = project.basePath ?: return
        val testPath = if (virtualFile.path.startsWith(projectPath)) {
            virtualFile.path.substring(projectPath.length + 1)
        } else {
            virtualFile.path
        }

        // Get the PsiFile for the virtual file
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return

        // Get the test service and run the test
        val testService = MaestroTestService.getInstance(project)
        testService.runTest(testPath)
    }

    /**
     * Get the target file from the action event
     */
    private fun getTargetFile(e: AnActionEvent): VirtualFile? {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile != null && psiFile.virtualFile != null) {
            return psiFile.virtualFile
        }

        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (!virtualFiles.isNullOrEmpty()) {
            return virtualFiles[0]
        }

        // Try fallback to single virtual file
        return e.getData(CommonDataKeys.VIRTUAL_FILE)
    }
}