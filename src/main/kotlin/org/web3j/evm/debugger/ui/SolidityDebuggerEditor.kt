/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.evm.debugger.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.ui.XDebuggerEditorBase
import me.serce.solidity.lang.SolidityLanguage
import javax.swing.JComponent


class SolidityDebuggerEditor(
    project: Project?,
    debuggerEditorsProvider: XDebuggerEditorsProvider, mode: EvaluationMode,
    historyId: String?, sourcePosition: XSourcePosition?
) : XDebuggerEditorBase(project, debuggerEditorsProvider, mode, historyId, sourcePosition) {

    override fun getEditor(): Editor {
        return FileEditorManager.getInstance(project).selectedTextEditor as Editor
    }

    override fun getComponent(): JComponent {
        return super.getEditorComponent()
    }

    override fun doSetText(text: XExpression?) {
        TODO("Not yet implemented")

    }

    override fun getExpression(): XExpression {

        val myExpression = XExpressionImpl("string greeting", SolidityLanguage.baseLanguage, "No custom info")

        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        return editorsProvider.createExpression(
            project,
            editor!!.document,
            myExpression.language,
            myExpression.mode
        );
    }

    override fun getPreferredFocusedComponent(): JComponent {
        TODO("Not yet implemented")
    }

    override fun selectAll() {
        TODO("Not yet implemented")
    }

    fun setExpression() {
        TODO("Not yet implemented")
    }
}