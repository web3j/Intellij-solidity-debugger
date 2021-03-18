package org.web3j.evm.debugger.run

import com.intellij.application.options.ModulesComboBox
import com.intellij.execution.MethodBrowser
import com.intellij.execution.configuration.BrowseModuleValueActionListener
import com.intellij.execution.testframework.SourceScope
import com.intellij.execution.ui.CommonJavaParametersPanel
import com.intellij.execution.ui.ConfigurationModuleSelector
import com.intellij.execution.ui.DefaultJreSelector
import com.intellij.execution.ui.JrePathEditor
import com.intellij.ide.util.TreeFileChooserFactoryImpl
import com.intellij.ide.util.TreeJavaClassChooserDialog
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.ui.PanelWithAnchor
import me.serce.solidity.ide.run.*
import me.serce.solidity.ide.run.ui.ContractBrowser
import me.serce.solidity.ide.run.ui.EditorTextFieldWithBrowseButton
import me.serce.solidity.ide.run.ui.IContractFilter
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.psi.SolContractDefinition
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class EvmSettingsEditorPanel(private val project: Project) : SettingsEditor<EvmRunConfiguration>(), PanelWithAnchor {

    private val moduleSelector: ConfigurationModuleSelector
    private lateinit var mainPanel: JPanel
    private var myModel: EvmRunConfigurationModel = EvmRunConfigurationModel(project)
    private lateinit var myModule: LabeledComponent<ModulesComboBox>
    private lateinit var commonJavaParametersPanel: CommonJavaParametersPanel
    lateinit var solidityContract: LabeledComponent<EditorTextFieldWithBrowseButton>
    lateinit var contractWrapper: LabeledComponent<EditorTextFieldWithBrowseButton>
    lateinit var wrappedMethod: LabeledComponent<EditorTextFieldWithBrowseButton>
    lateinit var walletPath: LabeledComponent<EditorTextFieldWithBrowseButton>
    lateinit var walletPassword: LabeledComponent<JTextField>
    lateinit var privateKey: LabeledComponent<JTextField>
    private lateinit var myJrePathEditor: JrePathEditor
    private var browsers = arrayListOf<BrowseModuleValueActionListener<JComponent>>()
    private val modulesComponent: ModulesComboBox
        get() = myModule.component

    init {
        moduleSelector = ConfigurationModuleSelector(project, modulesComponent)
        commonJavaParametersPanel.setModuleContext(moduleSelector.module)
        commonJavaParametersPanel.setHasModuleMacro()
        myJrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromModuleDependencies(modulesComponent, false))
        myModule.component.addActionListener { commonJavaParametersPanel.setModuleContext(moduleSelector.module) }
        myModel.setListener(this)
        setUpListeners()

    }

    override fun applyEditorTo(configuration: EvmRunConfiguration) {
        myModel.apply(configuration, this)
        commonJavaParametersPanel.applyTo(configuration)
        moduleSelector.applyTo(configuration)
        configuration.alternativeJrePath = myJrePathEditor.jrePathOrName
        configuration.isAlternativeJrePathEnabled = myJrePathEditor.isAlternativeJreSelected

    }

    override fun resetEditorFrom(configuration: EvmRunConfiguration) {
        myModel.reset(configuration,this)
        commonJavaParametersPanel.reset(configuration)
        moduleSelector.reset(configuration)
        myJrePathEditor.setPathOrName(configuration.alternativeJrePath, configuration.isAlternativeJrePathEnabled)

    }

    override fun createEditor(): JComponent {
        return mainPanel
    }

    override fun getAnchor(): JComponent? {
        return this.anchor
    }

    override fun setAnchor(anchor: JComponent?) {
        solidityContract.anchor = anchor
        contractWrapper.anchor = anchor
        wrappedMethod.anchor = anchor
        walletPath.anchor = anchor
        walletPassword.anchor = anchor
        privateKey.anchor = anchor
    }

    private fun createUIComponents() {
        solidityContract = LabeledComponent()
        solidityContract.component = EditorTextFieldWithBrowseButton(project)
        contractWrapper = LabeledComponent()
        contractWrapper.component = EditorTextFieldWithBrowseButton(project)
        wrappedMethod = LabeledComponent()
        wrappedMethod.component = EditorTextFieldWithBrowseButton(project)
        walletPath = LabeledComponent()
        walletPath.component = EditorTextFieldWithBrowseButton(project)
        walletPassword = LabeledComponent()
        walletPassword.component = JTextField("")
        privateKey = LabeledComponent()
        privateKey.component = JTextField("")
    }

    @Suppress("UNCHECKED_CAST")
    private fun setUpListeners() {
        val classChooserActionListener = ClassChooserActionListener(project)
        classChooserActionListener.setField(contractWrapper.component as ComponentWithBrowseButton<JComponent>)
        val methodChooserActionListener = classChooserActionListener.methodChooserActionListener
        methodChooserActionListener.setField(wrappedMethod.component)
        val walletChooserActionListener = WalletChooserActionListener(project)
        walletChooserActionListener.setField(walletPath.component as ComponentWithBrowseButton<JComponent>)
        val contractChooserActionListener = ContractChooserActionListener(project)
        contractChooserActionListener.setField(solidityContract.component as ComponentWithBrowseButton<JComponent>)
        browsers.add(classChooserActionListener)
        browsers.add(methodChooserActionListener)
        browsers.add(walletChooserActionListener)
        browsers.add(contractChooserActionListener)
    }


    inner class ClassChooserActionListener(project: Project) :
        BrowseModuleValueActionListener<JComponent>(project) {
        var psiClass: PsiClass? = null
        override fun setField(field: ComponentWithBrowseButton<JComponent>) {
            super.setField(field)
        }

        override fun showDialog(): String? {
            val treeJavaClassChooserDialog = TreeJavaClassChooserDialog(
                "Select a class",
                project
            )
            treeJavaClassChooserDialog.showDialog()
            val selectedClass = treeJavaClassChooserDialog.selected
            psiClass = selectedClass
            return selectedClass.qualifiedName
        }

        val methodChooserActionListener: MethodChooserActionListener
            get() = MethodChooserActionListener(project)

    }

    inner class MethodChooserActionListener(project: Project) :
        MethodBrowser(project) {
        override fun getClassName(): String {
            return contractWrapper.component.text
        }

        override fun getModuleSelector(): ConfigurationModuleSelector {
            return ConfigurationModuleSelector(project, ModulesComboBox())
        }

        override fun getFilter(testClass: PsiClass): Condition<PsiMethod> {
            return Condition { obj: PsiMethod -> obj.isValid && !obj.isDeprecated }
        }
    }

    inner class WalletChooserActionListener(project: Project) :
        BrowseModuleValueActionListener<JComponent>(project) {
        var treeFileChooserFactory: TreeFileChooserFactoryImpl = TreeFileChooserFactoryImpl(project)
        override fun showDialog(): String? {
            val fileChooser =
                treeFileChooserFactory.createFileChooser("Select your wallet.", null, JsonFileType.INSTANCE, null)
            fileChooser.showDialog()
            return fileChooser.selectedFile!!.originalFile.virtualFile.path
        }

    }

    private open inner class ContractClassBrowser(project: Project) :
        ContractBrowser(project, "Choose Contract to execute") {

        override fun findContract(contractName: String): SolContractDefinition? {
            return SearchUtils.findContract(contractName, project)
        }

        @Throws(NoFilterException::class)
        override fun filter(): IContractFilter.ContractFilterWithScope {
            val contractFilter: IContractFilter.ContractFilterWithScope
            try {
                val configurationCopy = SolidityRunConfig(
                    SolidityRunConfigModule(project),
                    SolidityConfigurationType.getInstance().configurationFactories[0]
                )
                contractFilter = ContractFilter
                    .create(SourceScope.modulesWithDependencies(configurationCopy.modules))
            } catch (e: ContractFilter.NoContractException) {
                throw NoFilterException(MessagesEx.MessageInfo(project, "Message", "title"))
            }
            return contractFilter
        }
    }

    private inner class ContractChooserActionListener(project: Project) :
        EvmSettingsEditorPanel.ContractClassBrowser(project) {

        @Throws(NoFilterException::class)
        override fun filter(): IContractFilter.ContractFilterWithScope {
            try {
                return ContractFilter.create(SourceScope.wholeProject(project))
            } catch (ignore: ContractFilter.NoContractException) {
                throw NoFilterException(
                    MessagesEx.MessageInfo(
                        project,
                        ignore.message, "Can't Browse Inheritors"
                    )
                )
            }
        }
    }
}