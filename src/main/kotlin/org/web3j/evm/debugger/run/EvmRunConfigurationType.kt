package org.web3j.evm.debugger.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import javax.swing.Icon

class EvmRunConfigurationType :
    ConfigurationTypeBase(
        "EVM_RUN_CONFIGURATION",
        "Evm",
        "Debug Solidity smart contracts",
        AllIcons.General.Information
    ) {
    init {
        addFactory(configurationFactory())
    }

    private fun configurationFactory(): ConfigurationFactory {
        return object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(p: Project): RunConfiguration {
                val configurationModule = EvmRunConfigurationModule(p)
                return if (hasJavaSupport) EvmRunConfiguration(configurationModule, this)
                else throw RuntimeException("Not a valid configuration")
            }

            override fun getIcon(): Icon {
                return AllIcons.General.Information
            }

            override fun isApplicable(project: Project): Boolean {
                return hasJavaSupport
            }
        }
    }
}

val hasJavaSupport = try {
    Class.forName("com.intellij.execution.CommonJavaRunConfigurationParameters")
    true
} catch (e: ClassNotFoundException) {
    false
}