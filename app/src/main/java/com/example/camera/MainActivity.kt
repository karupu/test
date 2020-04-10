package com.example.camera

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAKE_IMAGE_REQUEST_CODE = 1
        val REQUEST_IMAGE_CAPTURE = 2
        private val PERMISSION_REQUEST_CODE: Int = 101
        var imagePath: String? = null
        var imageFile: File? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        cameraButton.setOnClickListener {
            if (checkPersmission()) openCamera()
            else requestPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAKE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d("STATUSCHECK", "MAINACTIVITY: onActivityResult() uri: $imagePath")
            if (data != null) {
                decryptAndDisplayImage(data)
            }
        }
    }

    fun decryptAndDisplayImage(data: Intent) {
        Log.d("STATUSCHECK", "inside decryptData()")

        val key = data.getExtras()?.getString("key")
        val inputStream = FileInputStream(imagePath)
        try {
            val iv = ByteArray(16)
            val mac = ByteArray(32)
            inputStream.read(iv)
            inputStream.read(mac)
            val cipherSize = (imageFile!!.length() - 16 - 32).toInt()
            val cipherText = ByteArray(cipherSize)
            inputStream.read(cipherText);

            val keys : AesCbcWithIntegrity.SecretKeys = AesCbcWithIntegrity . keys (key!!)
            val cipherTextIvMac : AesCbcWithIntegrity.CipherTextIvMac = AesCbcWithIntegrity.CipherTextIvMac(
                cipherText,
                iv,
                mac
            )

            val decrypted = AesCbcWithIntegrity.decrypt (cipherTextIvMac, keys)
            val picture = BitmapFactory.decodeByteArray (decrypted, 0, decrypted.size)
            ivImage.setImageBitmap(picture)

            saveDecryptedImagePath(decrypted)

        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        }
    }

    fun saveDecryptedImagePath(decryptedImage: ByteArray) {

    }

    fun openCamera() {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            imageFile = createTempFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (imageFile != null) {
            val imageUri = FileProvider.getUriForFile(
                applicationContext,
                "com.example.camera.fileprovider",
                imageFile!!
            )
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            Log.d("STATUSCHECK", "MAINACTIVITY: uri before startActivityForResult(): $imageUri")
            startActivityForResult(captureIntent, TAKE_IMAGE_REQUEST_CODE)
        }
    }

    @Throws(IOException::class)
    fun createTempFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyymmdd_hhmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            imagePath = absolutePath
        }
    }

    private fun checkPersmission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(READ_EXTERNAL_STORAGE, CAMERA),
            TAKE_IMAGE_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
            }
        }
    }
}
