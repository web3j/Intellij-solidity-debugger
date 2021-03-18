package org.web3j.evm.debugger

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.frame.XSuspendContext

class SoliditySuspendContext(
    private val active: ExecutionStack,
    val breakpoint: XBreakpoint<*>,
    val srcPosition: XSourcePosition,
    val project: Project
) : XSuspendContext() {
    private lateinit var executionStack: ExecutionStack

    override fun getActiveExecutionStack() = active
    override fun computeExecutionStacks(container: XExecutionStackContainer?) {
        super.computeExecutionStacks(container)
    }


    private fun initExecutionStack(): ExecutionStack {
        val frame = SolidityStackFrame()
        return ExecutionStack(myProject, myController, "Main Routine", frame, myEncodedStack)
    }


}
