package org.web3j.evm.debugger.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.application.BaseJavaApplicationCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.isDirectory
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.run.EthereumRunner
import java.io.File
import java.nio.file.Paths

class EvmRunState(environment: ExecutionEnvironment, configuration: EvmRunConfiguration) :
    BaseJavaApplicationCommandLineState<EvmRunConfiguration>(environment, configuration) {

    override fun createJavaParameters(): JavaParameters {
        try {
            configuration.checkConfiguration()
        } catch (e: RuntimeConfigurationError) {
            throw ExecutionException(e)
        }
        val params = JavaParameters()
        val jreHome = if (myConfiguration.isAlternativeJrePathEnabled) myConfiguration.alternativeJrePath else null
        params.jdk = JavaParametersUtil.createProjectJdk(myConfiguration.project, jreHome)
        setupJavaParameters(params)
        params.mainClass = EthereumRunner::class.qualifiedName
        params.configureByModule(configuration.configurationModule.module, JavaParameters.JDK_AND_CLASSES)
        params.classPath.add(PathUtil.getJarPathForClass(EthereumRunner::class.java))
        var evmPath = SoliditySettings.instance.pathToEvm
        if (Paths.get(evmPath).isDirectory()) {
            evmPath += File.separator + "*"
        }
        params.classPath.add(evmPath)
        if (!SoliditySettings.instance.pathToDb.isBlank()) {
            params.vmParametersList.add("-Devm.database.dir=${SoliditySettings.instance.pathToDb}")
        }
        return params
    }
}