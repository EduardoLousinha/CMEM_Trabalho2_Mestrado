package intro.android.goncalo_quesado

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import intro.android.goncalo_quesado.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount: Int = 0
    private var stepGoal: Int = 0

    private lateinit var sharedPreferences: SharedPreferences
    private val stepCountKey = "step_count"
    private val stepGoalKey = "step_goal"
    private val lastResetDateKey = "last_reset_date"

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

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("step_tracker", Context.MODE_PRIVATE)

        // Retrieve step count, step goal, and last reset date from shared preferences
        stepCount = sharedPreferences.getInt(stepCountKey, 0)
        stepGoal = sharedPreferences.getInt(stepGoalKey, 0)

        // Reset step count if it's a new day
        resetStepCountIfNeeded()

        // Update UI with current step count and step goal
        updateStepCount(stepCount)
        binding.editTextStepGoal.setText(stepGoal.toString())

        // Botão para abrir o scanner
        binding.buttonOpenScanner.setOnClickListener {
            requestCameraPermission()
        }

        // Inicializar o SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


        if (stepSensor == null) {
            // Caso o dispositivo não tenha um sensor de contagem de passos
            Log.e("MainActivity", "Sensor de contagem de passos não suportado")
        }

        // Oermissão ACTIVITY_RECOGNITION
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
                    saveStepGoal(stepGoal) // Save the updated goal to shared preferences
                    updateStepCount(stepCount) // Update UI with the new goal
                }
            }
        })

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
                        binding.textViewQrType.text = "URL"
                        binding.textViewQrContent.text = barcode.url?.url
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
        // listener do sensor se permissão for concedida
        if (isPermissionGranted(activityRecognitionPermission)) {
            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Parar  o sensor de contagem de passos
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            // Update step count immediately when a sensor event is received
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
        // Update step count and progress towards goal
        val textViewStepCount = findViewById<TextView>(R.id.textViewStepCount)
        textViewStepCount.text = "Steps: $steps / $stepGoal "
        Log.d("MainActivity", "Step count updated: $steps")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onResume()
            } else {
                Log.e("MainActivity", "Permissão ACTIVITY_RECOGNITION negada")
            }
        }
    }

    private fun resetStepCountIfNeeded() {
        val today = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(today.time)

        val lastResetDate = sharedPreferences.getString(lastResetDateKey, "")

        if (lastResetDate != todayStr) {
            // Reset step count for a new day
            stepCount = 0
            saveStepCount(stepCount)

            // Update last reset date in shared preferences
            with(sharedPreferences.edit()) {
                putString(lastResetDateKey, todayStr)
                apply()
            }
        }
    }
    private fun saveStepCount(count: Int) {
        with(sharedPreferences.edit()) {
            putInt(stepCountKey, count)
            apply()
        }
    }

    private fun saveStepGoal(goal: Int) {
        with(sharedPreferences.edit()) {
            putInt(stepGoalKey, goal)
            apply()
        }
    }
}
