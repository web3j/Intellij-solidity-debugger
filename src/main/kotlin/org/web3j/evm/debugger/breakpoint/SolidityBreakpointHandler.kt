package org.web3j.evm.debugger.breakpoint


import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint


class SolidityBreakpointHandler : XBreakpointHandler<XLineBreakpoint<SolidityBreakpointProperties>>(SolidityBreakPointType::class.java) {
    val breakpoints = mutableListOf<XLineBreakpoint<*>>()

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<SolidityBreakpointProperties>) {
        val breakpointPosition = breakpoint.sourcePosition ?: return
        val file = breakpointPosition.file

        //TODO

        breakpoints.add(breakpoint)
    }

    override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<SolidityBreakpointProperties>, temporary: Boolean) {
        val breakpointPosition = breakpoint.sourcePosition ?: return

        breakpoints.remove(breakpoint)
    }
}