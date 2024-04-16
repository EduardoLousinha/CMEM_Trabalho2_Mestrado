package intro.android.goncalo_quesado

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
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount: Int = 0

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
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
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
            // Atualize a contagem de passos quando houver uma mudança no sensor
            stepCount = event.values[0].toInt()
            updateStepCount(stepCount)
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
        // Atualizar passos
        val textViewStepCount = findViewById<TextView>(R.id.textViewStepCount)
        textViewStepCount.text = "Steps: $steps"
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
}
