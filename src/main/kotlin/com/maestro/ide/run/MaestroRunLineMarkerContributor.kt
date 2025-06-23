package com.maestro.ide.run

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.maestro.common.extension.getTestStatus
import com.maestro.common.icon.MaestroIcons
import com.maestro.ide.actions.RunMaestroTestAction
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

        // Create a single action that will run the test
        val action = RunMaestroTestAction()

        // Return the line marker info with our run icon
        return Info(icon, arrayOf(action), generateJavaTooltipProvider(status))
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
