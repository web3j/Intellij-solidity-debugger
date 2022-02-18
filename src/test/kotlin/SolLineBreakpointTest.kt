import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.web3j.evm.debugger.breakpoint.SolidityBreakPointType

class SolLineBreakpointTest {

    @Mock
    val project = Mockito.mock(Project::class.java)

    @Mock
    val virtualFile = Mockito.mock(VirtualFile::class.java)

//    @Test
//    internal fun `test break point on sol file`() {
//        `when`(virtualFile.fileType).thenReturn(SolidityFileType)
//        val breakpointType = SolidityLineBreakpointType()
//        assertTrue(breakpointType.canPutAt(virtualFile, 20, project))
//    }

    @Test
    internal fun `test break point on different file`() {
        `when`(virtualFile.fileType).thenReturn(PlainTextFileType.INSTANCE)
        val breakpointType = SolidityBreakPointType()
        assertFalse(breakpointType.canPutAt(virtualFile, 20, project))
    }
}