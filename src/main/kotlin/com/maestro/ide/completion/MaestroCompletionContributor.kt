package com.maestro.ide.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.maestro.lang.schema.MaestroFileDetector
import com.maestro.lang.syntax.MaestroSyntax
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides intelligent code completion for Maestro test files.
 *
 * This contributor offers context-aware completions for:
 * - Root-level Maestro commands and configuration
 * - Action-level commands within test sequences
 * - Command-specific arguments and parameters
 * - General YAML structure with Maestro-specific enhancements
 */
class MaestroCompletionContributor : CompletionContributor() {
    companion object {
        private val LOG = Logger.getInstance(MaestroCompletionContributor::class.java)

        // Thread-safe cache for file type checking results
        private val checkedFiles = ConcurrentHashMap<String, Boolean>()

        /**
         * Clear the completion cache to force re-evaluation of file types
         */
        fun clearCache() {
            checkedFiles.clear()
        }

        // ============= Completion Element Factories =============

        /**
         * Creates a completion element based on the command type and context
         */
        fun createCompletionElement(name: String, type: String, isSequenceItem: Boolean = false): LookupElement {
            return when {
                isSequenceItem -> createSequenceLookupElement(name, type)
                shouldUseSameLineCompletion(name) -> createSameLineElement(name, type)
                else -> createLookupElement(name, type)
            }
        }

        /**
         * Checks if a command should use same-line completion
         */
        private fun shouldUseSameLineCompletion(name: String): Boolean {
            return MaestroSyntax.createFromValue(name).let {
                it is MaestroSyntax.Command && it.autoCompletionType == MaestroSyntax.AutoCompletionType.SAME_LINE
            }
        }

        /**
         * Create a lookup element with standard formatting
         */
        fun createLookupElement(name: String, type: String): LookupElement {
            return LookupElementBuilder.create(name)
                .withTypeText(type, true)
                .withInsertHandler { context, _ ->
                    val insertText = if (context.completionChar == ':') {
                        // If user typed ':', don't insert it again
                        context.setAddCompletionChar(false)
                        ": "
                    } else {
                        // User didn't type ':', add it ourselves
                        ": "
                    }
                    // Add the colon and a space
                    context.document.insertString(context.selectionEndOffset, insertText)
                    // Move the caret after the space
                    context.editor.caretModel.moveToOffset(context.selectionEndOffset + insertText.length)
                }
        }

        /**
         * Create a lookup element for a sequence item (with a hyphen)
         */
        fun createSequenceLookupElement(name: String, type: String): LookupElement {
            return LookupElementBuilder.create(name)
                .withTypeText(type, true)
                .withPresentableText("- $name")
                .withInsertHandler { context, _ ->
                    // Get the text being replaced to check if we're in the middle of a word
                    val replacedText = context.document.getText(
                        com.intellij.openapi.util.TextRange(
                            context.startOffset,
                            context.tailOffset
                        )
                    )

                    // First get the current text
                    val document = context.document
                    val offset = context.startOffset
                    val lineStartOffset = document.getLineStartOffset(document.getLineNumber(offset))
                    val prefix = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, offset))

                    // Determine what to insert based on the current line content
                    val insertText = if (prefix.trim().isEmpty()) {
                        // Start of line, insert with -
                        "- $name: "
                    } else {
                        // Something else already exists, insert with a space
                        "$name: "
                    }

                    // Insert the text and position the cursor
                    document.replaceString(context.startOffset, context.tailOffset, insertText)
                    // Corrected the calculation to keep the cursor at the end of the inserted text
                    context.editor.caretModel.moveToOffset(context.startOffset + insertText.length)
                }
        }

        /**
         * Create a specialized insert handler for elements that ensures
         * the cursor stays on the same line, even in the middle of a document
         */
        fun createSameLineElement(name: String, type: String): LookupElement {
            return LookupElementBuilder.create(name)
                .withTypeText(type, true)
                .withInsertHandler { context, _ ->
                    // Get the current editor state
                    val document = context.document
                    val editor = context.editor
                    val offset = context.selectionEndOffset
                    val lineNumber = document.getLineNumber(offset)

                    // Insert colon and space
                    val insertText = ": "
                    document.insertString(offset, insertText)

                    // Don't add any other characters
                    context.setAddCompletionChar(false)

                    // Calculate the final position
                    val finalOffset = offset + insertText.length

                    // Ensure we're still on the same line
                    val newLineNumber = document.getLineNumber(finalOffset)
                    if (newLineNumber != lineNumber) {
                        // If we somehow moved to a new line, get back to the previous line's end
                        val previousLineEndOffset = document.getLineEndOffset(lineNumber)
                        editor.caretModel.moveToOffset(previousLineEndOffset)
                    } else {
                        // Otherwise just move to the correct position after insertion
                        editor.caretModel.moveToOffset(finalOffset)
                    }

                    // Commit the document to ensure changes take effect
                    PsiDocumentManager.getInstance(context.project).commitDocument(document)
                }
        }
    }

    init {
        // Add more specific patterns for different contexts
        extend(CompletionType.BASIC, rootKeyPattern(), RootCompletionProvider())
        extend(CompletionType.BASIC, actionKeyPattern(), ActionCompletionProvider())
        extend(CompletionType.BASIC, commandArgumentPattern(), CommandArgumentCompletionProvider())
        extend(CompletionType.BASIC, anyYamlPattern(), GeneralCompletionProvider())
    }

    // Pattern for root level keys
    private fun rootKeyPattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .withLanguage(YAMLLanguage.INSTANCE)
            .withParent(YAMLDocument::class.java)
    }

    // Pattern for action level keys
    private fun actionKeyPattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .withLanguage(YAMLLanguage.INSTANCE)
            .withParent(YAMLSequence::class.java)
    }

    // Pattern for command arguments (any command, not just tapOn)
    private fun commandArgumentPattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .withLanguage(YAMLLanguage.INSTANCE)
            .withSuperParent(3, YAMLKeyValue::class.java)
    }

    // Fallback pattern for any YAML element
    private fun anyYamlPattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .withLanguage(YAMLLanguage.INSTANCE)
    }

    /**
     * Base class for all Maestro completion providers
     */
    abstract class BaseMaestroCompletionProvider : CompletionProvider<CompletionParameters>() {
        private val isProcessing = ThreadLocal.withInitial { false }

        protected fun isMaestroFile(file: VirtualFile): Boolean {
            // Use extension-based heuristic first
            val extension = file.extension?.lowercase()
            if (extension != "yaml" && extension != "yml") {
                return false
            }

            // Check if it's in a Maestro folder - fastest check
            if (MaestroFileDetector.isInMaestroFolder(file)) {
                return true
            }

            // Check cache for previous content analysis results
            return checkedFiles.getOrPut(file.url) {
                // For completion, we'll be lenient to improve usability while developing
                // Either it's a real Maestro file or it's a new YAML file that could become one
                MaestroFileDetector.isMaestroFile(file) || file.length < 200
            }
        }

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            if (isProcessing.get()) {
                return
            }

            try {
                isProcessing.set(true)
                val file = parameters.originalFile.virtualFile ?: return

                // Only continue if it's a potential Maestro file
                if (isMaestroFile(file)) {
                    addMaestroCompletions(parameters, context, result)
                }
            } catch (e: Exception) {
                LOG.debug("Exception during Maestro completion", e)
            } finally {
                isProcessing.set(false)
            }
        }

        protected abstract fun addMaestroCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        )
    }

    /**
     * Provider for root level properties
     */
    class RootCompletionProvider : BaseMaestroCompletionProvider() {
        override fun addMaestroCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            // Always add root completions
            addRootCompletions(result)
        }

        private fun addRootCompletions(result: CompletionResultSet) {
            MaestroSyntax.ROOT_COMMANDS.forEach { command ->
                result.addElement(createCompletionElement(command.key, "Maestro"))
            }
        }
    }

    /**
     * Provider for action properties
     */
    class ActionCompletionProvider : BaseMaestroCompletionProvider() {
        override fun addMaestroCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            // Always add action completions
            addActionCompletions(result)
        }

        private fun addActionCompletions(result: CompletionResultSet) {
            MaestroSyntax.ACTION_COMMANDS.forEach { command ->
                result.addElement(createCompletionElement(command.key, "Maestro Action", true))
            }
        }
    }

    /**
     * Provider for command arguments
     */
    class CommandArgumentCompletionProvider : BaseMaestroCompletionProvider() {
        override fun addMaestroCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.position
            val parent = position.parent ?: return

            // Check if this is inside a specific command
            val command = findCommandContext(parent)
            if (command is MaestroSyntax.Command) {
                // Add completions for the command arguments
                addCommandArgumentCompletions(result, command)
            }
        }

        /**
         * Find the command context by navigating up the PSI tree
         */
        private fun findCommandContext(element: PsiElement): MaestroSyntax.Command? {
            // Navigate up to find the command key
            var current: PsiElement = element

            // Look up to find a YAMLKeyValue with a command name
            while (current.parent != null) {
                if (current is YAMLKeyValue) {
                    val keyText = current.keyText
                    // Check if this key corresponds to a Maestro command
                    val command = MaestroSyntax.ALL_COMMANDS[keyText]
                    if (command is MaestroSyntax.Command) {
                        return command
                    }
                }
                current = current.parent
            }

            return null
        }

        private fun addCommandArgumentCompletions(
            result: CompletionResultSet,
            command: MaestroSyntax.Command
        ) {
            command.allowedSubCommands.forEach { subCommand ->
                result.addElement(createCompletionElement(subCommand.key, "${command.key} Argument"))
            }
        }
    }

    /**
     * General fallback provider that looks at context to decide what completions to provide
     */
    class GeneralCompletionProvider : BaseMaestroCompletionProvider() {
        override fun addMaestroCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.position
            val parent = position.parent

            // Check if we're inside a specific command first
            val commandContext = findCommandContext(position)
            if (commandContext != null) {
                // Inside a specific command, suggest its allowed subcommands
                addCommandArgumentCompletions(result, commandContext)
                return
            }

            // Otherwise fall back to the general context detection
            when {
                // Root level
                parent is YAMLDocument ||
                        (parent is YAMLMapping && parent.parent is YAMLDocument) -> {
                    addRootCompletions(result)
                }

                // Action level
                parent is YAMLSequence ||
                        position.prevSibling is YAMLSequence ||
                        (parent is YAMLMapping && parent.parent is YAMLSequence) -> {
                    addActionCompletions(result)
                }

                // Add both as fallback
                else -> {
                    addRootCompletions(result)
                    addActionCompletions(result)
                }
            }
        }

        /**
         * Find the command context of the current position
         * @return The MaestroSyntax.Command if found, null otherwise
         */
        private fun findCommandContext(element: PsiElement): MaestroSyntax.Command? {
            // Navigate up to find the command key
            var current: PsiElement = element

            // Look up to find a YAMLKeyValue with a command name
            while (current.parent != null) {
                if (current is YAMLKeyValue) {
                    val keyText = current.keyText
                    // Check if this key corresponds to a Maestro command
                    val command = MaestroSyntax.ALL_COMMANDS[keyText]
                    if (command is MaestroSyntax.Command && command.allowedSubCommands.isNotEmpty()) {
                        return command
                    }
                }
                // Stop at sequence boundaries
                if (current is YAMLSequenceItem) {
                    break
                }
                current = current.parent
            }

            return null
        }

        /**
         * Add completions for a specific command's arguments
         */
        private fun addCommandArgumentCompletions(
            result: CompletionResultSet,
            command: MaestroSyntax.Command
        ) {
            command.allowedSubCommands.forEach { subCommand ->
                result.addElement(createCompletionElement(subCommand.key, "${command.key} Argument"))
            }
        }

        private fun addRootCompletions(result: CompletionResultSet) {
            MaestroSyntax.ROOT_COMMANDS.forEach { command ->
                result.addElement(createCompletionElement(command.key, "Maestro"))
            }
        }

        private fun addActionCompletions(result: CompletionResultSet) {
            MaestroSyntax.ACTION_COMMANDS.forEach { command ->
                result.addElement(createCompletionElement(command.key, "Maestro Action", true))
            }
        }
    }
}
