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
package org.web3j.evm.debugger.utils

import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Paths
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.evm.Configuration
import org.web3j.evm.debugger.run.EvmRunConfiguration

object Web3jWalletUtils {

    fun createCredentialsConfigFromPK(privateKey: String): Pair<Configuration, Credentials> {
        val credentials = Credentials.create(privateKey)
        val configuration = Configuration(Address(credentials.address), 10)
        return Pair(configuration, credentials)
    }

    fun createCredentialsConfigFromWallet(evmRunConfiguration: EvmRunConfiguration): Pair<Configuration, Credentials>{
        val workingDir = evmRunConfiguration.workingDirectory + "/"
        val walletFile = "$workingDir${evmRunConfiguration.name}_wallet.json"
        if (!Paths.get(walletFile).exists()) {
            val newWallet = workingDir + WalletUtils
                .generateNewWalletFile("Password123", File(workingDir))
            File(newWallet).renameTo(File(walletFile))
        }

        val credentials = WalletUtils.loadCredentials("Password123", walletFile)

        return Pair(Configuration(Address(credentials.address), 10), credentials)
    }



}