package com.example.compassapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import java.util.*
import kotlin.math.roundToInt

class Compass(
    context: Context,
    private val compassDial: ImageView,
    private val statusTextView: TextView?,
    private val listener: NoSensorDetected
) : SensorEventListener {

    private val sides = intArrayOf(0, 45, 90, 135, 180, 225, 270, 315, 360)
    private val names = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")

    private var sensorManager: SensorManager? = null
    private var sensorAccelerometer: Sensor? = null
    private var sensorMagnetometer: Sensor? = null
    private var sensorRotationVector: Sensor? = null
    private var hasMagnetometer = false
    private var hasAccelerometer = false
    private var hasRotationVector = false

    private val floatGravity = FloatArray(3)
    private val floatGeoMagnetic = FloatArray(3)

    private val matrixR = FloatArray(9)
    private val matrixI = FloatArray(9)
    private val orientation = FloatArray(3)

    private var isAccelerometerSet = false
    private var isMagnetometerSet = false

    private var azimuth = 0f
    private var currentAzimuth = 0f

    init {
        sensorManager = context.getSystemService(SensorManager::class.java) as SensorManager
    }

    interface NoSensorDetected {
        fun onNoSensorDetected()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val alpha = 0.97f
        synchronized(this) {
            if (sensorEvent.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(matrixR, sensorEvent.values)
                SensorManager.getOrientation(matrixR, orientation)
            }
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                floatGravity[0] =
                    alpha * floatGravity[0] + (1 - alpha) * sensorEvent.values[0]
                floatGravity[1] =
                    alpha * floatGravity[1] + (1 - alpha) * sensorEvent.values[1]
                floatGravity[2] =
                    alpha * floatGravity[2] + (1 - alpha) * sensorEvent.values[2]
                isAccelerometerSet = true
            }
            if (sensorEvent.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                floatGeoMagnetic[0] =
                    alpha * floatGeoMagnetic[0] + (1 - alpha) * sensorEvent.values[0]
                floatGeoMagnetic[1] =
                    alpha * floatGeoMagnetic[1] + (1 - alpha) * sensorEvent.values[1]
                floatGeoMagnetic[2] =
                    alpha * floatGeoMagnetic[2] + (1 - alpha) * sensorEvent.values[2]
                isMagnetometerSet = true
            }
            if (isAccelerometerSet && isMagnetometerSet) {
                SensorManager.getRotationMatrix(matrixR, matrixI, floatGravity, floatGeoMagnetic)
                SensorManager.getOrientation(matrixR, orientation)
            }
            azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            azimuth = (azimuth + 360) % 360
            val anim: Animation = RotateAnimation(
                -currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
            )
            anim.duration = 300
            anim.repeatCount = 0
            anim.fillAfter = true
            compassDial.startAnimation(anim)
            if (statusTextView != null) {
                statusTextView.text = format(azimuth)
            }
            currentAzimuth = azimuth
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    fun start() {
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if (sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null
                && sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null
            ) {
                listener.onNoSensorDetected()
            } else {
                sensorAccelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                sensorMagnetometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                hasAccelerometer = sensorManager!!.registerListener(
                    this,
                    sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
                hasMagnetometer = sensorManager!!.registerListener(
                    this,
                    sensorMagnetometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        } else {
            sensorRotationVector = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            hasRotationVector = sensorManager!!.registerListener(
                this,
                sensorRotationVector,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        if (hasAccelerometer && hasMagnetometer) {
            sensorManager!!.unregisterListener(this, sensorAccelerometer)
            sensorManager!!.unregisterListener(this, sensorMagnetometer)
        } else if (hasRotationVector) sensorManager!!.unregisterListener(this, sensorRotationVector)
    }

    private fun format(azimuth: Float) =
        String.format(
            Locale.getDefault(),
            "%d° %s",
            azimuth.roundToInt(),
            names[findClosestIndex(azimuth.roundToInt())]
        )

    private fun findClosestIndex(target: Int): Int {
        var i = 0
        var j = sides.size
        var mid = 0
        while (i < j) {
            mid = (i + j) / 2
            // If target is less than array element, then search in left
            if (target < sides[mid]) {
                // If target is greater than previous to mid, return closest of two
                if (mid > 0 && target > sides[mid - 1]) {
                    return getClosest(mid - 1, mid, target)
                }
                // Repeat for left half
                j = mid
            } else {
                if (mid < sides.size - 1 && target < sides[mid + 1]) {
                    return getClosest(mid, mid + 1, target)
                }
                i = mid + 1 // update i
            }
        }
        // Only single element left after search
        return mid
    }

    private fun getClosest(index1: Int, index2: Int, target: Int) =
        if (target - sides[index1] >= sides[index2] - target) index2
        else index1
}