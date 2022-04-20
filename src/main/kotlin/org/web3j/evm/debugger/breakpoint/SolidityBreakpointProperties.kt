/*
 * Copyright 2019 Web3 Labs Ltd.
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

import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class SolidityBreakpointProperties : XBreakpointProperties<SolidityBreakpointProperties>() {
    override fun getState(): SolidityBreakpointProperties {
        return this;
    }

    override fun loadState(state: SolidityBreakpointProperties) {
        TODO("Not yet implemented")
    }
}