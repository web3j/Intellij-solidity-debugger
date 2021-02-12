package org.web3j.evm.debugger.configuration;

import com.intellij.execution.configurations.LocatableRunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

public class EvmRunConfigurationOptions extends LocatableRunConfigurationOptions {

    private final StoredProperty<String> contract = string("").provideDelegate(this, "contract");
    private final StoredProperty<String> method = string("").provideDelegate(this, "method");
    private final StoredProperty<String> walletPath = string("").provideDelegate(this, "walletPath");
    private final StoredProperty<String> walletPassword = string("").provideDelegate(this, "walletPassword");
  //  private final StoredProperty<String> privateKey = string("").provideDelegate(this, "privateKey");

    /*
    public String getPrivateKey() {
        return privateKey.getValue(this);
    }
*/
    public String getContract() {
        return contract.getValue(this);
    }

    public String getMethod() {
        return method.getValue(this);
    }

    public String getWalletPath() {
        return walletPath.getValue(this);
    }

    public String getWalletPassword() {
        return walletPassword.getValue(this);
    }

    public void setContract(String value) {
        contract.setValue(this, value);
    }

    public void setMethod(String value) {
        method.setValue(this, value);
    }

    public void setWalletPath(String value) {
        walletPath.setValue(this, value);
    }

    public void setWalletPassword(String value) {
        walletPassword.setValue(this, value);
    }
/*
    public void setPrivateKey(String value){
        privateKey.setValue(this,value);
    }
    */
}
