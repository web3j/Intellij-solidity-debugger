/*
 * Copyright 2021 Web3 Labs Ltd.
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
package org.web3j.evm.debugger

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase
import me.serce.solidity.lang.SolidityFileType

class DebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
    override fun getFileType(): FileType {
        return SolidityFileType
    }
    override fun getContextElement(virtualFile: VirtualFile, offset: Int, project: Project): PsiElement? {
        return super.getContextElement(virtualFile, offset, project)
    }

    override fun createExpressionCodeFragment(
        project: Project,
        text: String,
        context: PsiElement?,
        isPhysical: Boolean
    ): PsiFile {
        val fileName = context?.containingFile?.name as String
        return PsiFileFactory.getInstance(project)!!.createFileFromText(
            fileName,
            SolidityFileType,
            text
        )
    }
}