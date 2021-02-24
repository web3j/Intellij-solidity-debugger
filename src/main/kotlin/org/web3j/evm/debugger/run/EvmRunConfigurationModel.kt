package org.web3j.evm.debugger.run

import com.intellij.openapi.project.Project
import org.web3j.evm.debugger.configuration.EvmSettingsEditor

class EvmRunConfigurationModel(project: Project) {
    private var listener: EvmSettingsEditor? = null
    private val contracts = emptyList<Any>()
    private var project: Project? = null

    fun setListener(listener: EvmSettingsEditor?) {
        this.listener = listener
    }

    private fun applyTo(configuration: EvmRunConfiguration) {
        configuration.setContractName("")
    }
}