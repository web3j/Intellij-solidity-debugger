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