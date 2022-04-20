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
package org.web3j.evm.debugger.breakpoint


import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint


class SolidityBreakpointHandler : XBreakpointHandler<XLineBreakpoint<SolidityBreakpointProperties>>(SolidityBreakpointType::class.java) {
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