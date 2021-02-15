package org.web3j.evm.debugger.configuration;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.MethodBrowser;
import com.intellij.execution.configuration.BrowseModuleValueActionListener;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactoryImpl;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import me.serce.solidity.ide.run.ui.EditorTextFieldWithBrowseButton;
import me.serce.solidity.lang.SolidityFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.web3j.evm.debugger.run.EvmRunConfiguration;
import org.web3j.evm.debugger.run.EvmRunConfigurationModel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class EvmSettingsEditor extends SettingsEditor<EvmRunConfiguration> {

    private JPanel mainPanel;
    private Project project;
    private EvmRunConfigurationModel model;
    private ConfigurationModuleSelector moduleSelector;
    private LabeledComponent<ModulesComboBox> myModule;
    private CommonJavaParametersPanel commonJavaParametersPanel;
    private LabeledComponent<EditorTextFieldWithBrowseButton> contract;
    private LabeledComponent<EditorTextFieldWithBrowseButton> method;
    private LabeledComponent<EditorTextFieldWithBrowseButton> walletPath;
    private LabeledComponent<JTextField> walletPassword;
    //  private LabeledComponent<JTextField> privateKey;

    private CommonJavaParametersPanel myCommonJavaParameters;
    private JrePathEditor myJrePathEditor;
    private List<BrowseModuleValueActionListener<JComponent>> browsers;


    public EvmSettingsEditor(final Project project) {
        this.project = project;
        this.model = new EvmRunConfigurationModel(project);
        this.browsers = new ArrayList<>();
        setUpListeners();
        this.commonJavaParametersPanel = new CommonJavaParametersPanel();
        this.moduleSelector = new ConfigurationModuleSelector(project, new ModulesComboBox());
        commonJavaParametersPanel.setModuleContext(moduleSelector.getModule());
        commonJavaParametersPanel.setHasModuleMacro();
    }


    private void setUpListeners() {
        ClassChooserActionListener classChooserActionListener = new ClassChooserActionListener(project);
        MethodChooserActionListener methodChooserActionListener = classChooserActionListener.getMethodChooserActionListener();
        classChooserActionListener.setField((ComponentWithBrowseButton) contract.getComponent());
        WalletChooserActionListener walletChooserActionListener = new WalletChooserActionListener(project);
        methodChooserActionListener.setField(method.getComponent());
        walletChooserActionListener.setField((ComponentWithBrowseButton) walletPath.getComponent());
        browsers.add(classChooserActionListener);
        browsers.add(methodChooserActionListener);
        browsers.add(walletChooserActionListener);

    }


    @Override
    protected void resetEditorFrom(@NotNull EvmRunConfiguration s) {
        contract.getComponent().setText(s.getContractName());
        method.getComponent().setText(s.getMethodName());
        walletPath.getComponent().setText(s.getWalletPath());
        walletPassword.getComponent().setText(s.getWalletPassword());
        //  privateKey.getComponent().setText(s.getPrivateKey());
    }

    @Override
    protected void applyEditorTo(@NotNull EvmRunConfiguration s) {
        s.setContractName(contract.getComponent().getText());
        s.setMethodName(method.getComponent().getText());
        s.setWalletPath(walletPath.getComponent().getText());
        s.setWalletPassword(walletPassword.getComponent().getText());
        //  s.setPrivateKey(privateKey.getComponent().getText());

    }

    @Override
    protected @NotNull
    JComponent createEditor() {

        return mainPanel;
    }

    private void createUIComponents() {
        contract = new LabeledComponent<>();
        contract.setComponent(new EditorTextFieldWithBrowseButton(project));
        method = new LabeledComponent<>();
        method.setComponent(new EditorTextFieldWithBrowseButton(project));
        walletPath = new LabeledComponent<>();
        walletPath.setComponent(new EditorTextFieldWithBrowseButton(project));
        walletPassword = new LabeledComponent<>();
        walletPassword.setComponent(new JTextField("", 1));

    }



    private class ClassChooserActionListener extends BrowseModuleValueActionListener<JComponent> {
        Project project;
        PsiClass psiClass;

        protected ClassChooserActionListener(Project project) {
            super(project);
            this.project = project;
        }

        @Override
        public void setField(@NotNull ComponentWithBrowseButton<JComponent> field) {
            super.setField(field);
        }

        @Override
        protected @Nullable
        String showDialog() {

            TreeJavaClassChooserDialog treeJavaClassChooserDialog = new TreeJavaClassChooserDialog("Select a class", project);
            treeJavaClassChooserDialog.showDialog();
            PsiClass selectedClass = treeJavaClassChooserDialog.getSelected();
            this.psiClass = selectedClass;
            return selectedClass.getQualifiedName();
        }

        private MethodChooserActionListener getMethodChooserActionListener() {
            return new MethodChooserActionListener(project);
        }
    }

    private class MethodChooserActionListener extends MethodBrowser {
        private final Project project;

        public MethodChooserActionListener(Project project) {
            super(project);
            this.project = project;
        }

        @Override
        protected String getClassName() {
            return contract.getComponent().getText();
        }

        @Override
        protected ConfigurationModuleSelector getModuleSelector() {
            return new ConfigurationModuleSelector(project, new ModulesComboBox());
        }

        @Override
        protected Condition<PsiMethod> getFilter(PsiClass testClass) {
            return PsiElement::isValid;
        }
    }

    private class WalletChooserActionListener extends BrowseModuleValueActionListener<JComponent> {
        TreeFileChooserFactoryImpl treeFileChooserFactory;

        protected WalletChooserActionListener(Project project) {
            super(project);
            treeFileChooserFactory = new TreeFileChooserFactoryImpl(project);
        }

        @Override
        protected @Nullable
        String showDialog() {
            TreeFileChooser fileChooser = treeFileChooserFactory.createFileChooser("Select your wallet.", null, SolidityFileType.INSTANCE, null);
            fileChooser.showDialog();
            return fileChooser.getSelectedFile().getOriginalFile().getVirtualFile().getPath();
        }
    }
}





