package intro.android.goncalo_quesado

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import intro.android.goncalo_quesado.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : AppCompatActivity() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding

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
}
