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