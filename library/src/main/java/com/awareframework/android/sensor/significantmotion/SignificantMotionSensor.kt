package com.awareframework.android.sensor.significantmotion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.model.SensorConfig
import com.awareframework.android.sensor.significantmotion.model.SignificantMotionData
import kotlin.math.abs
import kotlin.math.sqrt


/**
 * This sensor is used to track device significant motion.
 * Also used internally by AWARE if available to save battery when the device is still with high-frequency sensors
 * Based of:
 * https://github.com/sensorplatforms/open-sensor-platform/blob/master/embedded/common/alg/significantmotiondetector.c
 *
 * @author  sercant
 * @date 27/08/2018
 */
class SignificantMotionSensor : AwareSensor(), SensorEventListener {

    companion object {
        const val TAG = "AWARE::Significant"


        /**
         * Broadcasted when there is significant motion
         */
        const val ACTION_AWARE_SIGNIFICANT_MOTION_STARTED = "ACTION_AWARE_SIGNIFICANT_MOTION_STARTED"
        const val ACTION_AWARE_SIGNIFICANT_MOTION_ENDED = "ACTION_AWARE_SIGNIFICANT_MOTION_ENDED"

        const val ACTION_AWARE_SIGNIFICANT_MOTION_START = "com.awareframework.android.sensor.significantmotion.SENSOR_START"
        const val ACTION_AWARE_SIGNIFICANT_MOTION_STOP = "com.awareframework.android.sensor.significantmotion.SENSOR_STOP"

        const val ACTION_AWARE_SIGNIFICANT_MOTION_SET_LABEL = "com.awareframework.android.sensor.significantmotion.ACTION_AWARE_SIGNIFICANT_MOTION_SET_LABEL"
        const val EXTRA_LABEL = "label"

        const val ACTION_AWARE_SIGNIFICANT_MOTION_SYNC = "com.awareframework.android.sensor.significantmotion.SENSOR_SYNC"

        val CONFIG = Config()

        var currentInterval: Int = 0
            private set

        fun start(context: Context, config: Config? = null) {
            if (config != null)
                CONFIG.replaceWith(config)
            context.startService(Intent(context, SignificantMotionSensor::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SignificantMotionSensor::class.java))
        }

        private var lastSignificantMotionState = false
        var currentSignificantMotionState = false
        private const val significantMotionThreshold = 1.0

        var isSignificantMotionActive = false
            private set
    }

    private lateinit var mSensorManager: SensorManager
    private var mAccelerometer: Sensor? = null
    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler


    private val buffer: ArrayList<Float> = ArrayList()

    private val significantMotionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                ACTION_AWARE_SIGNIFICANT_MOTION_SET_LABEL -> {
                    intent.getStringExtra(EXTRA_LABEL)?.let {
                        CONFIG.label = it
                    }
                }

                ACTION_AWARE_SIGNIFICANT_MOTION_SYNC -> onSync(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        initializeDbEngine(CONFIG)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER)

        sensorThread = HandlerThread(TAG)
        sensorThread.start()

        sensorHandler = Handler(sensorThread.looper)

        registerReceiver(significantMotionReceiver, IntentFilter().apply {
            addAction(ACTION_AWARE_SIGNIFICANT_MOTION_SET_LABEL)
            addAction(ACTION_AWARE_SIGNIFICANT_MOTION_SYNC)
        })

        logd("SignificantMotion service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        return if (mAccelerometer != null) {
            mSensorManager.registerListener(
                    this,
                    mAccelerometer,
                    SensorManager.SENSOR_DELAY_UI,
                    sensorHandler)

            isSignificantMotionActive = true
            logd("SignificantMotion service active.")

            START_STICKY
        } else {
            logw("This device doesn't have a accelerometer sensor!")

            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        sensorHandler.removeCallbacksAndMessages(null)
        mSensorManager.unregisterListener(this, mAccelerometer)
        sensorThread.quit()

        dbEngine?.close()

        unregisterReceiver(significantMotionReceiver)

        isSignificantMotionActive = false

        logd("SignificantMotion service terminated...")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //We log current accuracy on the sensor changed event
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]


        val mSignificantEnergy = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        buffer.add(abs(mSignificantEnergy))

        if (buffer.size == 40) {
            //remove oldest value
            buffer.removeAt(0)

            var maxEnergy = -1.0f
            for (e in buffer) {
                if (e >= maxEnergy) maxEnergy = e
            }

            if (maxEnergy >= significantMotionThreshold) {
                currentSignificantMotionState = true
            } else if (maxEnergy < significantMotionThreshold) {
                currentSignificantMotionState = false
            }

            if (currentSignificantMotionState != lastSignificantMotionState) {
                val data = SignificantMotionData(
                        isMoving = currentSignificantMotionState
                )

                dbEngine?.save(data, SignificantMotionData.TABLE_NAME)
                logd("Significant motion: $data")

                if (currentSignificantMotionState) {
                    CONFIG.sensorObserver?.onSignificantMotionStart()
                    sendBroadcast(Intent(ACTION_AWARE_SIGNIFICANT_MOTION_STARTED))
                } else {
                    CONFIG.sensorObserver?.onSignificantMotionEnd()
                    sendBroadcast(Intent(ACTION_AWARE_SIGNIFICANT_MOTION_ENDED))
                }
            }

            lastSignificantMotionState = currentSignificantMotionState
        }
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(SignificantMotionData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    interface Observer {
        fun onSignificantMotionStart()
        fun onSignificantMotionEnd()
    }

    data class Config(
            /**
             * For real-time observation of the sensor data collection.
             */
            var sensorObserver: Observer? = null
    ) : SensorConfig(dbPath = "aware_significant_motion") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
            }
        }
    }

    class SignificantMotionSensorBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        start(context)
                    }
                }

                ACTION_AWARE_SIGNIFICANT_MOTION_STOP,
                SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    stop(context)
                }

                ACTION_AWARE_SIGNIFICANT_MOTION_START -> {
                    start(context)
                }
            }
        }
    }
}

private fun logd(text: String) {
    if (SignificantMotionSensor.CONFIG.debug) Log.d(SignificantMotionSensor.TAG, text)
}

private fun logw(text: String) {
    Log.w(SignificantMotionSensor.TAG, text)
}