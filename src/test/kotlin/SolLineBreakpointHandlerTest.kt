import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl
import org.junit.Test
import org.mockito.Mockito.*
import org.web3j.evm.debugger.SolidityLineBreakpointHandler
import org.web3j.evm.debugger.Web3jDebugProcess

class SolLineBreakpointHandlerTest {


    val debugProcess = mock(Web3jDebugProcess::class.java)
    val solidityLineBreakpointTest = SolidityLineBreakpointHandler(debugProcess)
    val mockedBreakpoint = mock(XLineBreakpointImpl::class.java)

    @Test
    fun `test that breakpoint is registered`() {
        solidityLineBreakpointTest.registerBreakpoint(mockedBreakpoint as XLineBreakpoint<XBreakpointProperties<*>>)
        verify(debugProcess).addBreakpoint(mockedBreakpoint)
    }

    @Test
    fun `test that breakpoint is unregistered`() {
        solidityLineBreakpointTest.registerBreakpoint(mockedBreakpoint as XLineBreakpoint<XBreakpointProperties<*>>)
        solidityLineBreakpointTest.unregisterBreakpoint(mockedBreakpoint as XLineBreakpoint<XBreakpointProperties<*>>,false)
        verify(debugProcess).removeBreakpoint(mockedBreakpoint)
    }

}