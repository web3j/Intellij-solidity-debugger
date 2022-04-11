package org.web3j.evm.debugger.model

enum class OpCode(val codeName: String) {
    JUMP("JUMP"),
    JUMPDEST("JUMPDEST");


    companion object {
        fun isJump(code: String): Boolean {
            return JUMP.codeName == code
        }

        fun isJumpDest(code: String): Boolean {
            return JUMPDEST.codeName == code
        }
    }

}