package org.web3j.evm.debugger.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EvmRunConfigurationType implements ConfigurationType {
    @Override
    public @NotNull
    String getDisplayName() {
        return "Web3j EVM";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Web3j Evm run configuration";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.General.Information;
    }

    @Override
    public @NotNull
    @NonNls
    String getId() {
        return "EVM_RUN_CONFIGURATION";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new EvmConfigurationFactory(this)};
    }
}
