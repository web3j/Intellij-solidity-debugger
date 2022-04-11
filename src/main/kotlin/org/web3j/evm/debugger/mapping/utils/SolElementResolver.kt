package org.web3j.evm.debugger.mapping.utils

import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.psi.SolElement
import me.serce.solidity.lang.psi.SolParameterList
import me.serce.solidity.lang.psi.impl.SolAssignmentExpressionImpl
import me.serce.solidity.lang.psi.impl.SolConstructorDefinitionImpl
import me.serce.solidity.lang.psi.impl.SolContractDefinitionImpl
import me.serce.solidity.lang.psi.impl.SolNumberTypeImpl
import me.serce.solidity.lang.psi.impl.SolVarLiteralImpl
import me.serce.solidity.lang.psi.impl.SolVariableDeclarationImpl
import me.serce.solidity.lang.types.type
import org.web3j.evm.debugger.frame.SolidityStackFrame
import org.web3j.evm.debugger.frame.SolidityNamedValue

fun resolveContext(psiElement: SolElement, stackFrame: SolidityStackFrame): String {
    when (psiElement) {
        is SolConstructorDefinitionImpl -> {
            resolveConstructor(psiElement, stackFrame)
            return "SolConstructionDefinitionImpl"
        }
        is SolContractDefinitionImpl -> {
            resolveContract(psiElement, stackFrame)
            return "SolContractDefinitionImpl"
        }
        is SolNumberTypeImpl -> {
            resolveVariable(psiElement, stackFrame)
            return "SolVariableDeclarationImpl"
        }
        is SolVarLiteralImpl -> {
            resolveLiteral(psiElement, stackFrame)
            return "SolVarLiteralImpl"
        }
    }
    return "Can't find context"
}

private fun resolveConstructor(psiElement: SolElement, stackFrame: SolidityStackFrame) {
    psiElement as SolConstructorDefinitionImpl
    if ((psiElement.parameterList as SolParameterList).parameterDefList.isNotEmpty()) {
        (psiElement.parameterList as SolParameterList).parameterDefList.forEach {
            stackFrame.addValue(SolidityNamedValue(it.identifier!!.text, it.typeName.text, ""))
        }
    }
}

private fun resolveContract(psiElement: SolElement, stackFrame: SolidityStackFrame) {
    psiElement as SolContractDefinitionImpl
    val stateVariablesList = psiElement.stateVariableDeclarationList
    if (stateVariablesList.isNotEmpty()) {
        stateVariablesList.forEach {
            stackFrame.addValue(SolidityNamedValue(it.identifier.text, it.typeName.text, ""))
        }
    }
}

private fun resolveLiteral(psiElement: SolElement, stackFrame: SolidityStackFrame) {
    val maybeLiteral = PsiTreeUtil.getParentOfType(psiElement, SolAssignmentExpressionImpl::class.java)
    if (maybeLiteral != null) {
        stackFrame.addValue(
            SolidityNamedValue(
                maybeLiteral.expressionList[0].text,
                maybeLiteral.expressionList[0].type.toString(),
                maybeLiteral.expressionList[1].text
            )
        )
    }
}


private fun resolveVariable(psiElement: SolElement, stackFrame: SolidityStackFrame) {
    val maybeLiteral = PsiTreeUtil.getParentOfType(psiElement, SolVariableDeclarationImpl::class.java)
    if (maybeLiteral != null) {
        stackFrame.addValue(
            SolidityNamedValue(
                maybeLiteral.firstChild.nextSibling.nextSibling.text,
                maybeLiteral.firstChild.text,
                maybeLiteral.parent.lastChild.text
            )
        )
    }
}
