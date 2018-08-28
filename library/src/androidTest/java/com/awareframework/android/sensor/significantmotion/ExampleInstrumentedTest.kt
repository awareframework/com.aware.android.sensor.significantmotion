package com.awareframework.android.sensor.significantmotion

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.awareframework.android.core.db.Engine
import com.awareframework.android.sensor.significantmotion.model.SignificantMotionData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 * <p>
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.aware.android.sensor.significantmotion.test", appContext.packageName)

        SignificantMotionSensor.start(appContext, SignificantMotionSensor.Config().apply {
            sensorObserver = object : SignificantMotionSensor.Observer {
                override fun onSignificantMotionStart() {
                    // your code here...
                }

                override fun onSignificantMotionEnd() {
                    // your code here...
                }
            }
            dbType = Engine.DatabaseType.ROOM
            debug = true
            // more configuration...
        })
    }
}
