package org.web3j.evm.debugger.frame

import com.intellij.xdebugger.frame.XSuspendContext

class SoliditySuspendContext(private val active: SolidityExecutionStack) : XSuspendContext() {
    override fun getActiveExecutionStack() = active
}
