package rocks.crownstone.dev_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.komponents.kovenant.then

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("rocks.crownstone.dev_app", appContext.packageName)

        val address = "D1:6F:FA:4B:62:93"
        MainApp.instance.bluenet.connect(address)
                .then { MainApp.instance.bluenet.deviceInfo(address).getFirmwareVersion() }
                .fail { fail("$it") }
    }
}
