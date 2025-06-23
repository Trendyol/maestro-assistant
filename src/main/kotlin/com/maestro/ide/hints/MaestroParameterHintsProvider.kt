package com.maestro.ide.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import com.maestro.lang.reference.MaestroOutputValueResolver
import com.maestro.lang.schema.MaestroFileDetector
import org.jetbrains.yaml.psi.YAMLScalar
import java.util.regex.Pattern

/**
 * Provides parameter-style inlay hints for Maestro output variables.
 * Shows the actual resolved values inline in the editor.
 */
class MaestroParameterHintsProvider : InlayParameterHintsProvider {
    companion object {
        private val OUTPUT_REFERENCE_PATTERN = Pattern.compile("\\$\\{output\\.([^}]*)}")
    }

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        if (element !is YAMLScalar) return emptyList()

        val virtualFile = element.containingFile?.virtualFile ?: return emptyList()
        if (!MaestroFileDetector.isMaestroFile(virtualFile)) {
            return emptyList()
        }

        val hints = mutableListOf<InlayInfo>()
        val text = element.textValue
        val matcher = OUTPUT_REFERENCE_PATTERN.matcher(text)

        while (matcher.find()) {
            val variablePath = matcher.group(1)
            val endOffset = element.textRange.startOffset + matcher.end()

            try {
                val variableValue = MaestroOutputValueResolver.resolveOutputValue(
                    element.project,
                    virtualFile,
                    variablePath
                )

                val formattedValue = MaestroOutputValueResolver.formatValueForHint(variableValue)
                if (formattedValue != null) {
                    // Add an inlay hint at the end of the variable reference
                    hints.add(InlayInfo("→ $formattedValue", endOffset))
                }
            } catch (e: Exception) {
                // Skip if we can't resolve
            }
        }

        return hints
    }

    // We want all hints to be shown (no blacklists)
    override fun getDefaultBlackList(): Set<String> = emptySet()

    // We don't need to worry about these for our simple implementation
    override fun getHintInfo(element: PsiElement): HintInfo? = null
    override fun getInlayPresentation(inlayText: String): String = inlayText

    // Always show our hints since they're output values
    override fun isBlackListSupported(): Boolean = false
}