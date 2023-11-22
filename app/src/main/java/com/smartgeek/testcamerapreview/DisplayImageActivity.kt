package com.smartgeek.testcamerapreview

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DisplayImageActivity : AppCompatActivity() {

    private lateinit var imageUri: Uri
    private lateinit var displayImage: ImageView
    private lateinit var framesRV: RecyclerView
    private lateinit var btnSave: Button
    private var imageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        displayImage = findViewById(R.id.iv_display)
        btnSave = findViewById(R.id.btn_save)

        // Get the image URI from the Intent
        val imageUriString = intent.getStringExtra("MyImage")
        imageUri = Uri.parse(imageUriString)

        // Set the image in the ImageView
        displayImage.setImageURI(imageUri)

        framesRV = findViewById(R.id.rcv_frame)
        val framesModel: ArrayList<ImageDetails> = ArrayList()

        framesModel.add(ImageDetails("Cat Eyes Frame 1", R.drawable.fspecs_2))
        framesModel.add(ImageDetails("Office Frame", R.drawable.fspecs_3))
        framesModel.add(ImageDetails("Round Frame", R.drawable.fspecs_4))
        framesModel.add(ImageDetails("Cat Eyes Frame 2", R.drawable.fspecs_7))
        framesModel.add(ImageDetails("Aviator Frame", R.drawable.fspecs_8))

        // Setting up recyclerView and setOnClickListener to display frame on user's face
        val adapter = ImageVAdapter(framesModel) { imageDetails ->
            Toast.makeText(this, imageDetails.imgName, Toast.LENGTH_SHORT).show()
            Glide.with(this)
                .load(imageDetails.frameid)
                .placeholder(R.drawable.fspecs_2)
                .error(R.drawable.fspecs_2)
                .into(findViewById(R.id.frameOnFace))
        }
        framesRV.adapter = adapter

        btnSave.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveBitmapToMediaStoreQ(getBitmapFromView(findViewById(R.id.cl_display)))
            } else {
                folderCheck()
            }
        }
    }

    private fun folderCheck() {
        val folder =
            File(
                Environment.getExternalStorageDirectory().toString() + "/SMARTGGK_Frame_screenshots"
            )
        var success = true
        // If the folder GKB_Frame_screenshots not exist, create one
        if (!folder.exists()) {
            success = folder.mkdir()
        } else {
            screenShot()
        }
        // If mkdir successful
        if (success) {
            screenShot()
        } else {
            Log.e("mkdir_fail", "Failed to write")
        }
    }

    private fun screenShot() {
        val filePath =
            Environment.getExternalStorageDirectory()
                .toString() + "/SMARTGGK_Frame_screenshots/temp.png"

        // create bitmap screen capture
        val bitmap = getBitmapFromView(findViewById(R.id.cl_display))

        val fout: OutputStream?
        val imageFile = File(filePath)
        try {
            fout = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout)
            fout.flush()
            fout.close()
            Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    // Save the Bitmap to the device's public pictures directory (Android 10 and later)
    private fun saveBitmapToMediaStoreQ(bitmap: Bitmap) {
        imageName = getCurrentTimestamp()
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "screenshot_${imageName}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { it ->
            resolver.openOutputStream(it).use { outputStream ->
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    Toast.makeText(this, "Screenshot saved to MediaStore", Toast.LENGTH_SHORT)
                        .show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        shareMediaStoreImageQ()
                    } else {
                        shareExternalStorageImage()
                    }
                }
            }
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    // Share function starts here
    private fun shareMediaStoreImageQ() {
        val resolver = contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("screenshot_${imageName}.png")

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                shareImage(contentUri)
            }
        }
    }

    private fun shareExternalStorageImage() {
        val imagePath = Environment.getExternalStorageDirectory().toString() +
                "/GKB_Frame_screenshots/temp.png"
        val imageFile = File(imagePath)
        shareImage(Uri.fromFile(imageFile))
    }

    private fun shareImage(imageUri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

}
