package com.maestro.lang.reference

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

/**
 * Utility class to resolve the actual values of output variables
 * defined in JavaScript files.
 */
class MaestroOutputValueResolver {
    companion object {
        private val LOG = Logger.getInstance(MaestroOutputValueResolver::class.java)

        /**
         * Resolves the value of an output variable path
         * @param project The current project
         * @param currentFile The file containing the reference
         * @param variablePath The path of the variable to resolve (e.g. "homepage.accountTab")
         * @return The resolved value or null if it couldn't be resolved
         */
        fun resolveOutputValue(project: Project, currentFile: VirtualFile, variablePath: String): String? {
            val currentDir = currentFile.parent ?: return null
            val psiFile = PsiManager.getInstance(project).findFile(currentFile) ?: return null

            // Get the script paths from runScript directives if available
            val scriptPaths = MaestroJavaScriptUtils.findScriptPathsInFile(psiFile.text)

            // Find JavaScript files based on the script paths + general directory search
            val jsFiles = if (scriptPaths.isNotEmpty()) {
                scriptPaths.mapNotNull { MaestroJavaScriptUtils.resolveScriptPath(currentDir, it) } +
                        MaestroJavaScriptUtils.findJsFiles(currentDir)
            } else {
                MaestroJavaScriptUtils.findJsFiles(currentDir)
            }

            // Extract parts of variable path (e.g., ["homepage", "accountTab"])
            val pathParts = variablePath.split('.')
            val topLevelVar = pathParts.firstOrNull() ?: return null

            // For each JS file, look for the variable value
            for (jsFile in jsFiles) {
                try {
                    val jsPsiFile = PsiManager.getInstance(project).findFile(jsFile) ?: continue
                    val fileText = jsPsiFile.text

                    // Find the top-level variable definition
                    val topLevelMatch = MaestroJavaScriptUtils.findTopLevelDefinition(fileText, topLevelVar) ?: continue

                    // If we only have the top-level variable, try to extract the whole object
                    if (pathParts.size == 1) {
                        return MaestroJavaScriptUtils.extractObjectContent(
                            fileText,
                            fileText.indexOf('{', topLevelMatch.range.last)
                        )
                    }

                    // Navigate through nested object paths to find the specific property value
                    return MaestroJavaScriptUtils.findNestedPropertyValue(fileText, pathParts, topLevelMatch)
                } catch (e: Exception) {
                    LOG.debug("Error resolving output value", e)
                    continue
                }
            }

            return null
        }

        /**
         * Formats a value for display in a hint
         * Shortens long strings, formats quotes properly, etc.
         */
        fun formatValueForHint(value: String?): String? {
            return MaestroJavaScriptUtils.formatValueForHint(value)
        }
    }
}