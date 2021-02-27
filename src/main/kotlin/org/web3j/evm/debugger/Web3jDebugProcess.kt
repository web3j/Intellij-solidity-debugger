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
package org.web3j.evm.debugger

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.io.exists
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import me.serce.solidity.ide.run.SolidityRunConfig

import org.hyperledger.besu.ethereum.vm.OperationTracer
import org.web3j.abi.datatypes.Address
import org.web3j.tx.Contract.deployRemoteCall
import org.web3j.crypto.ContractUtils
import org.web3j.crypto.WalletUtils
import org.web3j.evm.Configuration
import org.web3j.evm.ConsoleDebugTracer
import org.web3j.evm.EmbeddedWeb3jService
import org.web3j.protocol.Web3j
import org.web3j.tx.Contract
import org.web3j.tx.Contract.deploy
import org.web3j.tx.gas.DefaultGasProvider

import java.io.File
import java.nio.file.Paths
import java.net.URLClassLoader

class Web3jDebugProcess constructor(session: XDebugSession) : XDebugProcess(session) {

    private val breakpointHandler = SolidityLineBreakpointHandler(this)
    private val breakpoints = mutableListOf<XLineBreakpoint<*>>()

    private lateinit var operationTracer: OperationTracer
    private lateinit var web3j: Web3j

    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return DebuggerEditorsProvider()
    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>?> {
        return arrayOf(breakpointHandler)
    }

    override fun sessionInitialized() {
        super.sessionInitialized()
        startWeb3jEmbeddedService()
        debug()
    }

    private fun startWeb3jEmbeddedService(){
        val workingDir = getRunConfig().workingDirectory as String
        var walletFile = "$workingDir/${getRunConfig().name}_wallet.json"

        if (!Paths.get(walletFile).exists()) {
            val newWallet = "$workingDir/" + WalletUtils.generateNewWalletFile("Password123", File(workingDir))
            File(newWallet).renameTo(File(walletFile))
        }

        consolePrint(walletFile)
        val credentials = WalletUtils.loadCredentials("Password123", walletFile)
        val configuration = Configuration(Address(credentials.address), 10)
        operationTracer = ConsoleDebugTracer()
        val web3jService = EmbeddedWeb3jService(configuration, operationTracer)
        web3j = Web3j.build(web3jService)
        println("EmbeddedWeb3jService started ${web3j}")

        //val classesLoc = File("/web3j-evmexample/build/classes/java/main")
        //val cl: ClassLoader = URLClassLoader.newInstance(arrayOf(classesLoc.toURI().toURL()),
          //  this.javaClass.classLoader)
        //val contract = cl.loadClass("org.web3j.regreeter.Regreeter") as <* : Contract>
        //val binary = ""
        //val encodedConstrcutro = ""
        //deployRemoteCall(contract, web3j, credentials, DefaultGasProvider(), binary, encodedConstrcutro )

    }

    private fun getRunConfig(): SolidityRunConfig {
        val runConfig = session.runProfile as SolidityRunConfig
        println("Working directory: ${runConfig.workingDirectory}")
        consolePrint(
            "Contract Name/File:" +
                    " ${runConfig.getPersistentData().contractName} \n" +
                    " ${runConfig.getPersistentData().contractFile}"
        )
        return runConfig
    }

    private fun debug() {
        ApplicationManager.getApplication().executeOnPooledThread {
            suspend()
        }
    }

    override fun stop() {
        println("Stopping debugger.")
        //web3j.shutdown()
    }

    private fun suspend() {
        ApplicationManager.getApplication().runReadAction {
            val contractFile = getRunConfig().getPersistentData().contractFile as String
            val suspendContext = SoliditySuspendContext(ExecutionStack(listOf(SolidityStackFrame())))
            consolePrint("Current stack ${session.currentStackFrame}")
            val breakpoint = findBreakpoint(contractFile,5)
            if (breakpoint != null) {
                session.breakpointReached(breakpoint, null, suspendContext)
            } else {
                session.positionReached(suspendContext)
            }
        }
    }

    private fun findBreakpoint(scriptPath: String, lineNumber: Int): XLineBreakpoint<out XBreakpointProperties<Any>>? {
        val manager = XDebuggerManager.getInstance(session.project).breakpointManager
        val type: XLineBreakpointType<*>? = XDebuggerUtil.getInstance().findBreakpointType(
            SolidityLineBreakpointType::class.java
        )

        if (type != null) {
            val breakpoints = manager.getBreakpoints(type)
            for (breakpoint in breakpoints) {
                if (breakpoint.fileUrl.contains(scriptPath) && breakpoint.line == lineNumber - 1) {
                    return breakpoint
                }
            }
        }
        return null
    }

    private fun consolePrint(message: String) {
       session.consoleView.print(message, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun addBreakpoint(breakpoint: XLineBreakpoint<*>) {
        println("Add breakpoint $breakpoint $breakpoints")
        breakpoints.add(breakpoint)
    }

    fun removeBreakpoint(breakpoint: XLineBreakpoint<*>) {
        println("Remove breakpoint $breakpoint")
        breakpoints.add(breakpoint)
    }

    override fun resume(context: XSuspendContext?) {
        println("Resume: $context")
        if (context != null) {
            context.activeExecutionStack as ExecutionStack
        }
    }

    override fun startPausing() {
       println("start Pausing..")
    }

    override fun startStepInto(context: XSuspendContext?) {
        println("step into..")
        debug()
    }

    override fun startStepOut(context: XSuspendContext?) {
        println("step out..")
        debug()
    }

    override fun startStepOver(context: XSuspendContext?) {
        println("step over..")
        debug()
    }

}
