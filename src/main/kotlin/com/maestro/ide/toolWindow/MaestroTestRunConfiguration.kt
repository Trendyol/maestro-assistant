package com.maestro.ide.toolWindow

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Run configuration for Maestro tests
 */
class MaestroTestRunConfiguration(project: Project, name: String) 
    : RunConfigurationBase<Any>(project, MaestroTestConfigurationType.getInstance().factory, name) {

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        // Create an empty settings editor
        return object : SettingsEditor<RunConfiguration>() {
            override fun resetEditorFrom(runConfiguration: RunConfiguration) {}
            override fun applyEditorTo(runConfiguration: RunConfiguration) {}
            override fun createEditor(): JComponent = JPanel()
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null
}

/**
 * Configuration type for Maestro tests
 */
class MaestroTestConfigurationType : ConfigurationType {
    val factory: ConfigurationFactory = object : ConfigurationFactory(this) {
        override fun getId() = "MaestroTestRunner"
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return MaestroTestRunConfiguration(project, "Maestro Test")
        }
    }
   
    override fun getDisplayName() = "Maestro Tests"
    override fun getConfigurationTypeDescription() = "Maestro test configuration"
    override fun getId() = "MaestroTests"
    override fun getIcon(): Icon = AllIcons.RunConfigurations.TestState.Run
    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)
   
    companion object {
        private val instance = MaestroTestConfigurationType()
       
        fun getInstance() = instance
    }
}