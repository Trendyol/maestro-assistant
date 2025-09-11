package com.maestro.ide.run

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.psi.PsiElement
import com.maestro.common.extension.getTestStatus
import com.maestro.common.icon.MaestroIcons
import com.maestro.ide.actions.RunMaestroTestAction
import com.maestro.ide.actions.RunMaestroTestTRAction
import com.maestro.ide.actions.RunMaestroTestARAction
import com.maestro.ide.actions.RunMaestroTestTRAZAction
import com.maestro.ide.actions.RunMaestroTestROAction
import com.maestro.ide.actions.RunMaestroTestENSAAction
import com.maestro.ide.service.model.TestStatus
import com.maestro.lang.schema.MaestroFileDetector
import org.jetbrains.yaml.psi.YAMLFile
import java.util.function.Function

/**
 * RunLineMarkerContributor that adds a run icon in the gutter for Maestro test files.
 * Clicking the icon runs the Maestro test.
 */
class MaestroRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        // Only process leaf elements as required by LineMarkerProvider documentation
        // Non-leaf elements can cause performance issues
        if (element.firstChild != null) {
            return null
        }

        val file = element.containingFile
        val virtualFile = file.virtualFile ?: return null

        // Check if this is a Maestro test file
        if (file !is YAMLFile || !MaestroFileDetector.isMaestroFile(virtualFile)) {
            return null
        }

        // Only place marker on the very first leaf element of the file
        // This ensures exactly one marker per file
        if (element.textOffset != 0) {
            return null
        }

        // Use the MaestroFileDetector to check for valid Maestro file content
        // This is more comprehensive than just checking for appId
        val fileText = file.text
        if (!MaestroFileDetector.isMaestroFile(fileText)) {
            return null
        }

        // Get stored test status if any
        val status = file.getTestStatus()

        // Get icon based on status
        val icon = MaestroIcons.RUN_ICON

        // Parse tags from the file to determine available regions
        // Use file.text instead of virtualFile to get the current content
        val tags = MaestroFileDetector.parseTagsFromContent(fileText)
        val availableRegions = MaestroFileDetector.getAvailableRegions(tags)
        
        // Create actions based on available regions
        val actions = mutableListOf<AnAction>()
        
        // Always add the default action first
        actions.add(RunMaestroTestAction())
        
        // Add region-specific actions based on available tags
        // If no expected regions found, default to TR only
        val regionsToShow = if (availableRegions.isEmpty()) {
            setOf("TR")
        } else {
            availableRegions
        }
        
        // Add actions for each available region
        regionsToShow.forEach { region ->
            when (region) {
                "TR" -> actions.add(RunMaestroTestTRAction())
                "AR" -> actions.add(RunMaestroTestARAction())
                "AZ" -> actions.add(RunMaestroTestTRAZAction())
                "RO" -> actions.add(RunMaestroTestROAction())
                "SA" -> actions.add(RunMaestroTestENSAAction())
            }
        }

        // Return the line marker info with our run icon and dynamic actions
        return Info(icon, actions.toTypedArray(), generateJavaTooltipProvider(status))
    }

    private fun generateJavaTooltipProvider(status: TestStatus?): Function<PsiElement, String> {
        return Function { _ -> generateTooltip(status) }
    }

    private fun generateTooltip(status: TestStatus?): String {
        return when (status) {
            TestStatus.PASSED -> "Run Maestro Test (Last run: Passed)"
            TestStatus.FAILED -> "Run Maestro Test (Last run: Failed)"
            TestStatus.RUNNING -> "Maestro Test is running..."
            TestStatus.QUEUED -> "Maestro Test is queued..."
            else -> "Run Maestro Test"
        }
    }
}
