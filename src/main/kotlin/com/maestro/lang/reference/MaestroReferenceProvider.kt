package com.maestro.lang.reference

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.maestro.lang.syntax.MaestroSyntax
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Provides references to file paths and output variables in Maestro files.
 * - File references: runScript, evalScript, runFlow
 * - Output references: ${output.xxx} variables
 */
class MaestroReferenceProvider : PsiReferenceProvider() {
    companion object {
        private val LOG = Logger.getInstance(MaestroReferenceProvider::class.java)
    }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is YAMLScalar) return PsiReference.EMPTY_ARRAY

        val scalarText = element.textValue
        if (scalarText.isBlank()) return PsiReference.EMPTY_ARRAY

        val references = mutableListOf<PsiReference>()

        val parent = element.parent
        if (parent is YAMLKeyValue) {
            val commandKey = parent.keyText
            val command = MaestroSyntax.createFromValue(commandKey)
            if (command is MaestroSyntax.Command && command.acceptsFileReferences &&
                scalarText.matches(Regex(".*\\.(js|yaml|yml)$"))
            ) {
                references.add(MaestroFileReference(element, scalarText))
            }
        }

        // Check for output variable references
        val pattern = Regex("\\$\\{output\\.([^}]*)}")
        val matches = pattern.findAll(scalarText)

        for (match in matches) {
            val startOffset = element.textRange.startOffset + match.range.first
            val endOffset = element.textRange.startOffset + match.range.last + 1
            val range = TextRange(startOffset, endOffset).shiftRight(-element.textRange.startOffset)
            references.add(MaestroOutputReference(element, range, match.groupValues[1]))
        }

        return references.toTypedArray()
    }
}

