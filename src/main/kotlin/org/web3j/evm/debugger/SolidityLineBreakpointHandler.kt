/*
 * Copyright 2021 Web3 Labs Ltd.
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

import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint

class SolidityLineBreakpointHandler(val debugProcess: Web3jDebugProcess):
    XBreakpointHandler<XLineBreakpoint<XBreakpointProperties<*>>>(SolidityLineBreakpointType::class.java) {
    /**
     * Called when a breakpoint need to be registered in the debugging engine
     * @param breakpoint breakpoint to register
     */
    override fun registerBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        println("Register breakpoint: $breakpoint")
    }

    /**
     * Called when a breakpoint need to be unregistered from the debugging engine
     * @param breakpoint breakpoint to unregister
     * @param temporary determines whether `breakpoint` is unregistered forever or it may be registered again. This parameter may
     * be used for performance purposes. For example the breakpoint may be disabled rather than removed in the debugging engine if
     * `temporary` is `true`
     */
    override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>, temporary: Boolean) {
        println("unregister breakpoint: $breakpoint")
    }
    

}