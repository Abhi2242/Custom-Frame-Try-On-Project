package com.smartgeek.testcamerapreview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var captureImage: Button
    private lateinit var btnTryOn: Button
    private lateinit var btnRetake: Button
    private lateinit var displayImage: ImageView
    private lateinit var dottedCircle: ImageView
    private lateinit var myCameraPreview: PreviewView
    private lateinit var imageUri: Uri
    private lateinit var view1: View
    private lateinit var view2: View
    private lateinit var view3: View
    private lateinit var view4: View

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureImage = findViewById(R.id.btn_capture)
        displayImage = findViewById(R.id.iv_display_image)
        myCameraPreview = findViewById(R.id.camera_preview)
        dottedCircle = findViewById(R.id.iv_dotted_circle)
        btnTryOn = findViewById(R.id.btn_try_on)
        btnRetake = findViewById(R.id.btn_retake)
        view1 = findViewById(R.id.view1)
        view2 = findViewById(R.id.view2)
        view3 = findViewById(R.id.view3)
        view4 = findViewById(R.id.view4)

        // Check camera permissions if permission granted start camera else ask for the permission
        if (checkMyPermission()) {
            showMessage()
            startCamera()
        } else {
            requestPermission()
        }

        // set on click listener for the captureImage it calls a method which is implemented below
        captureImage.setOnClickListener {
            takePhoto()
        }

        // start camera to retake image
        btnRetake.setOnClickListener {
            displayImage.visibility = View.GONE
            btnTryOn.visibility = View.GONE
            btnRetake.visibility = View.GONE
            myCameraPreview.visibility = View.VISIBLE
            captureImage.visibility = View.VISIBLE
            dottedCircle.visibility = View.VISIBLE
            view1.visibility = View.VISIBLE
            view2.visibility = View.VISIBLE
            view3.visibility = View.VISIBLE
            view4.visibility = View.VISIBLE
            startCamera()
            showMessage()
        }

        btnTryOn.setOnClickListener {
            startActivity(Intent(this, DisplayImageActivity::class.java).putExtra("MyImage",imageUri.toString()))
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    // set the saved uri to the image view
                    displayImage.visibility = View.VISIBLE
                    btnTryOn.visibility = View.VISIBLE
                    btnRetake.visibility = View.VISIBLE
                    myCameraPreview.visibility = View.GONE
                    captureImage.visibility = View.GONE
                    dottedCircle.visibility = View.GONE
                    view1.visibility = View.GONE
                    view2.visibility = View.GONE
                    view3.visibility = View.GONE
                    view4.visibility = View.GONE
                    displayImage.setImageURI(savedUri)

                    //save Image uri and using this variable for sending image to other activity
                    imageUri = savedUri

                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)
                }
            })
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(myCameraPreview.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder().build()

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    // Checking camera permissions
    private fun checkMyPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }


    // Requesting camera permissions
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA
            ), REQUEST_CODE_PERMISSIONS
        )
    }


    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (checkMyPermission()) {
                showMessage()
                startCamera()
            } else {
                // If permissions are not granted, show Toast message
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun showMessage(){
        // Create the object of AlertDialog Builder class
        val builder = AlertDialog.Builder(this)

        builder.setIcon(R.drawable.crisis_alert_yellow)
        builder.setTitle("Follow This Instructions !")  // Set Alert Title
        builder.setMessage("- Place your face inside Green Circle.\n" +
                "- Make sure your eyebrows are aligned with green line horizontally.\n" +
                "- Make sure your face is in center horizontally")  // Set the message show for the Alert time

        // Set Cancelable false
        builder.setCancelable(false)

        builder.setNeutralButton("Ok") {
            // When the user click Ok button then Dialog Box will close
                dialog, _ -> dialog.cancel()
        }

        // Create the Alert dialog and Show the Alert Dialog box
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.window?.setLayout(1000, 620)

    }


    companion object {
        private const val TAG = "SMARTGEEK CameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 100
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}