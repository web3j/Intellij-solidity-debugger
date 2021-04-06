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

import com.beust.klaxon.Klaxon
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.evaluation.EvaluationMode
import me.serce.solidity.lang.psi.SolElement
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256
import org.hyperledger.besu.ethereum.vm.MessageFrame
import org.hyperledger.besu.ethereum.vm.OperationTracer
import org.web3j.evm.ExceptionalHaltException
import org.web3j.evm.debugger.mapping.utils.resolveContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.min

data class ContractMeta(val contracts: Map<String, Map<String, String>>, val sourceList: List<String>)

data class ContractMapping(
    val idxSource: Map<Int, SolidityContract>,
    val pcSourceMappings: Map<Int, SourceMapElement>
)

data class SolidityContract(
    val filePath: String? = null,
    val contractContent: SortedMap<Int, ContractLine> = Collections.emptySortedMap()
)

data class ContractLine(val line: String, val selected: Boolean = false, val offset: Int = 0)

data class SourceMapElement(
    val sourceFileByteOffset: Int = 0,
    val lengthOfSourceRange: Int = 0,
    val sourceIndex: Int = 0,
    val jumpType: String = ""
)
// TODO: refactor this class, separate debug actions from solidity source code mapping
class SolidityDebugTracer(private val debugProcess: Web3jDebugProcess) : OperationTracer {
    private var breakPoints = mutableMapOf<String, MutableSet<Int>>()
    private var runTillNextLine = false
    private var lastSelectedLine = 0
    private val commandQueue: BlockingQueue<String> = LinkedBlockingDeque()
    private val stackFrames = mutableListOf<SolidityStackFrame>()
    private val UINT256_32: UInt256 = UInt256.valueOf(32)

    fun setBreakpoints(breakpoints: MutableMap<String, MutableSet<Int>>) {
        this.breakPoints = breakpoints
    }

    @Throws(ExceptionalHaltException::class)
    private fun step(frame: MessageFrame): String {
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
            "execute" -> {
                if (runTillNextLine && firstSelectedLine == lastSelectedLine) {
                    debugProcess.suspend(firstSelectedLine)

                } else if (runTillNextLine) {
                    runTillNextLine = false
                    commandQueue.put("suspend")
                    if (filePath != null) {
                        updateStackFrame("" + filePath, firstSelectedLine, firstSelectedOffset, frame)
                    }
                    return step(frame)
                } else if (breakPoints.values.any { it.contains(firstSelectedLine) }) {
                    commandQueue.put("suspend")
                    return step(frame)
                }
            }
            "suspend" -> {
                debugProcess.suspend(firstSelectedLine)
                debugProcess
                lastSelectedLine = firstSelectedLine
                runTillNextLine = true

                return step(frame)
            }
            "stepOver", "stepInto", "stepOut" -> {
                debugProcess.consolePrint("Stepping..")
            }
        }
        debugProcess.consolePrint(sb.toString())
        return ""
    }

    fun sendCommand(command: String) {
        commandQueue.put(command)
    }

    fun getStackFrames(): List<SolidityStackFrame> {
        return stackFrames
    }

    private fun updateStackFrame(filePath: String, line: Int, offset: Int, frame: MessageFrame) {
        val baseDir = debugProcess.getRunConfig().project.basePath

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
                 stackFrame.addValue(SolidityValue("memory", "string", value))
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
        commandQueue.put("execute")
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


    private fun sourceRange(contractContent: SortedMap<Int, ContractLine>, from: Int, to: Int): SortedMap<Int, String> {
        return contractContent.entries.fold(Pair(0, TreeMap<Int, String>())) { acc, entry ->
            val subsection = entry
                .value
                .line
                .withIndex()
                .filter { acc.first + it.index in from..to }
                .map { it.value }
                .joinToString(separator = "")
            val accMin = acc.first
            val accMax = acc.first + entry.value.line.length
            val overlap = accMin in from..to || accMax in from..to || from in accMin..accMax || to in accMin..accMax
            if (overlap) acc.second[entry.key] = subsection

            return@fold Pair(acc.first + entry.value.line.length + 1, acc.second)
        }.second
    }

    private fun toUnPrefixedString(bytes: org.apache.tuweni.bytes.Bytes): String {
        val prefixedHex = bytes.toString()
        return if (prefixedHex.startsWith("0x")) prefixedHex.substring(2) else prefixedHex
    }

    private fun findSourceNear(idxSource: Map<Int, SolidityContract>, sourceMapElement: SourceMapElement): SolidityContract {
        val sourceFile = idxSource[sourceMapElement.sourceIndex] ?: return SolidityContract()
        val sourceContent = sourceFile.contractContent
        val sourceLength = sourceSize(sourceContent)

        val from = sourceMapElement.sourceFileByteOffset
        val to = from + sourceMapElement.lengthOfSourceRange

        val head = sourceRange(sourceContent, 0, from - 1)

        val body = sourceRange(sourceContent, from, to - 1).map {
            Pair(it.key, it.value)
        }.toMap(TreeMap())

        val tail = sourceRange(sourceContent, to, sourceLength)

        val subsection = TreeMap<Int, ContractLine>()

        head.entries.reversed().take(2).forEach { (lineNumber, newLine) ->
            subsection[lineNumber] = ContractLine(newLine)
        }

        body.forEach { (lineNumber, newLine) ->
            subsection.compute(lineNumber) { _, sourceLine ->
                if (sourceLine == null) {
                    ContractLine(newLine, true, 0)
                } else {
                    val offset = if (sourceLine.selected) sourceLine.offset else sourceLine.line.length

                    ContractLine(sourceLine.line + newLine, true, offset)
                }
            }
        }

        tail.entries.take(2).forEach { (lineNumber, newLine) ->
            subsection.compute(lineNumber) { _, sourceLine ->
                if (sourceLine == null)
                    ContractLine(newLine)
                else
                    ContractLine(sourceLine.line + newLine, sourceLine.selected, sourceLine.offset)
            }
        }

        return SolidityContract(sourceFile.filePath, subsection)
    }

    private fun sourceSize(contractContent: SortedMap<Int, ContractLine>) = contractContent.values
        // Doing +1 to include newline
        .map { it.line.length + 1 }
        .sum()

    private fun pcSourceMap(
        sourceMapElements: List<SourceMapElement>,
        opCodeGroups: List<String>
    ): Map<Int, SourceMapElement> {
        val mappings = HashMap<Int, SourceMapElement>()

        var location = 0

        for (i in 0 until min(opCodeGroups.size, sourceMapElements.size)) {
            mappings[location] = sourceMapElements[i]
            location += (opCodeGroups[i].length / 2)
        }

        return mappings
    }

    private fun loadContractMapping(contractCreation: Boolean, bytecode: String): ContractMapping {
        val metaFile =
            File("${debugProcess.getRunConfig().workingDirectory}/build/resources/main/solidity")
        if (!metaFile.exists())
            return ContractMapping(emptyMap(), emptyMap())

        val contractMetas = loadContractMeta(metaFile)

        val (contract, sourceList) = contractMetas
            .map { Pair(maybeContractMap(bytecode, it), it.sourceList) }
            .firstOrNull { it.first.isNotEmpty() } ?: return ContractMapping(emptyMap(), emptyMap())

        val srcmap = if (contractCreation) {
            contract["srcmap"]
        } else {
            contract["srcmap-runtime"]
        } ?: return ContractMapping(emptyMap(), emptyMap())

        val idxSource = sourceList
            .withIndex()
            .map { Pair(it.index, SolidityContract(it.value, loadFile(debugProcess, it.value))) }
            .toMap()

        val sourceMapElements = decompressSourceMap(srcmap)
        val opCodeGroups = opCodeGroups(bytecode)
        val pcSourceMappings = pcSourceMap(sourceMapElements, opCodeGroups)

        return ContractMapping(idxSource, pcSourceMappings)
    }

    private fun opCodeToOpSize(opCode: String): Int {
        return when (opCode.toUpperCase()) {
            "60" -> 2
            "61" -> 3
            "62" -> 4
            "63" -> 5
            "64" -> 6
            "65" -> 7
            "66" -> 8
            "67" -> 9
            "68" -> 10
            "69" -> 11
            "6A" -> 12
            "6B" -> 13
            "6C" -> 14
            "6D" -> 15
            "6E" -> 16
            "6F" -> 17
            "70" -> 18
            "71" -> 19
            "72" -> 20
            "73" -> 21
            "74" -> 22
            "75" -> 23
            "76" -> 24
            "77" -> 25
            "78" -> 26
            "79" -> 27
            "7A" -> 28
            "7B" -> 29
            "7C" -> 30
            "7D" -> 31
            "7E" -> 32
            "7F" -> 33
            else -> 1
        }
    }

    private fun opCodeGroups(bytecode: String): List<String> {
        return bytecode
            .split("(?<=\\G.{2})".toRegex())
            .foldIndexed(Pair(0, ArrayList<String>()), { index, state, opCode ->
                if (opCode.isBlank()) return@foldIndexed state

                val acc = state.first
                val groups = state.second

                if (index >= acc) {
                    Pair(acc + opCodeToOpSize(opCode), groups.apply { add(opCode) })
                } else {
                    Pair(acc, groups.apply { set(size - 1, last() + opCode) })
                }
            }).second
    }

    /**
     * Breaks down the src map from a contract's metadata file.
     * The sequences are separated by ; e.g 122:136; 8:9:-1;
     * If the current sequence is empty 122:136;; then it will be assigned the value of the previous one
     * Each section is then a SourceMapElement
     */

    private fun decompressSourceMap(sourceMap: String): List<SourceMapElement> {
        fun foldOp(elements: MutableList<SourceMapElement>, sourceMapPart: String): MutableList<SourceMapElement> {
            val prevSourceMapElement = if (elements.isNotEmpty()) elements.last() else SourceMapElement()
            val parts = sourceMapPart.split(":")
            val s =
                if (parts.isNotEmpty() && parts[0].isNotBlank()) parts[0].toInt() else prevSourceMapElement.sourceFileByteOffset
            val l =
                if (parts.size > 1 && parts[1].isNotBlank()) parts[1].toInt() else prevSourceMapElement.lengthOfSourceRange
            val f = if (parts.size > 2 && parts[2].isNotBlank()) parts[2].toInt() else prevSourceMapElement.sourceIndex
            val j = if (parts.size > 3 && parts[3].isNotBlank()) parts[3] else prevSourceMapElement.jumpType
            return elements.apply { add(SourceMapElement(s, l, f, j)) }
        }

        return sourceMap.split(";").fold(ArrayList(), ::foldOp)
    }


    /**
     *
     * Loads the metadata files from a directory into a map as keys
     * In the value section it extracts the bin, bin-runtime, srcmap, srcmap-runtime
     *
     */
    private fun loadContractMeta(file: File): List<ContractMeta> {
        return when {
            file.isFile && file.name.endsWith(".json") && !file.name.endsWith("meta.json") -> {
                listOf(Klaxon().parse<ContractMeta>(file) ?: ContractMeta(emptyMap(), emptyList()))
            }
            file.isDirectory -> {
                file.listFiles()
                    ?.map { loadContractMeta(it) }
                    ?.flatten() ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun loadFile(debugProcess: Web3jDebugProcess, path: String): SortedMap<Int, ContractLine> {
        val baseDir = debugProcess.getRunConfig().project.basePath
        return BufferedReader(FileReader("$baseDir/$path")).use { reader ->
            reader.lineSequence()
                .withIndex()
                .map { indexedLine -> Pair(indexedLine.index + 1, ContractLine(indexedLine.value)) }
                .toMap(TreeMap())
        }
    }

    /**
     * Given a message frame derive the source code
     */

    private fun sourceAtMessageFrame(
        messageFrame: MessageFrame
    ): Pair<SourceMapElement?, SolidityContract> {
        val byteCodeContractMapping = HashMap<Pair<String, Boolean>, ContractMapping>()
        var lastSourceFile = SolidityContract()

        val pc = messageFrame.pc
        val contractCreation = MessageFrame.Type.CONTRACT_CREATION == messageFrame.type
        val bytecode = toUnPrefixedString(messageFrame.code.bytes)
        val (idxSource, pcSourceMappings) = byteCodeContractMapping.getOrPut(Pair(bytecode, contractCreation)) {
            loadContractMapping(
                contractCreation,
                bytecode
            )
        }

        val sourceFileSelection =
            findSourceNear(idxSource, pcSourceMappings[pc] ?: return Pair(pcSourceMappings[pc], lastSourceFile))

        if (sourceFileSelection.contractContent.isNotEmpty()) {
            lastSourceFile = sourceFileSelection
        }

        val outputSourceFile = if (lastSourceFile.contractContent.isEmpty()) {
            SolidityContract(contractContent = sortedMapOf(0 to ContractLine("No source available")))
        } else lastSourceFile

        return Pair(pcSourceMappings[pc], outputSourceFile)
    }

    private fun maybeContractMap(bytecode: String, contractMeta: ContractMeta): Map<String, String> {
        return contractMeta
            .contracts
            .values
            .firstOrNull { contractProps ->
                contractProps.filter { propEntry ->
                    propEntry.key.startsWith("bin")
                }.values.any { v ->
                    bytecode.startsWith(v)
                }
            } ?: emptyMap()
    }


}


