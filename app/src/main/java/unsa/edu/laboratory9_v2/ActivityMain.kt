package unsa.edu.laboratory9_v2

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCaptureSession
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList


class ActivityMain : AppCompatActivity() {
    private val REQUEST_CAMERA_PERMISSION = 1
    private val TAG = MainActivity::class.java.simpleName
    private var textureView: TextureView? = null
    private var textureViewResult: TextureView? = null
    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var mCameraId: String? = null
    private var cameraDevice: CameraDevice? = null

    private var previewList: ArrayList<Surface>? =null
    private var previewListToResult: ArrayList<Surface>? =null

    private var buttonRecordShotCode: Button? =null

    private var buttonCompareShotCode: Button? =null

    private val REQ_CODE_SPREECH_INPUT=100;

    // Modo en 0 para grabar y modo en 1 para comparar
    private var audio_mode:Int? =0;
    // Fichero donde se guarda el codigo para tomar una foto por sonido
    private val FILE_NAME = "code.txt";

    //Sin uso Actual - Evidencia primera implementación
    //private var textToTakePhoto:String? = "Tomar foto";

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        initView()
        initData()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initData() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun initView() {
        textureView = findViewById(R.id.ttv_camera)
        textureViewResult = findViewById(R.id.ttv_camera_result)
        buttonRecordShotCode = findViewById(R.id.btn_set_shot_code)
        buttonCompareShotCode = findViewById(R.id.btn_compare_shot_code)
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onResume() {
        super.onResume()
        buttonRecordShotCode?.setOnClickListener{
            audio_mode=0;
            iniciarEntradaVoz()
        }
        buttonCompareShotCode?.setOnClickListener{
            audio_mode=1;
            iniciarEntradaVoz()
        }
        textureView!!.surfaceTextureListener = object : SurfaceTextureListener {
            @RequiresApi(api = Build.VERSION_CODES.M)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // 1. Configuring the camera whenTextureView is available
                initCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCamera() {
        // 2. Configure the pre-camera, get the size and ID
        getCameraIdAndPreviewSizeByFacing(CameraCharacteristics.LENS_FACING_FRONT)
        // 3. Open the camera
        openCamera()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun openCamera() {
        try {
            // 4. Permission check
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermission()
                return
            }
            // 5. Virtually open the camera
            cameraManager!!.openCamera(mCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera // Opened, save the CAMERADEVICE instance representing the camera
                    val surfaceTexture = textureView!!.surfaceTexture
                    surfaceTexture!!.setDefaultBufferSize(textureView!!.width, textureView!!.height)
                    val surface = Surface(surfaceTexture)
                    previewList = ArrayList()
                    //val previewList: ArrayList<Surface> = ArrayList()
                    previewList!!.add(surface)
                    try {
                        // 6. Pass the TEXTUREVIEW's Surface to CameraDevice
                        cameraDevice!!.createCaptureSession(previewList!!, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    builder.addTarget(surface) // must set up to preview normal
                                    val captureRequest = builder.build()
                                    // 7.cameracaptureSession and CaptureRequest Bind (this is the last step, you have displayed camera preview)
                                    session.setRepeatingRequest(captureRequest, object : CaptureCallback() {

                                    }, null)
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    releaseCamera()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    releaseCamera()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun releaseCamera() {
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    private fun getCameraIdAndPreviewSizeByFacing(lensFacingFront: Int) {
        try {
            val cameraIdList = cameraManager!!.cameraIdList
            for (cameraId in cameraIdList) {
                val cameraCharacteristics = cameraManager!!.getCameraCharacteristics(cameraId)
                val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (facing != lensFacingFront) {
                    continue
                }
                val streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputSizes = streamConfigurationMap!!.getOutputSizes(
                    SurfaceTexture::class.java
                )
                mCameraId = cameraId
                previewSize =
                    setOptimalPreviewSize(outputSizes, textureView!!.measuredWidth, textureView!!.measuredHeight)
                Log.d(
                    TAG,
                    "Best Preview Size (W-H):" + previewSize!!.width + "+" + previewSize!!.height + ", camera ID: " + mCameraId
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setOptimalPreviewSize(sizes: Array<Size>, previewViewWidth: Int, previewViewHeight: Int): Size? {
        val bigEnoughSizes: MutableList<Size> = ArrayList()
        val notBigEnoughSizes: MutableList<Size> = ArrayList()
        for (size in sizes) {
            if (size.width >= previewViewWidth && size.height >= previewViewHeight) {
                bigEnoughSizes.add(size)
            } else {
                notBigEnoughSizes.add(size)
            }
        }
        return when {
            bigEnoughSizes.size > 0 -> {
                Collections.min(bigEnoughSizes) { o1, o2 ->
                    java.lang.Long.signum(
                        o1!!.width.toLong() * o1.height -
                                o2!!.width.toLong() * o2.height
                    )
                }
            }
            notBigEnoughSizes.size > 0 -> {
                Collections.max(notBigEnoughSizes) { o1, o2 ->
                    java.lang.Long.signum(
                        o1!!.width.toLong() * o1.height -
                                o2!!.width.toLong() * o2.height
                    )
                }
            }
            else -> {
                Log.d(TAG, "No suitable preview size.")
                sizes[0]
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please grant the camera permissions!", Toast.LENGTH_SHORT).show()
            } else {
                openCamera()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    // Método que encapsula el método de tomar una fotografía singular y mostrarla en la esquina superior izquierda
    fun takePhoto(){
        val surfaceTexture = textureViewResult!!.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(textureViewResult!!.width, textureViewResult!!.height)
        val surfaceFromResult = Surface(surfaceTexture)

        previewListToResult = ArrayList()
        previewListToResult!!.add(surfaceFromResult)

        try {
            cameraDevice!!.createCaptureSession(previewListToResult!!, object : CameraCaptureSession.StateCallback() {
                @RequiresApi(Build.VERSION_CODES.M)
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        builder.addTarget(surfaceFromResult)
                        val captureRequest = builder.build()
                        session.capture(captureRequest, object : CaptureCallback() {}, null)
                        Thread.sleep(300);

                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    } finally {
                        openCamera()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    // Método para reconocer un patrón de sonido, según el modo luego de la ejecución del intent se procederá a almacenar como nueva palabra
    // disparadora o se comparará con aquella previamente definida

    fun iniciarEntradaVoz(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Escuchando ...");
        try{
            this.startActivityForResult(intent,REQ_CODE_SPREECH_INPUT);

        }catch(e: ActivityNotFoundException){

        }
    }
    // Método ejecutado una vez terminada la ejecución del Activity anterior, se implementa la funcionalidad de cada modo y se notifica
    // el resultado al usuario. El código comentado pertenece a la primera versión, ahora no tiene utilidad y pueden ser eliminados
    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        when(requestCode){
            REQ_CODE_SPREECH_INPUT->{
                if(resultCode == RESULT_OK && null != data){
                    val result: java.util.ArrayList<String>? = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if(audio_mode==0){
                        //textToTakePhoto = result?.get(0); set
                        setCode(result?.get(0));
                        Toast.makeText(applicationContext,"Mensaje de Disparo Grabado",Toast.LENGTH_SHORT).show();
                    }else if(audio_mode==1){
                        //if(result?.get(0).equals(textToTakePhoto)){
                        if(getCode().equals(result?.get(0))){
                            Toast.makeText(applicationContext,"Mensaje de Disparo Reocnocido",Toast.LENGTH_SHORT).show();
                            takePhoto();
                        }else{
                            Toast.makeText(applicationContext,"Mensaje de Disparo Incorrecto",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }
    // Función que permite guardar una cadena de texto como nuevo codigo disparador con el cual se toma una fotografía con la aplicación
    fun setCode(texto: String?){
        var fileOutputStream: FileOutputStream? = null;
        try {
            fileOutputStream = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fileOutputStream.write(texto?.toByteArray());
        }catch (e:Exception){
            e.printStackTrace()
        }finally {
            fileOutputStream?.close();
        }
    }
    // Función que permite obtener el codigo disparador como una cadena de texto para compararlo y determinar si se debiera o no tomar la fotografía
    fun getCode(): String {
        var fileInputStream:FileInputStream?=null;
        try{
            fileInputStream = openFileInput(FILE_NAME);
            var inputStreamReader:InputStreamReader= InputStreamReader(fileInputStream);
            var bufferedReader:BufferedReader = BufferedReader(inputStreamReader);
            var stringBuilder:StringBuilder = StringBuilder();
            var linea_texto:String?;
            do{
                linea_texto=bufferedReader.readLine();
                if(linea_texto!=null){
                    stringBuilder.append(linea_texto).append("\n");
                }else{
                    break;
                }
            }
            while(true);
            return stringBuilder.toString().substring(0,stringBuilder.length-1);
        }catch (e:Exception){
            e.printStackTrace();
        }finally {
            fileInputStream?.close();
        }
        return "";
    }
}