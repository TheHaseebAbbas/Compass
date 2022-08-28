package com.example.compassapp

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.compassapp.Compass.NoSensorDetected
import com.example.compassapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var compass: Compass
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        compass = Compass(
            applicationContext,
            binding.compassDial,
            binding.tvDegrees,
            object : NoSensorDetected {
                override fun onNoSensorDetected() {
                    AlertDialog.Builder(applicationContext)
                        .setTitle("No Sensor Detected")
                        .setMessage("Your device doesn't support compass")
                        .setCancelable(false)
                        .setNegativeButton("Close") { _, _ -> finish() }
                        .create()
                        .show()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        compass.stop()
    }

    override fun onResume() {
        super.onResume()
        compass.start()
    }
}