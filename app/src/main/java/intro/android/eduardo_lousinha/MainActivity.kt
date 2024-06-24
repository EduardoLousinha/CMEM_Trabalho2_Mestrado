package intro.android.eduardo_lousinha

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import intro.android.eduardo_lousinha.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount: Int = 0
    private var stepGoal: Int = 15
    private var distCount: Int = 0
    private var distGoal: Int = 13
    private var stepLength: Int = 90


    private lateinit var sharedPreferences: SharedPreferences
    private val stepCountKey = "step_count"
    private val stepGoalKey = "step_goal"

    private val activityRecognitionPermission = android.Manifest.permission.ACTIVITY_RECOGNITION
    private val permissionRequestCode = 100

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startScanner()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("step_tracker", Context.MODE_PRIVATE)

        stepCount = sharedPreferences.getInt(stepCountKey, 0)
        stepGoal = sharedPreferences.getInt(stepGoalKey, 0)

        updateStepCount(stepCount)

        binding.scannerButton.setOnClickListener {
            requestCameraPermission()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


        if (stepSensor == null) {

            Log.e("MainActivity", "Step counting sensor not compatible")
        }

        if (ContextCompat.checkSelfPermission(this, activityRecognitionPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(activityRecognitionPermission), permissionRequestCode)
        }

        binding.editTextStepGoal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val newGoal = s.toString().toIntOrNull()
                if (newGoal != null) {
                    stepGoal = newGoal
                    saveStepGoal(stepGoal)
                    updateStepCount(0)
                }
            }
        })

        binding.buttonClearStepCount.setOnClickListener {
            clearStepCount()
        }

    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (isPermissionGranted(cameraPermission)) {
            startScanner()
        } else {
            requestPermissionLauncher.launch(cameraPermission)
        }
    }

    private fun startScanner() {
        ScannerActivity.startScanner(this) { barcodes ->
            barcodes.forEach { barcode ->
                when (barcode.valueType) {
                    Barcode.TYPE_URL -> {
                        val url = barcode.url?.url
                        if (!url.isNullOrBlank()) {
                            openUrlInBrowser(url)
                            binding.textViewQrType.text = "URL"
                            binding.textViewQrContent.text = url
                        }
                    }
                    Barcode.TYPE_CONTACT_INFO -> {
                        binding.textViewQrType.text = "Contact"
                        binding.textViewQrContent.text = barcode.contactInfo?.toString()
                    }
                    else -> {
                        binding.textViewQrType.text = "Other"
                        binding.textViewQrContent.text = barcode.rawValue?.toString()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionGranted(activityRecognitionPermission)) {
            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val newStepCount = event.values[0].toInt()
            updateStepCount(newStepCount)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                Log.d("MainActivity", "Sensor accuracy changed to UNRELIABLE")
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                Log.d("MainActivity", "Sensor accuracy changed to LOW")
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                Log.d("MainActivity", "Sensor accuracy changed to MEDIUM")
            }
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                Log.d("MainActivity", "Sensor accuracy changed to HIGH")
            }
        }
    }

    private fun updateStepCount(steps: Int) {
        stepCount = steps
        val textViewStepCount = findViewById<TextView>(R.id.textViewStepCount)
        val textViewGoalStatus = findViewById<TextView>(R.id.textViewGoalStatus)
        textViewStepCount.text = "Steps: $stepCount / $stepGoal steps (goal)"

        val progress = stepCount.toFloat() / stepGoal.toFloat() * 100
        textViewGoalStatus.text = "Progress: %.2f%%".format(progress)

        if (stepCount >= stepGoal) {
            textViewStepCount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            textViewStepCount.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun clearStepCount() {
        stepCount = 0
        saveStepCount(stepCount)
        updateStepCount(stepCount)
    }

    private fun saveStepCount(count: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(stepCountKey, count)
        editor.apply()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onResume()
            } else {
                Log.e("MainActivity", "Permission for ACTIVITY_RECOGNITION denied!")
            }
        }
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("MainActivity", "No activity found to handle URL intent")
        }
    }


    private fun saveStepGoal(goal: Int) {
        with(sharedPreferences.edit()) {
            putInt(stepGoalKey, goal)
            apply()
        }
    }
}
