package org.web3j.evm.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTextContainer
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XStackFrame

class SolidityStackFrame(srcFile: VirtualFile?, line: Int) : XStackFrame() {
    private val srcPosition: XSourcePosition?

    init {
        srcPosition = XDebuggerUtil.getInstance().createPosition(srcFile, line)
    }


    override fun getEqualityObject(): Any? {
        return super.getEqualityObject()
    }

    override fun getEvaluator(): XDebuggerEvaluator? {
        return super.getEvaluator()
    }

    override fun getSourcePosition(): XSourcePosition? {
        return srcPosition
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        super.customizePresentation(component)
    }
}