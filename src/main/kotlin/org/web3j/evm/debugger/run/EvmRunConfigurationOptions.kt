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

