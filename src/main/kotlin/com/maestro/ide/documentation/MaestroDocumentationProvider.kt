package com.maestro.ide.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import com.maestro.lang.schema.MaestroFileDetector
import com.maestro.lang.syntax.MaestroSyntax
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides documentation for Maestro commands when hovering over them in the editor.
 */
class MaestroDocumentationProvider : AbstractDocumentationProvider() {
    private val log = logger<MaestroDocumentationProvider>()
    private val cache = ConcurrentHashMap<String, String?>()

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || originalElement == null) return null

        // Only handle YAML key values that correspond to Maestro commands
        if (element !is YAMLKeyValue) return null

        val keyText = element.keyText
        val command = MaestroSyntax.createFromValue(keyText)

        return if (command is MaestroSyntax.Command) {
            "Maestro command: $keyText"
        } else null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || originalElement == null) return null

        // Check if this is a Maestro file
        val file = element.containingFile?.virtualFile
        if (file == null || !MaestroFileDetector.isMaestroFile(file)) return null

        // Only handle YAML key values that correspond to Maestro commands
        if (element !is YAMLKeyValue) return null

        val keyText = element.keyText
        val command = MaestroSyntax.createFromValue(keyText)

        return if (command is MaestroSyntax.Command) {
            buildDocumentationHtml(command)
        } else null
    }

    private fun buildDocumentationHtml(command: MaestroSyntax.Command): String? {
        fetchDocumentation(command)?.let { markdown ->
            val flavour = CommonMarkFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
            val html = HtmlGenerator(markdown, parsedTree, flavour).generateHtml()

            // Add link to the original Maestro documentation
            val commandName = command.key.lowercase()
            var originalDocUrl = "https://docs.maestro.dev/api-reference/commands/$commandName"

            if (command.documentationUrl.contains("selector")) {
                originalDocUrl = "https://docs.maestro.dev/api-reference/selectors"
            }
            val linkHtml = """
                <p><br/><a href="$originalDocUrl" target="_blank">View full documentation at docs.maestro.dev</a></p>
            """.trimIndent()

            return html + linkHtml
        }

        return null
    }

    /**
     * Fetches documentation for a Maestro command from GitHub.
     */
    private fun fetchDocumentation(command: MaestroSyntax.Command): String? {
        if (command.documentationUrl.isEmpty()) return null
        val commandName = command.key
        // Check cache first
        if (cache.containsKey(commandName)) {
            return cache[commandName]
        }

        try {
            var url =
                URL("https://raw.githubusercontent.com/mobile-dev-inc/maestro-docs/main/api-reference/commands/${commandName.lowercase()}.md")

            if (command.documentationUrl.contains("selector")) {
                url = URL(
                    "https://raw.githubusercontent.com/mobile-dev-inc/maestro-docs/main/api-reference/selectors.md"
                )
            }
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.info("Failed to fetch documentation for $commandName, response code: $responseCode")
                cache[commandName] = null
                return null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val content = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                content.append(line)
                content.append("\n")
            }

            reader.close()
            connection.disconnect()

            val markdownContent = content.toString()
            if (markdownContent.isBlank()) {
                cache[commandName] = null
                return null
            }

            // Convert the markdown to HTML content
            val htmlContent = convertMarkdownToHtml(markdownContent)
            cache[commandName] = htmlContent
            return htmlContent
        } catch (e: Exception) {
            log.warn("Error fetching documentation for $commandName", e)
            cache[commandName] = null
            return null
        }
    }

    /**
     * Converts markdown content to HTML.
     * This is a simple conversion that handles some basic Markdown syntax.
     */
    private fun convertMarkdownToHtml(markdown: String): String {
        val sb = StringBuilder()

        // First, filter out GitBook-specific syntax
        val filteredMarkdown = filterGitBookSyntax(markdown)

        // Decode HTML entities
        val decodedMarkdown = decodeHtmlEntities(filteredMarkdown)

        // Process markdown line by line for basic formatting
        val lines = decodedMarkdown.split("\n")
        var inCodeBlock = false
        var inList = false

        for (line in lines) {
            // Skip empty lines after filtering
            if (line.isBlank()) {
                if (inList) {
                    sb.append("</ul>")
                    inList = false
                }
                sb.append("<p></p>")
                continue
            }

            // Handle code blocks
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                if (inCodeBlock) {
                    sb.append("<pre><code>")
                } else {
                    sb.append("</code></pre>")
                }
                continue
            }

            if (inCodeBlock) {
                sb.append(line.replace("<", "&lt;").replace(">", "&gt;"))
                sb.append("\n")
                continue
            }

            // Handle headers
            when {
                line.trim().matches(Regex("#{1}\\s+.*")) -> {
                    sb.append("<h1>").append(line.trim().substring(2)).append("</h1>")
                }

                line.trim().matches(Regex("#{2}\\s+.*")) -> {
                    sb.append("<h2>").append(line.trim().substring(3)).append("</h2>")
                }

                line.trim().matches(Regex("#{3}\\s+.*")) -> {
                    sb.append("<h3>").append(line.trim().substring(4)).append("</h3>")
                }

                line.trim().matches(Regex("#{4}\\s+.*")) -> {
                    sb.append("<h4>").append(line.trim().substring(5)).append("</h4>")
                }
                // Handle headers without spaces (e.g., ####Header)
                line.trim().matches(Regex("#{1}[^#].*")) && !line.trim().startsWith("# ") -> {
                    sb.append("<h1>").append(line.trim().substring(1)).append("</h1>")
                }

                line.trim().matches(Regex("#{2}[^#].*")) && !line.trim().startsWith("## ") -> {
                    sb.append("<h2>").append(line.trim().substring(2)).append("</h2>")
                }

                line.trim().matches(Regex("#{3}[^#].*")) && !line.trim().startsWith("### ") -> {
                    sb.append("<h3>").append(line.trim().substring(3)).append("</h3>")
                }

                line.trim().matches(Regex("#{4}[^#].*")) && !line.trim().startsWith("#### ") -> {
                    sb.append("<h4>").append(line.trim().substring(4)).append("</h4>")
                }
                // Handle lists
                line.trim().startsWith("- ") -> {
                    if (!inList) {
                        sb.append("<ul>")
                        inList = true
                    }
                    sb.append("<li>").append(line.substring(2)).append("</li>")
                }

                line.trim().startsWith("* ") -> {
                    if (!inList) {
                        sb.append("<ul>")
                        inList = true
                    }
                    sb.append("<li>").append(line.substring(2)).append("</li>")
                }

                line.isBlank() -> {
                    if (inList) {
                        sb.append("</ul>")
                        inList = false
                    }
                    sb.append("<p></p>")
                }

                else -> {
                    if (inList) {
                        sb.append("</ul>")
                        inList = false
                    }
                    // Handle links
                    var processedLine = line
                    val linkPattern = "\\[(.*?)]\\((.*?)\\)".toRegex()
                    processedLine = linkPattern.replace(processedLine) {
                        val text = it.groupValues[1]
                        val url = it.groupValues[2]
                        "<a href=\"$url\">$text</a>"
                    }

                    // Handle inline code
                    val codePattern = "`(.*?)`".toRegex()
                    processedLine = codePattern.replace(processedLine) {
                        val code = it.groupValues[1]
                        "<code>$code</code>"
                    }

                    // Handle bold
                    val boldPattern = "\\*\\*(.*?)\\*\\*".toRegex()
                    processedLine = boldPattern.replace(processedLine) {
                        val text = it.groupValues[1]
                        "<strong>$text</strong>"
                    }

                    // Handle emphasis
                    val emphasisPattern = "\\*(.*?)\\*".toRegex()
                    processedLine = emphasisPattern.replace(processedLine) {
                        val text = it.groupValues[1]
                        "<em>$text</em>"
                    }

                    sb.append(processedLine)
                }
            }
            sb.append("\n")
        }

        if (inList) {
            sb.append("</ul>")
        }

        return sb.toString()
    }

    /**
     * Decodes common HTML entities in the markdown
     */
    private fun decodeHtmlEntities(text: String): String {
        var result = text

        // First normalize any HTML entities with spaces
        result = result.replace(Regex("&\\s*#\\s+x\\s*([0-9a-fA-F]+)\\s*;"), "&#x$1;")  // Handle "& #  x20;"
        result = result.replace(Regex("&\\s*#\\s+([0-9]+)\\s*;"), "&#$1;")  // Handle "& # 65;"
        result = result.replace(Regex("&\\s*#x\\s*([0-9a-fA-F]+)\\s*;"), "&#x$1;")  // Handle "& #x20;"

        // Common HTML entities
        val htmlEntities = mapOf(
            "&#x20;" to " ",
            "&#x21;" to "!",
            "&#x22;" to "\"",
            "&#x23;" to "#",
            "&#x24;" to "$",
            "&#x25;" to "%",
            "&#x26;" to "&",
            "&#x27;" to "'",
            "&#x28;" to "(",
            "&#x29;" to ")",
            "&#x2A;" to "*",
            "&#x2B;" to "+",
            "&#x2C;" to ",",
            "&#x2D;" to "-",
            "&#x2E;" to ".",
            "&#x2F;" to "/",
            "&lt;" to "<",
            "&gt;" to ">",
            "&amp;" to "&",
            "&quot;" to "\"",
            "&nbsp;" to " ",
            "&apos;" to "'"
        )

        // Replace HTML entities with their corresponding characters
        for ((entity, char) in htmlEntities) {
            result = result.replace(entity, char)
        }

        // Handle decimal HTML entities like &#65; (which is 'A')
        val decimalPattern = "&#(\\d+);".toRegex()
        result = decimalPattern.replace(result) {
            val decimal = it.groupValues[1].toIntOrNull()
            if (decimal != null && decimal in 32..126) { // Only handle printable ASCII for safety
                decimal.toChar().toString()
            } else {
                it.value
            }
        }

        // Handle hex HTML entities like &#x41; (which is 'A')
        val hexPattern = "&#[xX]([0-9a-fA-F]+);".toRegex()
        result = hexPattern.replace(result) {
            val hex = it.groupValues[1].toIntOrNull(16)
            if (hex != null && hex in 32..126) { // Only handle printable ASCII for safety
                hex.toChar().toString()
            } else {
                it.value
            }
        }

        return result
    }

    /**
     * Filter out GitBook-specific syntax tags from markdown content
     */
    private fun filterGitBookSyntax(markdown: String): String {
        var result = markdown

        // First clean up any spaces in HTML entities to normalize them
        result = result.replace(Regex("&\\s*#\\s+x\\s*([0-9a-fA-F]+)\\s*;"), "&#x$1;")
        result = result.replace(Regex("&\\s*#\\s+([0-9]+)\\s*;"), "&#$1;")
        result = result.replace(Regex("&\\s*#x\\s*([0-9a-fA-F]+)\\s*;"), "&#x$1;")

        // Remove <figure> blocks
        result = result.replace(Regex("<figure>.*?</figure>", RegexOption.DOT_MATCHES_ALL), "")

        // Remove {% hint %} blocks
        result = result.replace(Regex("\\{%\\s*hint.*?%.*?\\{%\\s*endhint\\s*%", RegexOption.DOT_MATCHES_ALL), "")

        // Remove {% tabs %} and {% endtabs %}
        result = result.replace(Regex("\\{%\\s*tabs\\s*%"), "")
        result = result.replace(Regex("\\{%\\s*endtabs\\s*%"), "")

        // Remove {% tab title="..." %} and {% endtab %}
        result = result.replace(Regex("\\{%\\s*tab\\s+title=\"(.*?)\"\\s*%"), "**$1:**")
        result = result.replace(Regex("\\{%\\s*endtab\\s*%"), "")

        // Remove {% code %} and {% endcode %}
        result = result.replace(Regex("\\{%\\s*code\\s*%"), "```")
        result = result.replace(Regex("\\{%\\s*endcode\\s*%"), "```")

        // Remove {% content-ref %} and {% endcontent-ref %}
        result = result.replace(
            Regex(
                "\\{%\\s*content-ref.*?%.*?\\{%\\s*endcontent-ref\\s*%",
                RegexOption.DOT_MATCHES_ALL
            ), ""
        )

        // Remove {% embed %} tags
        result = result.replace(Regex("\\{%\\s*embed.*?%", RegexOption.DOT_MATCHES_ALL), "")

        // Handle {% api-method-parameter %}
        result = result.replace(
            Regex("\\{%\\s*api-method-parameter\\s+name=\"([^\"]+)\"\\s+.*?required=(true|false).*?%"),
            "- **$1** (${if ("$2" == "true") "required" else "optional"}): "
        )

        // Handle misc common GitBook tags
        val tagsToRemove = listOf(
            "api-method-summary", "api-method-description", "api-method-spec", "api-method-request",
            "api-method-path-parameters", "api-method-parameter-description", "api-method-response",
            "api-method-response-example", "api-method"
        )

        for (tag in tagsToRemove) {
            result = result.replace(Regex("\\{%\\s*$tag.*?%"), "")
            result = result.replace(Regex("\\{%\\s*end$tag\\s*%"), "")
        }

        // Fix spaces around headings
        result = result.replace(Regex("(#+)([^\\s#])"), "$1 $2")

        // Clean up multiple newlines
        result = result.replace(Regex("\n{3,}"), "\n\n")

        return result
    }
}