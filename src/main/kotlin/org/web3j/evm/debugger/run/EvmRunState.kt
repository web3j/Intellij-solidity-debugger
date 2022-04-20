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