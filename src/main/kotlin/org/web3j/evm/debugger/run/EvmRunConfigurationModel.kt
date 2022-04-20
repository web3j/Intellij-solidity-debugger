/*
 * Copyright 2021 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.evm.debugger.run

import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.project.Project
import me.serce.solidity.ide.run.SearchUtils
import me.serce.solidity.lang.psi.SolContractDefinition

class EvmRunConfigurationModel(private val project: Project) {
    private var listener: EvmSettingsEditorPanel? = null
    fun setListener(listener: EvmSettingsEditorPanel) {
        this.listener = listener
    }

    fun apply(configuration: EvmRunConfiguration, evmSettingsEditorPanel: EvmSettingsEditorPanel) {
        val shouldUpdateName = configuration.isGeneratedName
        val data = configuration.getPersistentData()
        val contract = findPsiContract(evmSettingsEditorPanel.solidityContract.component.text, project)
        if (contract != null) {
            data.setContract(contract)
        }
        if (shouldUpdateName && !JavaExecutionUtil.isNewName(configuration.name)) {
            configuration.setGeneratedName()
        }
        data.setContractWrapperName(evmSettingsEditorPanel.contractWrapper.component.text)
        data.setMethodWrapperName(evmSettingsEditorPanel.wrappedMethod.component.text)
        data.setWalletPath(evmSettingsEditorPanel.walletPath.component.text)
        data.setWalletPassword(evmSettingsEditorPanel.walletPassword.component.text)
        data.setPrivateKey(evmSettingsEditorPanel.privateKey.component.text)
    }


    private fun findPsiContract(contractName: String, myProject: Project): SolContractDefinition? {
        return SearchUtils.findContract(contractName, myProject)
    }


    fun reset(configuration: EvmRunConfiguration, evmSettingsEditorPanel: EvmSettingsEditorPanel) {
        val data = configuration.getPersistentData()
        evmSettingsEditorPanel.contractWrapper.component.text = data.getContractWrapperName()
        evmSettingsEditorPanel.solidityContract.component.text = data.getContractName()
        evmSettingsEditorPanel.wrappedMethod.component.text = data.getMethodWrapperName()
        evmSettingsEditorPanel.walletPath.component.text = data.getWalletPath()
        evmSettingsEditorPanel.walletPassword.component.text = data.getWalletPassword()
        evmSettingsEditorPanel.privateKey.component.text = data.getPrivateKey()

    }

}