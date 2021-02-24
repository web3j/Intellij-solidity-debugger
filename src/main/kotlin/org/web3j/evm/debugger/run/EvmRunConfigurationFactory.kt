package org.web3j.evm.debugger.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class EvmRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    private val FACTORY_NAME: String = "EVM configuration factory"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return EvmRunConfiguration(project, this, "EVM")
    }

    override fun getId(): String {
        return "EVM_RUN_CONFIGURATION"
    }

    override fun getOptionsClass(): Class<out BaseState> {
        return EvmRunConfigurationOptions::class.java
    }

    override fun getName(): String {
        return FACTORY_NAME
    }
}