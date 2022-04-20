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
package org.web3j.evm.debugger.model

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class JumpOpCodeProcessManager {
    private var active = AtomicBoolean(false)

    private var destCounter = AtomicInteger(2)

    @Synchronized fun stepOverActivate(){
        if(active.compareAndExchange(false, true)){
            destCounter.set(2)
        }
    }

    @Synchronized fun stepOutActivate(){
        active.set(true)
        destCounter.set(1)
    }

    @Synchronized fun deactivate(){
        if(active.compareAndExchange(true, false)){
            destCounter.set(2)
        }
    }

    @Synchronized fun incrementDestCounter(){
        if(active.get() && destCounter.decrementAndGet() <= 0){
            deactivate()
        }
    }

    fun isActive() : Boolean{
        return active.get()
    }


}