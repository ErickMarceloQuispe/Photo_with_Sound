package unsa.edu.laboratory9_v2

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.util.Size
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Componentes Visuales
    private lateinit var btnTakePhoto: Button
    private lateinit var previewPhoto: SurfaceView

    // Peticion de permisos al usuarios
    private lateinit var activityResultLauncher: ActivityResultLauncher<String>

    // Atributos realcionados a la camara.
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageReader: ImageReader
    private val resolution: Size = Size(480, 640)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        this.imageReader =
            ImageReader.newInstance(resolution.width, resolution.height, ImageFormat.JPEG, 1)
        this.btnTakePhoto = findViewById(R.id.btn_photo)
        this.previewPhoto = findViewById(R.id.surface_view)
        this.btnTakePhoto.setOnClickListener {}
        this.activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                } else {
                    Toast.makeText(
                        this,
                        "Es necesario dar permiso de acceso a la camara.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        this.activityResultLauncher.launch(Manifest.permission.CAMERA)
    }
}