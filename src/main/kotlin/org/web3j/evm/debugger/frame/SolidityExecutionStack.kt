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
package org.web3j.evm.debugger.frame

import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.frame.XStackFrameContainerEx

class SolidityExecutionStack(private val stackFrameList: List<SolidityStackFrame>)
    :XExecutionStack("EVM Stack") {

    private var topFrame: XStackFrame? = null

    init {
        if (stackFrameList.isNotEmpty())
            topFrame = stackFrameList[stackFrameList.size - 1]
    }

    /**
     * Return top stack frame synchronously
     * @return top stack frame or `null` if it isn't available
     */
    override fun getTopFrame() = topFrame

    /**
     * Start computing stack frames top-down starting from `firstFrameIndex`. This method is called from the Event Dispatch Thread
     * so it should return quickly
     * @param firstFrameIndex frame index to start from (`1` corresponds to the frame just under the top frame)
     * @param container callback
     */
    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer?) {
        val stackFrameContainerEx = container as XStackFrameContainerEx
        stackFrameContainerEx.addStackFrames(stackFrameList, topFrame, true)
    }


}
