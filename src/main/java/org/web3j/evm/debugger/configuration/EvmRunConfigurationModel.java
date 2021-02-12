package org.web3j.evm.debugger.configuration;

import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

public class EvmRunConfigurationModel {
    private EvmSettingsEditor listener = null;
    private List<Object> contracts = Collections.emptyList();
    private Project project;

    public EvmRunConfigurationModel(Project project) {
        this.project = project;
    }


    public void setListener(EvmSettingsEditor listener) {
        this.listener = listener;
    }

    private void applyTo(EvmRunConfiguration configuration) {
        configuration.setContractName("");
    }
}
