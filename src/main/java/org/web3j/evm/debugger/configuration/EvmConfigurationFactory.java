package org.web3j.evm.debugger.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class EvmConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Web3j Evm configuration factory";


    protected EvmConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new EvmRunConfiguration(project, this, "EVM");
    }

    @Override
    public @NotNull String getName() {
        return FACTORY_NAME;
    }

    @Override
    public @NotNull String getId() {
        return "EVM_RUN_CONFIGURATION";
    }

    @Override
    public Class<? extends BaseState> getOptionsClass() {
        return EvmRunConfigurationOptions.class;
    }

}
