package org.web3j.evm.debugger.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

class EvmRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String {
        return "EVM"
    }

    override fun getConfigurationTypeDescription(): String {
        return "EVM configuration type"
    }

    override fun getIcon(): Icon {
        return AllIcons.General.Information
    }

    override fun getId(): String {
        return "EVM_RUN_CONFIGURATION"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(EvmRunConfigurationFactory(this))
    }
}