package org.web3j.evm.debugger.run

import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.project.Project

class EvmRunConfigurationModel(private val project: Project) {
    private var listener: EvmSettingsEditorPanel? = null

    fun setListener(listener: EvmSettingsEditorPanel) {
        this.listener = listener
    }

    fun apply(configuration: EvmRunConfiguration) {
        val shouldUpdateName = configuration.isGeneratedName
        if (shouldUpdateName && !JavaExecutionUtil.isNewName(configuration.name)) {
            configuration.setGeneratedName()
        }
    }


    fun reset(configuration: EvmRunConfiguration) {
        val data = configuration.getPersistentData()
        data.getContractName()
        data.getWalletPath()
        data.getPrivateKey()
        data.getWalletPassword()
        data.getMethodWrapperName()
        data.getContractWrapperName()

    }

}