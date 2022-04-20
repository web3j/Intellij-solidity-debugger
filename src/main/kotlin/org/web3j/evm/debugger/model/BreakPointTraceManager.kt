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

class BreakPointTraceManager {
    private var breakPoints = mutableMapOf<String, MutableSet<Int>>()
    private var breakPointsSkip = mutableMapOf<String, MutableSet<Int>>()


    @Synchronized fun setBreakPoints(breakPoints: MutableMap<String, MutableSet<Int>>){
        this.breakPoints = breakPoints
    }


    @Synchronized fun skipBreakPoint(contract: String, breakPointLine: Int){
        breakPointsSkip.getOrPut(contract) {mutableSetOf()}
        breakPointsSkip[contract]?.add(breakPointLine)
    }

    @Synchronized fun resetSkipBreakPoints(){
        breakPointsSkip.clear()
    }

    fun stoppableBreakPoint(contract: String?, breakLine: Int) : Boolean{
        if(!breakPoints.containsKey(contract) || breakPoints[contract]?.contains(breakLine) == false){
            return false
        }

        if(breakPointsSkip.containsKey(contract) && breakPointsSkip[contract]?.contains(breakLine) == true){
            return false
        }

        return true
    }


}