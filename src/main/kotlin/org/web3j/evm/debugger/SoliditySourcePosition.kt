package org.web3j.evm.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XSourcePositionImpl
import java.nio.file.Paths


class SoliditySourcePosition(
    srcFile: String,
    private val jumpLine: Int,
    private val offset: Int,
    private val project: Project
) :
    XSourcePosition {

    private val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(Paths.get(srcFile).normalize())

    override fun getLine(): Int {
        return jumpLine - 1
    }

    override fun getOffset(): Int {
        return offset
    }

    override fun getFile(): VirtualFile {
        return virtualFile!!
    }

    override fun createNavigatable(project: Project): Navigatable {
        return XSourcePositionImpl.doCreateOpenFileDescriptor(project, this)
    }

    fun getPsiElementAtPosition(): PsiElement? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile!!)
        val mapOfLineOffsets = resolveOffsetAtLine(psiFile!!.text)
        val indexWithoutWhitespace = mapOfLineOffsets[line]!!.second.indexOfFirst { c -> c != ' ' }
        return psiFile.findElementAt(mapOfLineOffsets[line]!!.first.first + indexWithoutWhitespace)
    }

    private fun resolveOffsetAtLine(textFile: String): Map<Int, Pair<Pair<Int, Int>, String>> {
        val map = mutableMapOf<Int, Pair<Pair<Int, Int>, String>>()
        textFile.lines().forEachIndexed { index, s ->
            if (index == 0) {
                map.putIfAbsent(index, Pair(Pair(index, s.length), s))
            } else {
                val previousLineEndOffset = map[index - 1]!!.first.second
                map.putIfAbsent(index, Pair(Pair(previousLineEndOffset + 1, previousLineEndOffset + 1 + s.length), s))
            }
        }
        return map
    }
}