package org.web3j.evm.debugger.model

enum class DebugCommand(val command: String) {
    EXECUTE("execute"),

    STEP_INTO("stepInto"),
    STEP_OUT("stepOut"),
    STEP_OVER("stepOver"),

    SUSPEND("suspend")
}