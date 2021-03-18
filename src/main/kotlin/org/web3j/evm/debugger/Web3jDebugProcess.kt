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
import com.intellij.openapi.vfs.LocalFileSystem
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
import com.intellij.xdebugger.ui.XDebugTabLayouter
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.evm.Configuration
import org.web3j.evm.EmbeddedWeb3jService
import org.web3j.evm.debugger.run.EvmRunConfiguration
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.RemoteCall
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider
import java.io.BufferedReader
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.reflect.jvm.kotlinFunction

class Web3jDebugProcess constructor(session: XDebugSession) : XDebugProcess(session) {

    private val breakpointHandler = SolidityLineBreakpointHandler(this)
    private val breakpoints = mutableListOf<XLineBreakpoint<*>>()
    private var operationTracer = SolidityDebugTracer()
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

    private fun startWeb3jEmbeddedService() {
        val evmRunConfig = getRunConfig()
        val credentials = resolveCredentials()
        val configuration = Configuration(Address(credentials.address), 10)

        val web3jService = EmbeddedWeb3jService(configuration, operationTracer)
        web3j = Web3j.build(web3jService)
        consolePrint("EmbeddedWeb3jService started ${web3j}")
        val classesDir = File("/home/alexandrou/Documents/dev/web3j-evmexample/build/classes/java/main")
        val cl: ClassLoader = URLClassLoader.newInstance(
            arrayOf(classesDir.toURI().toURL()),
            this.javaClass.classLoader
        )
        val contractClass = cl.loadClass(evmRunConfig.getPersistentData().contractWrapperName)
        val _deploy = contractClass.getMethod(
            "deploy",
            Web3j::class.java,
            Credentials::class.java,
            ContractGasProvider::class.java,
            String::class.java
        )
        var instance = _deploy?.kotlinFunction?.call(web3j, credentials, DefaultGasProvider(), "Hello!")
                as RemoteCall<*>
        consolePrint("Deployed Contract ${instance.send()}")
        operationTracer.parseBreakPointOption("break list")

    }


    private fun resolveCredentials(): Credentials {
        val evmRunConfig = getRunConfig()
        var walletFile = "${evmRunConfig.getPersistentData().workingDirectory}/${getRunConfig().name}_wallet.json"
        if (!evmRunConfig.getPersistentData().walletPath.isNullOrBlank()) {
            return WalletUtils.loadCredentials(
                evmRunConfig.getPersistentData().walletPassword,
                evmRunConfig.getPersistentData().walletPath
            )
        } else if (!evmRunConfig.getPersistentData().privateKey.isNullOrBlank()) {
            return Credentials.create(evmRunConfig.getPersistentData().getPrivateKey())
        } else {
            if (!Paths.get(walletFile).exists()) {
                val generatedWallet = WalletUtils
                    .generateNewWalletFile("Password123", File(evmRunConfig.getPersistentData().workingDirectory!!))
                val newWallet = "${evmRunConfig.getPersistentData().workingDirectory}/" + generatedWallet
                File(newWallet).renameTo(File(walletFile))
                return WalletUtils.loadCredentials("Password123", walletFile)
            }
            return WalletUtils.loadCredentials("Password123", walletFile)
        }
        TODO("Make this more readable")
    }

    private fun getRunConfig(): EvmRunConfiguration {
        val runConfig = session.runProfile as EvmRunConfiguration
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
            val reader = BufferedReader(operationTracer.getOutputAsStream().reader())
            suspend()
            val content = StringBuilder()
            reader.use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    content.append(line)
                    line = reader.readLine()
                    consolePrint(line)
                }
            }
        }
    }

    override fun stop() {
        println("Stopping debugger.")
        //web3j.shutdown()
    }

    private fun suspend() {
        ApplicationManager.getApplication().runReadAction {
            val contractFile = getRunConfig().getPersistentData().contractFile as String
            val virtualContractFile =
                LocalFileSystem.getInstance().findFileByIoFile(File(getRunConfig().getPersistentData().contractFile!!))
            val messageFrame = operationTracer.getMessageFrame()
            val suspendContext =
                SoliditySuspendContext(
                    ExecutionStack(
                        listOf(
                            SolidityStackFrame(
                                virtualContractFile,
                                5
                            )
                        )
                    )
                )

            consolePrint("Current stack ${session.currentStackFrame}")
            val breakpoint = findBreakpoint(contractFile, 5)
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
        session.consoleView.print("${message}\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun addBreakpoint(breakpoint: XLineBreakpoint<*>) {
        println("Add breakpoint $breakpoint $breakpoints")
        val fileName = breakpoint.shortFilePath.subSequence(0, breakpoint.shortFilePath.length - 4).toString()
        operationTracer.getBreakPointMap().getOrPut(fileName) { mutableSetOf() }.add(breakpoint.line)
        breakpoints.add(breakpoint)
    }

    fun removeBreakpoint(breakpoint: XLineBreakpoint<*>) {
        println("Remove breakpoint $breakpoint")
        val fileName = breakpoint.shortFilePath.subSequence(0, breakpoint.shortFilePath.length - 4).toString()
        operationTracer.getBreakPointMap().remove(fileName, mutableSetOf(breakpoint.line))
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
        session.debugProcess.logStack(context!!, this.session)
        // session.setCurrentStackFrame()
        debug()
    }

    override fun createTabLayouter(): XDebugTabLayouter {
        return super.createTabLayouter()
    }


}
