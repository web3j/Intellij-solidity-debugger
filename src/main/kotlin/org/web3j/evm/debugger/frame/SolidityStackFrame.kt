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