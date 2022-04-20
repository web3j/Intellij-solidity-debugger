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

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class EvmRunConfigurationOptions : LocatableRunConfigurationOptions() {
    val contract: StoredProperty<String?> = string("").provideDelegate(this, "contract")
    val method: StoredProperty<String?> = string("").provideDelegate(this, "method")
    val walletPath: StoredProperty<String?> =
        string("").provideDelegate(this, "walletPath")
    val walletPassword: StoredProperty<String?> =
        string("").provideDelegate(this, "walletPassword")

    fun getContract(): String? {
        return contract.getValue(this)
    }

    fun getMethod(): String? {
        return method.getValue(this)
    }

    fun getWalletPath(): String? {
        return walletPath.getValue(this)
    }

    fun getWalletPassword(): String? {
        return walletPassword.getValue(this)
    }

    fun setContract(value: String?) {
        contract.setValue(this, value)
    }

    fun setMethod(value: String?) {
        method.setValue(this, value)
    }

    fun setWalletPath(value: String?) {
        walletPath.setValue(this, value)
    }

    fun setWalletPassword(value: String?) {
        walletPassword.setValue(this, value)
    }
}

