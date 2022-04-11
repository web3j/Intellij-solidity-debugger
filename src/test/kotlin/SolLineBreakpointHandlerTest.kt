import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl
import org.junit.Test
import org.mockito.Mockito.mock
import org.web3j.evm.debugger.breakpoint.SolidityBreakpointProperties
import org.web3j.evm.debugger.breakpoint.SolidityBreakpointHandler

class SolLineBreakpointHandlerTest {


    val solidityLineBreakpointTest = SolidityBreakpointHandler()
    val mockedBreakpoint = mock(XLineBreakpointImpl::class.java)

    @Test
    fun `test that breakpoint is registered`() {
        solidityLineBreakpointTest.registerBreakpoint(mockedBreakpoint as XLineBreakpoint<SolidityBreakpointProperties>)
//        verify(debugProcess).addBreakpoint(mockedBreakpoint)
    }

    @Test
    fun `test that breakpoint is unregistered`() {
        solidityLineBreakpointTest.registerBreakpoint(mockedBreakpoint as XLineBreakpoint<SolidityBreakpointProperties>)
        solidityLineBreakpointTest.unregisterBreakpoint(mockedBreakpoint as XLineBreakpoint<SolidityBreakpointProperties>,false)
//        verify(debugProcess).removeBreakpoint(mockedBreakpoint)
    }

}