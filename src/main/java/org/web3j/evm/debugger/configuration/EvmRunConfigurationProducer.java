package org.web3j.evm.debugger.configuration;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class EvmRunConfigurationProducer extends LazyRunConfigurationProducer<EvmRunConfiguration> {

    @Override
    protected boolean setupConfigurationFromContext(@NotNull EvmRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        PsiFile psiFile = context.getPsiLocation().getContainingFile();
        if (psiFile == null) return false;
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(sourceElement.get(), PsiMethod.class);
        PsiClass psiClass = psiMethod.getContainingClass();
        configuration.setName("Evm" + psiClass.getName());
        configuration.setMethodName(psiMethod.getName());
        configuration.setContractName(psiClass.getQualifiedName());
        isContextValid(context, sourceElement);
        return true;

    }

    @Override
    public boolean isConfigurationFromContext(@NotNull EvmRunConfiguration configuration, @NotNull ConfigurationContext context) {
        return false;
    }

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return new EvmConfigurationFactory(new EvmRunConfigurationType());
    }


    private boolean isContextValid(ConfigurationContext context, Ref<PsiElement> sourceElement) {
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(sourceElement.get(), PsiMethod.class);
        psiMethod.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                super.visitLocalVariable(variable);

                if (variable.getType().toString().equals("PsiType:Web3j") && variable.hasInitializer()) {
                    System.out.println(variable.getName() + " exists and it's initialised");
                }
                if (variable.getType().toString().equals("PsiType:Web3j") &&  !variable.hasInitializer()) {
                    System.out.println(variable.getName() + " exists and it's not initialised");
                }

            }
        });
        return true;
    }
}
