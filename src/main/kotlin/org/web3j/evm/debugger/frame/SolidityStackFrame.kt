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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import me.serce.solidity.lang.psi.SolElement
import me.serce.solidity.lang.psi.elementType
import me.serce.solidity.lang.psi.impl.SolConstructorDefinitionImpl
import me.serce.solidity.lang.psi.impl.SolContractDefinitionImpl

class SolidityStackFrame(
    private val project: Project,
    private val srcFile: String,
    private val line: Int,
    private val offset: Int
) : XStackFrame() {

    private val values = XValueChildrenList()

    override fun getSourcePosition(): XSourcePosition {
        return SoliditySourcePosition(srcFile, line, offset, project)
    }

    fun addValue(node: SolidityNamedValue) {
        values.add(node.name, node)
    }

    override fun computeChildren(node: XCompositeNode) {
        node.addChildren(values, true)
    }

    override fun customizePresentation(component: ColoredTextContainer) {

        // TODO: If not a parent the the parent we need to know the context of execution if we are withing a function then the context is the function element if its a state variable then the context is the contract
        val srcPos = sourcePosition as SoliditySourcePosition

        ApplicationManager.getApplication().runReadAction {
            val context = srcPos.getPsiElementAtPosition()?.let { resolveContext(it) }
            if (context != null) {
                component.append(context.elementType.debugName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }

    private fun resolveContext(psiElementAtPosition: PsiElement): SolElement? {
        return when (val parent = PsiTreeUtil.getParentOfType(psiElementAtPosition, SolElement::class.java)) {
            is SolConstructorDefinitionImpl -> {
                return parent
            }
            is SolContractDefinitionImpl -> {
                return parent
            }
            else -> {
                if (parent != null && parent.parent != null){
                    return resolveContext(parent.parent) ?: return parent
                } else {
                    return null
                }
            }
        }
    }

}