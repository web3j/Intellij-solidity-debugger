package org.web3j.evm.debugger.breakpoint

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import me.serce.solidity.lang.SolidityFileType

class SolidityBreakPointType : XLineBreakpointType<SolidityBreakpointProperties>(ID, NAME) {

    companion object {
        private const val ID = "solidity-line"
        private const val NAME = "solidity-line-breakpoint"
    }

    override fun createBreakpointProperties(file: VirtualFile, line: Int): SolidityBreakpointProperties {
        println("SolidityLineBreakpoint createBreakpoint fileType: ${file.fileType}, line: $line")

        return SolidityBreakpointProperties()
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        val canPut = line >= 0 && file.fileType === SolidityFileType && isLineBreakpointAvailable(
            file, line,
            project
        )
        println("SolidityLineBreakpoint canPutAt: $canPut, line: $line")
        return canPut
    }

    private fun isLineBreakpointAvailable(file: VirtualFile, line: Int, project: Project): Boolean {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document == null || document.getLineEndOffset(line) == document.getLineStartOffset(line)) {
            return false
        }
        val canPutAtChecker = Checker()
        XDebuggerUtil.getInstance().iterateLine(project, document, line, canPutAtChecker)
        return canPutAtChecker.isLineBreakpointAvailable
    }

    private class Checker : Processor<PsiElement> {
        //TODO
        val isLineBreakpointAvailable = true

        override fun process(element: PsiElement): Boolean {
            // TODO
            return true
        }
    }
}