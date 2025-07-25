import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.changelog)
}
val pluginGroup: String by project.properties
val currentPluginVersion: String by project.properties

group = pluginGroup
version = currentPluginVersion

repositories {
    google()
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(17)
}

val pluginSinceBuild: String by project.properties

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = pluginSinceBuild
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            ide(IntelliJPlatformType.AndroidStudio, "2024.3.1.13") // Meerkat Feature Drop 2024.3.2
            ide(IntelliJPlatformType.AndroidStudio, "2024.3.2.14") // Meerkat Feature Drop 2024.3.2
            ide(IntelliJPlatformType.AndroidStudio, "2025.1.1.10") // Narwhal 2025.1.1 Canary 10
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

dependencies {
    intellijPlatform {
        androidStudio("2024.3.2.14")
        bundledPlugins(
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "org.jetbrains.plugins.yaml",
            "org.jetbrains.plugins.terminal"
        )

        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
    implementation(libs.kotlinxSerialization)
    implementation(libs.okhttp)
    implementation(libs.markdown)
}

changelog {
    val pluginRepositoryUrl: String by project.properties
    version = currentPluginVersion
    repositoryUrl = pluginRepositoryUrl
}

tasks {
    wrapper {
        val gradleVersion: String by project.properties
        this.gradleVersion = gradleVersion
    }

    patchPluginXml {
        version = currentPluginVersion
        sinceBuild = pluginSinceBuild

        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            val flavour = CommonMarkFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(it)
            val html = HtmlGenerator(it, parsedTree, flavour).generateHtml()

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                html
            }
        }

        val changelog = project.changelog
        val changeNotesValue = pluginVersion.map { version ->
            with(changelog) {
                try {
                    renderItem(
                        (getOrNull(version) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                } catch (e: Exception) {
                    "<p>Initial release</p>"
                }
            }
        }
        changeNotes.set(changeNotesValue)
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    buildPlugin {
        archiveBaseName.set("maestro-assistant")
        archiveVersion.set(currentPluginVersion)
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        channels.set(listOf("default"))
    }
}
