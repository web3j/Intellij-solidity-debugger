package org.web3j.evm.debugger

import com.intellij.xdebugger.frame.XSuspendContext

class SoliditySuspendContext(private val active: ExecutionStack) : XSuspendContext() {
    override fun getActiveExecutionStack() = active
}