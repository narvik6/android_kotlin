package com.example.module4_t3to14.task14

import com.example.module4_t3to14.R

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel

class CompassViewModel : ViewModel() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var sensorListener: SensorEventListener? = null

    var onAzimuthChanged: ((Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var isAccelerometerSet = false
    private var isMagnetometerSet = false
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)

    fun initSensors(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            onError?.invoke("Устройство не поддерживает датчик ориентации")
            return
        }

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                        isAccelerometerSet = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                        isMagnetometerSet = true
                    }
                }

                if (isAccelerometerSet && isMagnetometerSet) {
                    calculateAzimuth()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

            }
        }
    }

    private fun calculateAzimuth() {
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            lastAccelerometer,
            lastMagnetometer
        )

        SensorManager.getOrientation(rotationMatrix, orientationAngles)


        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()


        azimuth = (azimuth + 360) % 360

        onAzimuthChanged?.invoke(azimuth)
    }

    fun startListening() {
        sensorListener?.let { listener ->
            accelerometer?.let {
                sensorManager?.registerListener(
                    listener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

            magnetometer?.let {
                sensorManager?.registerListener(
                    listener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }
    }

    fun stopListening() {
        sensorListener?.let {
            sensorManager?.unregisterListener(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}