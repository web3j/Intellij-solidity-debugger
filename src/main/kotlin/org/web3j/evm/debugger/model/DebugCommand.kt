package org.web3j.evm.debugger.model

enum class DebugCommand(val command: String) {
    EXECUTE("execute"),

    STEP_INTO("stepInto"),
    FORCE_STEP_INTO("forceStepInto"),
    STEP_OUT("stepOut"),
    STEP_OVER("stepOver"),

    SUSPEND("suspend"),

    RESUME("resume"),

    JUMP("jump")
}