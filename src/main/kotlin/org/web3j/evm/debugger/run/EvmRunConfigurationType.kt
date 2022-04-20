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

            override fun getId(): String = super.getName()
        }
    }
}

val hasJavaSupport = try {
    Class.forName("com.intellij.execution.CommonJavaRunConfigurationParameters")
    true
} catch (e: ClassNotFoundException) {
    false
}