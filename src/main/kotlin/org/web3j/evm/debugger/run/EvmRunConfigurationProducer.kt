package org.web3j.evm.debugger.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

open class EvmRunConfigurationProducer : LazyRunConfigurationProducer<EvmRunConfiguration>() {

    override fun setupConfigurationFromContext(
        configuration: EvmRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val psiFile = context.psiLocation!!.containingFile ?: return false
        val psiMethod = PsiTreeUtil.getParentOfType(sourceElement.get(), PsiMethod::class.java)
        val psiClass = psiMethod!!.containingClass!!
        configuration.name = "Evm" + psiClass.name
        configuration.setMethodName(psiMethod.name)
        configuration.setContractName(psiClass.qualifiedName)
        isContextValid(context, sourceElement)
        return true
    }


    override fun getConfigurationFactory(): ConfigurationFactory {
        return EvmRunConfigurationFactory(EvmRunConfigurationType())
    }


    private fun isContextValid(context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val psiMethod = PsiTreeUtil.getParentOfType(sourceElement.get(), PsiMethod::class.java)
        psiMethod!!.accept(object : JavaRecursiveElementVisitor() {
            override fun visitLocalVariable(variable: PsiLocalVariable) {
                super.visitLocalVariable(variable)
                if (variable.type.toString() == "PsiType:Web3j" && variable.hasInitializer()) {
                    println(variable.name + " exists and it's initialised")
                }
                if (variable.type.toString() == "PsiType:Web3j" && !variable.hasInitializer()) {
                    println(variable.name + " exists and it's not initialised")
                }
            }
        })
        return true
    }

    override fun isConfigurationFromContext(
        configuration: EvmRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        return false
    }
}