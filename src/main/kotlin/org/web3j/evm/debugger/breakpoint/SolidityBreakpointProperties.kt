package org.web3j.evm.debugger.breakpoint

import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class SolidityBreakpointProperties : XBreakpointProperties<SolidityBreakpointProperties>() {
    override fun getState(): SolidityBreakpointProperties {
        return this;
    }

    override fun loadState(state: SolidityBreakpointProperties) {
        TODO("Not yet implemented")
    }
}