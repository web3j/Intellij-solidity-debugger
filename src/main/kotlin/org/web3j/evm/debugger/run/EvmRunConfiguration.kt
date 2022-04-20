/*
 * Copyright 2019 Web3 Labs Ltd.
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

import com.intellij.diagnostic.logging.LogConfigurationPanel
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.ExternalizablePath
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.util.xmlb.XmlSerializer
import me.serce.solidity.ide.run.SearchUtils
import me.serce.solidity.lang.psi.SolContractDefinition
import org.jdom.Element
import org.web3j.abi.datatypes.Uint

abstract class EvmRunConfigurationBase(
    factory: ConfigurationFactory,
    configurationModule: EvmRunConfigurationModule
) :
    ModuleBasedConfiguration<EvmRunConfigurationModule, Element>(configurationModule, factory),
    RunConfigurationWithSuppressedDefaultDebugAction {
}


class EvmRunConfiguration(
    configurationModule: EvmRunConfigurationModule,
    factory: ConfigurationFactory
) : EvmRunConfigurationBase(factory, configurationModule), CommonJavaRunConfigurationParameters {


    private var myData = Data()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val state = EvmRunState(environment, this)
        state.consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        return state
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val group = SettingsEditorGroup<EvmRunConfiguration>()
        group.addEditor(
            ExecutionBundle.message("run.configuration.configuration.tab.title", *arrayOfNulls(0)),
            EvmSettingsEditorPanel(this.project)
        )
        group.addEditor(ExecutionBundle.message("logs.tab.title", *arrayOfNulls(0)), LogConfigurationPanel())
        return EvmSettingsEditorPanel(project)
    }


    override fun setProgramParameters(value: String?) {
        myData.programParameters = value
    }

    override fun getProgramParameters(): String? {
        return myData.programParameters
    }

    override fun setWorkingDirectory(workingDir: String?) {
        if (workingDir.isNullOrEmpty()) {
            myData.setWorkingDirectory(this.project.basePath)
        } else {
            myData.setWorkingDirectory(workingDir)
        }
    }

    override fun getWorkingDirectory(): String? {
        return myData.getWorkingDirectory()
    }

    override fun setEnvs(envs: MutableMap<String, String>) {
        myData.envs.clear()
        myData.envs.putAll(envs)
    }

    override fun getEnvs(): MutableMap<String, String> {
        return myData.envs
    }

    override fun setPassParentEnvs(passParentEnvs: Boolean) {
    }

    override fun isPassParentEnvs(): Boolean {
        return true
    }

    override fun isAlternativeJrePathEnabled(): Boolean {
        return myData.ajreEnabled
    }

    override fun getAlternativeJrePath(): String? {
        return myData.ajre
    }

    override fun setVMParameters(vmParamenters: String?) {
        myData.vmParameters = vmParamenters
    }

    override fun getVMParameters(): String? {
        return myData.vmParameters
    }

    override fun setAlternativeJrePathEnabled(enabled: Boolean) {
        myData.ajreEnabled = enabled
    }

    override fun setAlternativeJrePath(ajre: String?) {
        myData.ajre = ajre
    }

    override fun getRunClass(): String? {
        throw UnsupportedOperationException("Don't know if this needs to be used yet")
    }

    override fun getPackage(): String? {
        throw UnsupportedOperationException("Don't know if this needs to be used yet")
    }

    fun getPersistentData(): Data {
        return myData
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JavaRunConfigurationExtensionManager.instance.writeExternal(this, element)
        XmlSerializer.serializeInto(this, element)
        XmlSerializer.serializeInto(myData, element)
        writeModule(element)
        EnvironmentVariablesComponent.writeExternal(element, envs)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        JavaRunConfigurationExtensionManager.instance.readExternal(this, element)
        XmlSerializer.deserializeInto(this, element)
        XmlSerializer.deserializeInto(myData, element)
        readModule(element)
        EnvironmentVariablesComponent.readExternal(element, envs)
    }

    data class Data(val u: Uint? = null) : Cloneable {

        @JvmField
        var contractWrapperName: String? = null

        @JvmField
        var methodWrapperName: String? = null

        @JvmField
        var walletPath: String? = null

        @JvmField
        var walletPassword: String? = null

        @JvmField
        var privateKey: String? = null

        @JvmField
        var contractName: String? = null

        @JvmField
        var contractFile: String? = null

        @JvmField
        var vmParameters: String? = null

        @JvmField
        var programParameters: String? = null

        @JvmField
        var workingDirectory: String? = null

        @JvmField
        var envs: MutableMap<String, String> = LinkedHashMap()

        @JvmField
        var ajreEnabled = false

        @JvmField
        var ajre: String? = null


        fun setContractWrapperName(wrapper: String) {
            this.contractWrapperName = wrapper
        }

        fun getContractWrapperName(): String = contractWrapperName ?: ""

        fun getMethodWrapperName(): String = methodWrapperName ?: ""

        fun setMethodWrapperName(wrapper: String) {
            this.methodWrapperName = wrapper
        }

        fun getWalletPath(): String = walletPath ?: ""

        fun setWalletPath(path: String) {
            this.walletPath = path
        }

        fun getWalletPassword(): String = walletPassword ?: ""

        fun setWalletPassword(password: String) {
            this.walletPassword = password
        }

        fun getPrivateKey(): String = privateKey ?: ""

        fun setPrivateKey(privateKey: String) {
            this.privateKey = privateKey
        }

        fun getContractName(): String = contractName ?: ""

        fun setContract(contract: SolContractDefinition) {
            this.contractName = contract.name
            this.contractFile = contract.containingFile.virtualFile.path

        }

        fun getWorkingDirectory(): String = ExternalizablePath.localPathValue(workingDirectory)

        fun setWorkingDirectory(value: String?) {
            workingDirectory = ExternalizablePath.urlValue(value)
        }

        public override fun clone(): Data {
            try {
                val data = super.clone() as Data
                data.envs = LinkedHashMap(envs)
                return data
            } catch (e: CloneNotSupportedException) {
                throw RuntimeException(e)
            }
        }
    }

    override fun checkConfiguration() {
        if (configurationModule.module == null) {
            throw RuntimeConfigurationError("Module is not specified")
        }
        JavaParametersUtil.checkAlternativeJRE(this)
        val configurationModule = configurationModule
        val psiContract = SearchUtils.findContract(
            myData.getContractName(),
            configurationModule.project,
            configurationModule.module
        )
            ?: throw RuntimeConfigurationError("Can't find contract ${myData.contractName} within module ${configurationModule.moduleName}")
        if (psiContract.containingFile.virtualFile.path != myData.contractFile) {
            throw RuntimeConfigurationError("Can't find contract ${myData.contractName} within file ${myData.contractFile}")
        }
        ProgramParametersUtil.checkWorkingDirectoryExist(this, project, configurationModule.module)
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)
    }

    override fun getValidModules(): MutableCollection<Module> {
        throw ExecutionException("Current IDE platform does not support execution of Solidity contracts")
    }
}
