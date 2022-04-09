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
package org.web3j.evm.debugger

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.evaluation.EvaluationMode
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import me.serce.solidity.lang.psi.SolElement
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.vm.MessageFrame
import org.hyperledger.besu.ethereum.vm.OperationTracer
import org.web3j.evm.ExceptionalHaltException
import org.web3j.evm.debugger.mapping.utils.resolveContext
import org.web3j.evm.debugger.ui.SolidityDebuggerEditor
import org.web3j.evm.debugger.frame.SoliditySourcePosition
import org.web3j.evm.debugger.frame.SolidityNamedValue
import org.web3j.evm.debugger.frame.SolidityStackFrame
import org.web3j.evm.debugger.model.DebugCommand
import org.web3j.evm.debugger.model.JumpOpCodeProcessManager
import org.web3j.evm.debugger.model.OpCode
import org.web3j.evm.entity.ContractMapping
import org.web3j.evm.entity.source.SourceFile
import org.web3j.evm.entity.source.SourceMapElement
import org.web3j.evm.utils.SourceMappingUtils


class SolidityDebugTracer(private val debugProcess: Web3jDebugProcess) : OperationTracer {
    private var breakPoints = mutableMapOf<String, MutableSet<Int>>()
    private var lastSelectedLine = 0
    private var runTillNextLine = false
    private var jumpOpCodeManager = JumpOpCodeProcessManager();


    private val commandQueue: BlockingQueue<DebugCommand> = LinkedBlockingDeque()
    private val stackFrames = mutableListOf<SolidityStackFrame>()
    private val UINT256_32: UInt256 = UInt256.valueOf(32)

    @Throws(ExceptionalHaltException::class)
    private fun step(frame: MessageFrame): String {
        debugProcess.consolePrint("OpCode: " + frame.currentOperation.name)

        val (sourceMapElement, sourceFile) = sourceAtMessageFrame(frame)
        val (filePath, contractContent) = sourceFile
        // This takes the body without the pragma So in this case the current execution is at line 3 which is the contract declaration
        val firstSelectedLine =
            contractContent.entries.filter { it.value.selected }.map { it.key }.min() ?: 0
        val lastSelectedLineNumber =
            contractContent.entries.filter { it.value.selected }.map { it.key }.max() ?: 0

        val firstSelectedOffset = contractContent[firstSelectedLine]?.offset ?: 0
        val sb = StringBuilder()

        if (sourceMapElement != null) sb
            .append(
                "Start Line:${contractContent[firstSelectedLine]?.line}, End Line: ${contractContent[lastSelectedLineNumber]?.line}}"
            )

        sb.append("Line $firstSelectedLine: Offset $firstSelectedOffset")

        when (commandQueue.take()) {
            DebugCommand.EXECUTE -> {
                val opCode = frame.currentOperation.name
                var skipFrame = isSkipFrame(frame)

                if(skipFrame){
                    return ""
                } else
                if (runTillNextLine && firstSelectedLine == lastSelectedLine && !OpCode.isJump(opCode)) {
                    debugProcess.suspend(firstSelectedLine)

                } else if (runTillNextLine) {
                    runTillNextLine = false
                    commandQueue.put(DebugCommand.SUSPEND)
                    if (filePath != null) {
                        updateStackFrame(filePath, firstSelectedLine, firstSelectedOffset, frame)
                    }
                    return step(frame)
                } else if (breakPoints.values.any { it.contains(firstSelectedLine) }) {
                    commandQueue.put(DebugCommand.SUSPEND)
                    return step(frame)
                }
            }

            DebugCommand.SUSPEND -> {
                debugProcess.suspend(firstSelectedLine)
                lastSelectedLine = firstSelectedLine
                runTillNextLine = true

                return step(frame)
            }

            DebugCommand.STEP_INTO -> {
                debugProcess.consolePrint("Stepping into..")
            }

            DebugCommand.STEP_OVER -> {
                val opCode = frame.currentOperation.name
                if (OpCode.isJump(opCode)){
                    jumpOpCodeManager.activate()
                }

                debugProcess.consolePrint("Stepping over..")
            }

            DebugCommand.STEP_OUT -> {
                debugProcess.consolePrint("Stepping out..")
            }
        }

        debugProcess.consolePrint(sb.toString())
        return ""
    }

    private fun isSkipFrame(frame: MessageFrame) : Boolean {
        if(jumpOpCodeManager.isActive()){
            val opCode = frame.currentOperation.name
            if (OpCode.isJumpDest(opCode)){
                jumpOpCodeManager.incrementDestCounter()
            }
        }

        return jumpOpCodeManager.isActive()
    }

    fun setBreakpoints(breakpoints: MutableMap<String, MutableSet<Int>>) {
        this.breakPoints = breakpoints
    }

    fun sendCommand(command: DebugCommand) {
        commandQueue.put(command)
    }

    fun getStackFrames(): List<SolidityStackFrame> {
        return stackFrames
    }

    private fun updateStackFrame(filePath: String, line: Int, offset: Int, frame: MessageFrame) {
        val baseDir = debugProcess.getRunConfig().project.basePath
//
        debugProcess.consolePrint("Call for the pointer to update and go to line $line")
        debugProcess.consolePrint("Base directory: $baseDir, FilePath: $filePath")
        // TODO: Change this to show the name of the context
        val stackFrame = SolidityStackFrame(
            debugProcess.session.project,
            "$baseDir/$filePath", line, offset
        )
        resolveFrameContext(stackFrame.sourcePosition as SoliditySourcePosition, stackFrame)

        updateSourcePosition(stackFrame.sourcePosition as SoliditySourcePosition)
        captureMemory(frame).forEach {
            it.let {
                val value = it?.toHexString() + " " + it?.toArray()?.let { it1 -> String(it1) }
                 stackFrame.addValue(SolidityNamedValue("memory", "string", value))
            }
        }
        stackFrames.add(stackFrame)
    }

    private fun captureStack(frame: MessageFrame): Array<Bytes32?> {
        val stackContents = arrayOfNulls<Bytes32>(frame.stackSize())
        for (i in stackContents.indices) {
            // Record stack contents in reverse
            stackContents[i] = frame.getStackItem(stackContents.size - i - 1)
        }
        return stackContents
    }

    private fun captureMemory(frame: MessageFrame): Array<Bytes32?> {
        val memoryContents: Array<Bytes32?> = arrayOfNulls(frame.memoryWordSize().intValue())
        for (i in memoryContents.indices) {
            memoryContents[i] = frame.readMemory(UInt256.valueOf(i * 32L), UINT256_32) as Bytes32?
        }
        return memoryContents
    }

    override fun traceExecution(messageFrame: MessageFrame, executeOperation: OperationTracer.ExecuteOperation?) {
        commandQueue.put(DebugCommand.EXECUTE)
        step(messageFrame)
        executeOperation?.execute()
        if (messageFrame.state != MessageFrame.State.CODE_EXECUTING) {
            runTillNextLine = false
        }
    }

    private fun updateSourcePosition(
        mySourcePosition:
        SoliditySourcePosition
    ) {
        val solidityDebuggerEditor =
            SolidityDebuggerEditor(
                debugProcess.session.project,
                debugProcess.editorsProvider,
                EvaluationMode.EXPRESSION,
                "",
                mySourcePosition
            )
        if (debugProcess.session == null) return
        solidityDebuggerEditor.setSourcePosition(mySourcePosition)
        ApplicationManager.getApplication().runReadAction {
            val psiElementAtPosition = mySourcePosition.getPsiElementAtPosition()
            debugProcess.consolePrint(psiElementAtPosition!!.text)
        }
    }

    private fun resolveFrameContext(sourcePosition: SoliditySourcePosition, stackFrame: SolidityStackFrame) {
        val parent = PsiTreeUtil.getParentOfType(
            sourcePosition.getPsiElementAtPosition(),
            SolElement::class.java
        )
        resolveContext(parent!!, stackFrame)
    }


    /**
     * Given a message frame derive the source code
     */
    private fun sourceAtMessageFrame(
        messageFrame: MessageFrame
    ): Pair<SourceMapElement?, SourceFile> {
        val baseDir = debugProcess.getRunConfig().workingDirectory;
        val metaFile = File("${baseDir}/build/resources/main/solidity")
        val sourceFileBodyTransform = fun(key: Int, value: String): Pair<Int, String> = Pair(key, value)
        val lastSourceFile = SourceFile()
        val byteCodeContractMapping = HashMap<Pair<String, Boolean>, ContractMapping>()

        return SourceMappingUtils.sourceAtMessageFrame(messageFrame,
            metaFile,
            lastSourceFile,
            byteCodeContractMapping,
            sourceFileBodyTransform
        )
    }

}


