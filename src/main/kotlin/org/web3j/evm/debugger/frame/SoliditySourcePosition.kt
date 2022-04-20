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
package org.web3j.evm.debugger.frame

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XDebuggerUtilImpl
import java.nio.file.Paths
import org.web3j.evm.debugger.utils.SourceMapperUtils


class SoliditySourcePosition(
    srcFile: String,
    private val jumpLine: Int,
    private val offset: Int,
    private val project: Project
) :
    XSourcePosition {

    private val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(Paths.get(srcFile).normalize())

    override fun getLine(): Int {
        return jumpLine - 1
    }

    override fun getOffset(): Int {
        return offset
    }

    override fun getFile(): VirtualFile {
        return virtualFile!!
    }

    override fun createNavigatable(project: Project): Navigatable {
        return XDebuggerUtilImpl.createNavigatable(project, this);
    }

    fun getPsiElementAtPosition(): PsiElement? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile!!)
        val mapOfLineOffsets = SourceMapperUtils.resolveOffsetAtLine(psiFile!!.text)
        val indexWithoutWhitespace = mapOfLineOffsets[line]!!.second.indexOfFirst { c -> c != ' ' }
        return psiFile.findElementAt(mapOfLineOffsets[line]!!.first.first + indexWithoutWhitespace)
    }
}