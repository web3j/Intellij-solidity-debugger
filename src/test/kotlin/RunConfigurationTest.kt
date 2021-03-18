import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase


class RunConfigurationTest : BasePlatformTestCase() {

    fun testProjectCreation() {
        myFixture.addFileToProject("Greeter.sol", "")
        println("This is a test")

    }

}


