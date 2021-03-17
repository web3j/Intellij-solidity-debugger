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
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.evm.Configuration
import org.web3j.evm.EmbeddedWeb3jService
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.RemoteCall
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.reflect.jvm.kotlinFunction

class Web3jDebugProcess constructor(session: XDebugSession) : XDebugProcess(session) {

    private val breakpointHandler = SolidityLineBreakpointHandler(this)
    private val breakpoints = mutableListOf<XLineBreakpoint<*>>()

    private var operationTracer: SolidityDebugTracer = SolidityDebugTracer(this)
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
    }

    private fun setBreakpoints() {
        val breakpointsMap = mutableMapOf<String, MutableSet<Int>>()

        breakpoints.forEach {
            val src = "src/main/solidity/" + it.shortFilePath;
            breakpointsMap.getOrPut(src) { mutableSetOf() }.add(it.line+1) // first XLineBreakpoint is 0
        }

        operationTracer.setBreakpoints(breakpointsMap)
    }

    private fun startWeb3jEmbeddedService() {
        val workingDir = getRunConfig().workingDirectory as String

        val (config, credentials) =
            createWeb3jConfig("8956cf546a960f49ce61ec2bcc892565355a67cb9cd9830bc5a674fb5e2d8e1e")

        ApplicationManager.getApplication().executeOnPooledThread {
            operationTracer = SolidityDebugTracer(this)
            setBreakpoints()

            val web3jService = EmbeddedWeb3jService(config, operationTracer as OperationTracer)
            web3j = Web3j.build(web3jService)

            consolePrint("EmbeddedWeb3jService started ${web3j}")

            val classesDir = File("${workingDir}/build/classes/java/main")
            val cl: ClassLoader = URLClassLoader.newInstance(
                arrayOf(classesDir.toURI().toURL()),
                this.javaClass.classLoader
            )

            val contractClass = cl.loadClass("org.web3j.greeter.Greeter")

            val deploy = contractClass.getMethod(
                "deploy",
                Web3j::class.java,
                Credentials::class.java,
                ContractGasProvider::class.java,
                String::class.java
            )

            val instance = deploy.kotlinFunction?.call(web3j, credentials, DefaultGasProvider(), "Hello!")
                    as RemoteCall<*>

            consolePrint("Deployed Contract ${instance.send()}")
        }
    }

    fun getRunConfig(): SolidityRunConfig {
        return session.runProfile as SolidityRunConfig
    }

    private fun createWeb3jConfig(privateKey: String?): Pair<Configuration, Credentials> {
        val credentials = if (privateKey.isNullOrEmpty()) {
            val workingDir = getRunConfig().workingDirectory + "/"
            val walletFile = "$workingDir${getRunConfig().name}_wallet.json"
            if (!Paths.get(walletFile).exists()) {
                val newWallet = workingDir + WalletUtils
                    .generateNewWalletFile("Password123", File(workingDir))
                File(newWallet).renameTo(File(walletFile))
            }
            WalletUtils.loadCredentials("Password123", walletFile)
        } else {
            Credentials.create(privateKey)
        }
        return Pair(Configuration(Address(credentials.address), 10), credentials)
    }

    override fun stop() {
        println("Stopping debugger.")
        web3j.shutdown()
    }

    fun suspend(lineNumber: Int) {
        ApplicationManager.getApplication().runReadAction {
            val contractFile = getRunConfig().getPersistentData().contractFile as String
            val executionStack = ExecutionStack(operationTracer.getStackFrames())
            val suspendContext = SoliditySuspendContext(executionStack)
            val breakpoint = findBreakpoint(contractFile, lineNumber)
            if (breakpoint != null) {
                session.breakpointReached(breakpoint, null, suspendContext)
            } else {
                session.positionReached(suspendContext)
            }
        }
    }

    private fun findBreakpoint(filePath: String, lineNumber: Int): XLineBreakpoint<out XBreakpointProperties<Any>>? {
        val manager = XDebuggerManager.getInstance(session.project).breakpointManager
        val type: XLineBreakpointType<*>? = XDebuggerUtil.getInstance().findBreakpointType(
            SolidityLineBreakpointType::class.java
        )

        if (type != null) {
            val breakpoints = manager.getBreakpoints(type)
            for (breakpoint in breakpoints) {
                if (breakpoint.fileUrl.contains(filePath) && breakpoint.line == lineNumber - 1) {
                    return breakpoint
                }
            }
        }
        return null
    }

    fun consolePrint(message: String) {
        session.consoleView.print("${message}\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun addBreakpoint(breakpoint: XLineBreakpoint<*>) {
        breakpoints.add(breakpoint)
        setBreakpoints()
    }

    fun removeBreakpoint(breakpoint: XLineBreakpoint<*>) {
        breakpoints.remove(breakpoint)
        setBreakpoints()
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
        operationTracer.sendCommand("stepInto")
        println("step into..")
    }

    override fun startStepOut(context: XSuspendContext?) {
        println("step out..")
        operationTracer.sendCommand("stepOut")
    }

    override fun startStepOver(context: XSuspendContext?) {
        println("step over..")
        operationTracer.sendCommand("stepOver")
    }

}
