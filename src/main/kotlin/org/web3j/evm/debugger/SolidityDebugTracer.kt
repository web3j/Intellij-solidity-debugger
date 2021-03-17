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
import org.apache.tuweni.bytes.Bytes32
import org.hyperledger.besu.ethereum.vm.MessageFrame
import org.hyperledger.besu.ethereum.vm.OperationTracer
import org.web3j.evm.ExceptionalHaltException
import java.io.*
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.BlockingQueue


private data class ContractMeta(val contracts: Map<String, Map<String, String>>, val sourceList: List<String>)

private data class ContractMapping(
    val idxSource: Map<Int, SourceFile>,
    val pcSourceMappings: Map<Int, SourceMapElement>
)

data class SourceFile(
    val filePath: String? = null,
    val sourceContent: SortedMap<Int, SourceLine> = Collections.emptySortedMap()
)

data class SourceLine(val line: String, val selected: Boolean = false, val offset: Int = 0)

data class SourceMapElement(
    val sourceFileByteOffset: Int = 0,
    val lengthOfSourceRange: Int = 0,
    val sourceIndex: Int = 0,
    val jumpType: String = ""
)

// TODO: refactor this class, seperate debug actions from solidity source code mapping
class SolidityDebugTracer(private val debugProcess: Web3jDebugProcess) : OperationTracer {
    private val operations = ArrayList<String>()
    private val skipOperations = AtomicInteger()
    private var breakPoints = mutableMapOf<String, MutableSet<Int>>()
    private val byteCodeContractMapping = HashMap<Pair<String, Boolean>, ContractMapping>()
    private var runTillNextLine = false
    private var lastSourceFile = SourceFile()
    private var lastSourceMapElement: SourceMapElement? = null
    private var lastSelectedLine = 0
    private val commandQueue: BlockingQueue<String> = LinkedBlockingDeque()
    private val stackFrames = mutableListOf<SolidityStackFrame>()
    private var metaFile: File = File("${debugProcess.getRunConfig().workingDirectory}/build/resources/main/solidity")

    fun setBreakpoints(breakpoints: MutableMap<String, MutableSet<Int>>) {
        this.breakPoints = breakpoints
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

        return sourceMap.split(";").fold(ArrayList<SourceMapElement>(), ::foldOp)
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

    private fun loadFile(path: String): SortedMap<Int, SourceLine> {
        val baseDir = debugProcess.getRunConfig().project.basePath
        return BufferedReader(FileReader("$baseDir/$path")).use { reader ->
            reader.lineSequence()
                .withIndex()
                .map { indexedLine -> Pair(indexedLine.index + 1, SourceLine(indexedLine.value)) }
                .toMap(TreeMap())
        }
    }

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
            .map { Pair(it.index, SourceFile(it.value, loadFile(it.value))) }
            .toMap()

        val sourceMapElements = decompressSourceMap(srcmap)
        val opCodeGroups = opCodeGroups(bytecode)
        val pcSourceMappings = pcSourceMap(sourceMapElements, opCodeGroups)

        return ContractMapping(idxSource, pcSourceMappings)
    }

    private fun sourceSize(sourceContent: SortedMap<Int, SourceLine>) = sourceContent.values
        // Doing +1 to include newline
        .map { it.line.length + 1 }
        .sum()

    private fun sourceRange(sourceContent: SortedMap<Int, SourceLine>, from: Int, to: Int): SortedMap<Int, String> {
        return sourceContent.entries.fold(Pair(0, TreeMap<Int, String>())) { acc, entry ->
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

    private fun findSourceNear(idxSource: Map<Int, SourceFile>, sourceMapElement: SourceMapElement): SourceFile {
        val sourceFile = idxSource[sourceMapElement.sourceIndex] ?: return SourceFile()
        val sourceContent = sourceFile.sourceContent
        val sourceLength = sourceSize(sourceContent)

        val from = sourceMapElement.sourceFileByteOffset
        val to = from + sourceMapElement.lengthOfSourceRange

        val head = sourceRange(sourceContent, 0, from - 1)

        val body = sourceRange(sourceContent, from, to - 1).map {
            Pair(it.key, it.value)
        }.toMap(TreeMap())

        val tail = sourceRange(sourceContent, to, sourceLength)

        val subsection = TreeMap<Int, SourceLine>()

        head.entries.reversed().take(2).forEach { (lineNumber, newLine) ->
            subsection[lineNumber] = SourceLine(newLine)
        }

        body.forEach { (lineNumber, newLine) ->
            subsection.compute(lineNumber) { _, sourceLine ->
                if (sourceLine == null) {
                    SourceLine(newLine, true, 0)
                } else {
                    val offset = if (sourceLine.selected) sourceLine.offset else sourceLine.line.length

                    SourceLine(sourceLine.line + newLine, true, offset)
                }
            }
        }

        tail.entries.take(2).forEach { (lineNumber, newLine) ->
            subsection.compute(lineNumber) { _, sourceLine ->
                if (sourceLine == null)
                    SourceLine(newLine)
                else
                    SourceLine(sourceLine.line + newLine, sourceLine.selected, sourceLine.offset)
            }
        }

        return SourceFile(sourceFile.filePath, subsection)
    }

    private fun sourceAtMessageFrame(messageFrame: MessageFrame): Pair<SourceMapElement?, SourceFile> {
        val pc = messageFrame.pc
        val contractCreation = MessageFrame.Type.CONTRACT_CREATION == messageFrame.type
        val bytecode = toUnprefixedString(messageFrame.code.bytes)
        val (idxSource, pcSourceMappings) = byteCodeContractMapping.getOrPut(Pair(bytecode, contractCreation)) {
            loadContractMapping(
                contractCreation,
                bytecode
            )
        }

        val sourceFileSelection =
            findSourceNear(idxSource, pcSourceMappings[pc] ?: return Pair(pcSourceMappings[pc], lastSourceFile))

        if (sourceFileSelection.sourceContent.isNotEmpty()) {
            lastSourceFile = sourceFileSelection
        }

        val outputSourceFile = if (lastSourceFile.sourceContent.isEmpty()) {
            SourceFile(sourceContent = sortedMapOf(0 to SourceLine("No source available")))
        } else lastSourceFile

        return Pair(pcSourceMappings[pc], outputSourceFile)
    }

    private fun toUnprefixedString(bytes: org.apache.tuweni.bytes.Bytes): String {
        val prefixedHex = bytes.toString()
        return if (prefixedHex.startsWith("0x")) prefixedHex.substring(2) else prefixedHex
    }

    @Throws(ExceptionalHaltException::class)
    private fun step(messageFrame: MessageFrame): String {
        val (sourceMapElement, sourceFile) = sourceAtMessageFrame(messageFrame)
        val (filePath, sourceSection) = sourceFile

        val firstSelectedLine =
            sourceSection.entries.filter { it.value.selected }.map { it.key }.min() ?: 0
        val firstSelectedOffset = sourceSection[firstSelectedLine]?.offset ?: 0

        val sb = StringBuilder()
        if (sourceMapElement != null) sb
            .append("At solidity source location" +
                    " ${sourceMapElement.sourceFileByteOffset}:${sourceMapElement.lengthOfSourceRange}:${sourceMapElement.sourceIndex}:")

        sb.append("Line $firstSelectedLine:$firstSelectedOffset")
        println(sb.toString())

        when (commandQueue.take()) {
            "execute" -> {
               if (runTillNextLine && firstSelectedLine == lastSelectedLine) {
                   updateStackFrame("" + filePath, firstSelectedLine, messageFrame)
                   debugProcess.suspend(firstSelectedLine)
               } else if (runTillNextLine) {
                   runTillNextLine = false
                   commandQueue.put("suspend")
                   updateStackFrame("" + filePath, firstSelectedLine, messageFrame)
                   return step(messageFrame)
               }
               else if (breakPoints.values.any { it.contains(firstSelectedLine) }) {
                   commandQueue.put("suspend")
                   updateStackFrame("" + filePath, firstSelectedLine, messageFrame)
                   return step(messageFrame)
                }
            }
            "suspend" -> {
                debugProcess.suspend(firstSelectedLine)
                debugProcess.consolePrint("line $firstSelectedLine offset $firstSelectedOffset")
                lastSelectedLine = firstSelectedLine
                runTillNextLine = true
                return step(messageFrame)
            }
            "stepOver", "stepInto", "stepOut" -> {
               debugProcess.consolePrint("Stepping..")
            }
        }
       return ""
    }

    fun sendCommand(command: String) {
        commandQueue.put(command)
    }

    fun getStackFrames(): List<SolidityStackFrame> {
        return stackFrames
    }

    private fun updateStackFrame(filePath: String, line: Int, frame: MessageFrame) {
        val baseDir = debugProcess.getRunConfig().project.basePath
        val stackFrame = SolidityStackFrame(debugProcess.session.project,
            "$baseDir/$filePath", line)
        //TODO: extract stack frame variable names and values
        val content = captureStack(frame)
        stackFrame.addValue(SolidityValue("x", "String", content.toString()))
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

    override fun traceExecution(messageFrame: MessageFrame, executeOperation: OperationTracer.ExecuteOperation?) {
        commandQueue.put("execute")
        step(messageFrame)

        executeOperation?.execute()
        if (messageFrame.state != MessageFrame.State.CODE_EXECUTING) {
            skipOperations.set(0)
            operations.clear()
            runTillNextLine = false
        }
    }

}
