# AWARE Significant Motion

[![jitpack-badge](https://jitpack.io/v/awareframework/com.aware.android.sensor.significantmotion.svg)](https://jitpack.io/#awareframework/com.aware.android.sensor.significantmotion)

This sensor is used to track device significant motion. Also used internally by AWARE if available to save battery when the device is still with high-frequency sensors. `SignificantMotionSensor.Observer` allows programmers to take actions on detection of a significant motion.

Based of: [sensorplatforms/open-sensor-platform/significantmotiondetector.c](https://github.com/sensorplatforms/open-sensor-platform/blob/master/embedded/common/alg/significantmotiondetector.c)

## Public functions

### SignificantMotionSensor

+ `start(context: Context, config: SignificantMotionSensor.Config?)`: Starts the significant motion sensor with the optional configuration.
+ `stop(context: Context)`: Stops the service.

### SignificantMotionSensor.Config

Class to hold the configuration of the sensor.

#### Fields

+ `sensorObserver: SignificantMotionSensor.Observer`: Callback for live data updates.
+ `enabled: Boolean` Sensor is enabled or not. (default = false)
+ `debug: Boolean` enable/disable logging to `Logcat`. (default = false)
+ `label: String` Label for the data. (default = "")
+ `deviceId: String` Id of the device that will be associated with the events and the sensor. (default = "")
+ `dbEncryptionKey` Encryption key for the database. (default =String? = null)
+ `dbType: Engine` Which db engine to use for saving data. (default = `Engine.DatabaseType.NONE`)
+ `dbPath: String` Path of the database. (default = "aware_significantmotion")
+ `dbHost: String` Host for syncing the database. (Defult = `null`)

## Broadcasts

### Fired Broadcasts

+ `SignificantMotionSensor.ACTION_AWARE_SIGNIFICANT_MOTION_STARTED` fired when there is a significant motion.
+ `SignificantMotionSensor.ACTION_AWARE_SIGNIFICANT_MOTION_ENDED` fired when the significant motion has ended.

### Received Broadcasts

+ `SignificantMotionSensor.ACTION_AWARE_SIGNIFICANT_MOTION_START`: received broadcast to start the sensor.
+ `SignificantMotionSensor.ACTION_AWARE_SIGNIFICANT_MOTION_STOP`: received broadcast to stop the sensor.
+ `SignificantMotionSensor.ACTION_AWARE_SIGNIFICANT_MOTION_SYNC`: received broadcast to send sync attempt to the host.
+ `SignificantMotionSensor.ACTION_AWARE_SIGNIFICANT_MOTION_SET_LABEL`: received broadcast to set the data label. Label is expected in the `SignificantMotionSensor.EXTRA_LABEL` field of the intent extras.

## Data Representations

### SignificantMotion Data

Contains the motion changes.

| Field     | Type    | Description                                                     |
| --------- | ------- | --------------------------------------------------------------- |
| moving    | Boolean | Indicates that a significant motion was detected or not.        |
| label     | String  | Customizable label. Useful for data calibration or traceability |
| deviceId  | String  | AWARE device UUID                                               |
| label     | String  | Customizable label. Useful for data calibration or traceability |
| timestamp | Long    | unixtime milliseconds since 1970                                |
| timezone  | Int     | [Raw timezone offset][1] of the device                          |
| os        | String  | Operating system of the device (ex. android)                    |

## Example usage

```kotlin
// To start the service.
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

// To stop the service
SignificantMotionSensor.stop(appContext)
```

## License

Copyright (c) 2018 AWARE Mobile Context Instrumentation Middleware/Framework (http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[1]: https://developer.android.com/reference/java/util/TimeZone#getRawOffset()