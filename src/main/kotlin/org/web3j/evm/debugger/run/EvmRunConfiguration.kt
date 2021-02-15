package org.web3j.evm.debugger.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.web3j.evm.debugger.configuration.EvmSettingsEditor

class EvmRunConfiguration(project: Project, factory: ConfigurationFactory, name: String?) :
    LocatableConfigurationBase<EvmRunConfigurationOptions>(project, factory, name) {

    fun getContractName(): String? {
        return options.getContract()
    }

    fun setContractName(scriptName: String?) {
        options.setContract(scriptName)
    }

    fun getMethodName(): String? {
        return options.getMethod()
    }

    fun setMethodName(scriptName: String?) {
        options.setMethod(scriptName)
    }

    fun getWalletPath(): String? {
        return options.getWalletPath()
    }

    fun setWalletPath(scriptName: String?) {
        options.setWalletPath(scriptName)
    }

    fun getWalletPassword(): String? {
        return options.getWalletPassword()
    }

    fun setWalletPassword(scriptName: String?) {
        options.setWalletPassword(scriptName)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @kotlin.jvm.Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val commandLine =
                    GeneralCommandLine(
                        options.contract.name,
                        options.method.name,
                        options.walletPath.name,
                        options.walletPassword.name
                    )
                val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return EvmSettingsEditor(project)
    }

    override fun getOptions(): EvmRunConfigurationOptions {
        return super.getOptions() as EvmRunConfigurationOptions
    }


}