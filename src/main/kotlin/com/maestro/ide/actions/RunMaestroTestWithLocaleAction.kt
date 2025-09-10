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
import com.maestro.ide.service.model.MaestroTest
import com.maestro.lang.schema.MaestroFileDetector
import javax.swing.Icon

/**
 * Generic action to run a Maestro test file with specific locale parameters
 * This action can be configured for different locales via constructor parameters
 */
open class RunMaestroTestWithLocaleAction(
    private val displayText: String,
    private val description: String,
    private val language: String,
    private val country: String,
    private val icon: Icon = MaestroIcons.RUN_ICON
) : AnAction(displayText, description, icon), DumbAware {

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

        // Update the text and icon
        if (enabled) {
            presentation.text = displayText
            presentation.icon = icon
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

        // Create a MaestroTest with locale-specific environment variables
        // Note: APP_ID=com.trendyol.milla.android.stage is automatically added by MaestroTestExecutor
        val maestroTest = MaestroTest(
            path = testPath,
            commandLineArgs = "-e LANGUAGE=$language -e COUNTRY=$country"
        )

        // Get the test service and run the test
        val testService = MaestroTestService.getInstance(project)
        testService.runTest(maestroTest)
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

/**
 * Pre-configured Turkish locale action
 */
class RunMaestroTestTRAction : RunMaestroTestWithLocaleAction(
    displayText = "Run in TR",
    description = "Run the selected Maestro test file with Turkish locale",
    language = "tr",
    country = "TR",
    icon = MaestroIcons.FLAG_TR
)

/**
 * Pre-configured Arabic locale action
 */
class RunMaestroTestARAction : RunMaestroTestWithLocaleAction(
    displayText = "Run in AR",
    description = "Run the selected Maestro test file with Arabic locale",
    language = "ar",
    country = "AR",
    icon = MaestroIcons.FLAG_AR
)

/**
 * Pre-configured Turkish Azerbaijan locale action
 */
class RunMaestroTestTRAZAction : RunMaestroTestWithLocaleAction(
    displayText = "Run in TR-AZ",
    description = "Run the selected Maestro test file with Turkish Azerbaijan locale",
    language = "tr",
    country = "AZ",
    icon = MaestroIcons.FLAG_AZ
)

/**
 * Pre-configured Romanian locale action
 */
class RunMaestroTestROAction : RunMaestroTestWithLocaleAction(
    displayText = "Run in RO",
    description = "Run the selected Maestro test file with Romanian locale",
    language = "ro",
    country = "RO",
    icon = MaestroIcons.RUN_ICON
)

/**
 * Pre-configured English Saudi Arabia locale action
 */
class RunMaestroTestENSAAction : RunMaestroTestWithLocaleAction(
    displayText = "Run in EN-SA",
    description = "Run the selected Maestro test file with English Saudi Arabia locale",
    language = "en",
    country = "SA",
    icon = MaestroIcons.FLAG_SA
)
