package org.web3j.evm.debugger.mapping.utils

import com.intellij.patterns.PsiJavaPatterns.psiClass
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.psi.SolElement
import me.serce.solidity.lang.psi.SolParameterList
import me.serce.solidity.lang.psi.impl.SolAssignmentExpressionImpl
import me.serce.solidity.lang.psi.impl.SolConstructorDefinitionImpl
import me.serce.solidity.lang.psi.impl.SolContractDefinitionImpl
import me.serce.solidity.lang.psi.impl.SolVarLiteralImpl
import me.serce.solidity.lang.types.type
import org.web3j.evm.debugger.SolidityStackFrame
import org.web3j.evm.debugger.SolidityValue

fun getContracts(){

}

fun getLocalVariables (psiElement: SolElement) {
    var variables = PsiTreeUtil.collectElements(psiElement, PsiElementFilter { e ->
        if (e is PsiVariable) {
            true
        } else false
    })
    println(variables)
    variables
}

fun resolveContext(psiElement: SolElement, stackFrame: SolidityStackFrame): String {
    println("resolve context")

    when (psiElement) {
        is SolConstructorDefinitionImpl -> {
            resolveConstructor(psiElement, stackFrame)
            println("Memory var :" + psiElement.parameterList.toString())

            //getVariables(psiElement)
            return "SolConstructionDefinitionImpl"
        }
        is SolContractDefinitionImpl -> {
            resolveContract(psiElement, stackFrame)
//            println("Storage var: " + psiElement.stateVariableDeclarationList)
            return "SolContractDefinitionImpl"
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
    println("constructorDefinition : " )
    psiElement.parameterList
    psiElement.children.iterator()
    if ((psiElement.parameterList as SolParameterList).parameterDefList.isNotEmpty()) {
        (psiElement.parameterList as SolParameterList).parameterDefList.forEach {
            println(it.identifier!!.text +"  "+ it.typeName.text + " "+ it.typeName)
            stackFrame.addValue(SolidityValue(it.identifier!!.text, it.typeName.text, ""))
        }
    }
}

private fun resolveContract(psiElement: SolElement, stackFrame: SolidityStackFrame) {
    psiElement as SolContractDefinitionImpl
    val stateVariablesList = psiElement.stateVariableDeclarationList



    //println("stateVariablesList : " )
    if (stateVariablesList.isNotEmpty()) {
        stateVariablesList.forEach {
            //println(it.identifier.text +"  "+ it.typeName.text)
            stackFrame.addValue(SolidityValue(it.identifier.text,
                "typexx ", //it.typeName.text,
                ""))
        }
    }
}

private fun resolveLiteral(psiElement: SolElement, stackFrame: SolidityStackFrame) {
    val maybeLiteral = PsiTreeUtil.getParentOfType(psiElement, SolAssignmentExpressionImpl::class.java)
    if (maybeLiteral != null) {


        stackFrame.addValue(
            SolidityValue(
                maybeLiteral.expressionList[0].text,
                "typexx", //maybeLiteral.expressionList[0].type.toString(),
                "valuexx"//maybeLiteral.expressionList[1].text
            )
        )
    }
}
