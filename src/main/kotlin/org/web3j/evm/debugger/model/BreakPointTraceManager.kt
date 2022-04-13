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