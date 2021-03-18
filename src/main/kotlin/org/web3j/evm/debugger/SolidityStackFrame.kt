package org.web3j.evm.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import java.nio.file.Paths

class SolidityValue(name: String, private val type: String, private val value: String) : XNamedValue(name) {
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(null, type, value, false)
    }
}

class SolidityStackFrame(private val project: Project,
                         private val srcFile: String,
                         private val line: Int): XStackFrame() {

    private val values = XValueChildrenList()

    override fun getSourcePosition(): XSourcePosition? {
        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(Paths.get(srcFile).normalize())
        return XDebuggerUtil.getInstance().createPosition(virtualFile, line - 1)
    }

    fun addValue(node: SolidityValue) {
        values.add(node.name, node)
    }

    override fun computeChildren(node: XCompositeNode) {
        node.addChildren(values, true)
    }

}