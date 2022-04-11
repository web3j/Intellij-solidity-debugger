package org.web3j.evm.debugger.frame

import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace

class SolidityNamedValue (name: String, private val type: String, private val value: String) : XNamedValue(name) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(null, type, value, false)
    }
}