package org.web3j.evm.debugger.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EvmRunConfiguration extends LocatableConfigurationBase<EvmRunConfigurationOptions> {

    private final Project project;

    protected EvmRunConfiguration(@NotNull Project project, @Nullable ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
        this.project = project;
    }

    public String getContractName() {
        return getOptions().getContract();
    }

    public void setContractName(String scriptName) {
        getOptions().setContract(scriptName);
    }

    public String getMethodName() {
        return getOptions().getMethod();
    }

    public void setMethodName(String scriptName) {
        getOptions().setMethod(scriptName);
    }

    public String getWalletPath() {
        return getOptions().getWalletPath();
    }

    public void setWalletPath(String scriptName) {
        getOptions().setWalletPath(scriptName);
    }

    public String getWalletPassword() {
        return getOptions().getWalletPassword();
    }

    public void setWalletPassword(String scriptName) {
        getOptions().setWalletPassword(scriptName);
    }
    /*
    public String getPrivateKey() {
        return getOptions().getPrivateKey();
    }

    public void setPrivateKey(String scriptName) {
        getOptions().setPrivateKey(scriptName);
    }
*/

    @NotNull
    @Override
    protected EvmRunConfigurationOptions getOptions() {
        return (EvmRunConfigurationOptions) super.getOptions();
    }

    @Override
    public @NotNull
    SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new EvmSettingsEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new CommandLineState(executionEnvironment) {
            @NotNull
            @Override
            protected ProcessHandler startProcess() throws ExecutionException {
                GeneralCommandLine commandLine = new GeneralCommandLine(getOptions().getContract(), getOptions().getMethod(), getOptions().getWalletPath(), getOptions().getWalletPassword());
                OSProcessHandler processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine);
                ProcessTerminatedListener.attach(processHandler);
                return processHandler;
            }
        };
    }


}
