package me.liquaristo.reldom

import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var dataSource: Sensor? = null
    private var dataPool: MutableList<List<Float>> = mutableListOf()
    private lateinit var numberView: TextView
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        numberView = this.findViewById(R.id.randomNumber)
        imageView = this.findViewById(R.id.loadingImage)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        dataSource = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (dataSource != null) {
                /*
            with(AlertDialog.Builder(this)) {
                setTitle("Usage")
                setMessage("Tap on screen to generate a random number between 0-100. More settings coming soon.")
                setPositiveButton("OK") { _, _ -> run {} }
                setNegativeButton("Don't show again") { _, _ -> run {

                } }
                create()
            }.show()
            */
            dataSource?.also { accelerometer ->
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else {
            with(AlertDialog.Builder(this)) {
                setTitle("Error")
                setMessage("Reldom needs an accelerometer to work. Your device seems don't have one.")
                setNeutralButton("Exit") { _, _ -> finishAffinity() }
                create()
            }.show()
        }
        numberView.setOnClickListener {
            numberView.text = ""
            numberView.isClickable = false
            imageView.visibility = VISIBLE
            thread {
                while (dataPool.size < 4);
                val currentData: List<List<Float>> = dataPool.toList()
                dataPool.clear()
                val extractedData: MutableList<Int> = mutableListOf()
                for (dataGroup in currentData) {
                    val extracted = abs(dataGroup[0] * dataGroup[1] + dataGroup[2])
                    val decimal = extracted - extracted.toInt()
                    extractedData.add((decimal * 10000000).toInt())
                }
                println(extractedData.toString())
                val randomResult = extractedData[0] xor extractedData[1] xor extractedData[2] xor extractedData[3]
                vibratePhone()
                this@MainActivity.runOnUiThread(java.lang.Runnable {
                    this.numberView.text = randomResult.toString()
                    this.imageView.visibility = GONE
                    this.numberView.isClickable = true
                })
            }
        }
        numberView.setOnLongClickListener {
            Toast.makeText(applicationContext, "You pressed me.", Toast.LENGTH_SHORT).show()
            true
        }
    }

    fun vibratePhone() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(20)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val data = listOf(event!!.values[0], event.values[1], event.values[2])
        dataPool.add(data)
        if (dataPool.size > 4) {
            dataPool.removeAt(0)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        println("onAccuracyChanged")
    }

    override fun onResume() {
        super.onResume()
        dataSource?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause(){
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}