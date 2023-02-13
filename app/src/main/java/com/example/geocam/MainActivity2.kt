package com.example.geocam

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.geocam.databinding.ActivityMain2Binding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity2 : AppCompatActivity() {
    private var _binding: ActivityMain2Binding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val CAMERA_REQUEST_CODE = 100
    private val REQUEST_LOCATION_PERMISSION = 10
    private var imageUri: Uri? = null

    private var currentPhotoPath = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.btnCapturePicture.setOnClickListener {
            openCamera()
        }

    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    ex.printStackTrace()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    imageUri = FileProvider.getUriForFile(
                        this,
                        "com.example.geocam.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Get current location
            val location = getCurrentLocation()
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.get(0)?.getAddressLine(0)
                // Add location information as watermark on the image
                addWatermark(imageUri, "$latitude, $longitude,  $address")
            }else {
                Toast.makeText(this, "lokasi tidak ada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addWatermark(imageUri: Uri?, address: String) {
        Log.d(TAG, "addWatermark: $address")
        try {
            val matrix = Matrix()
            matrix.postRotate(90f)
//            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath, BitmapFactory.Options().apply { inMutable = true })

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val stream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val compressedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, (rotatedBitmap.width * 0.3).toInt(), (rotatedBitmap.height * 0.3).toInt(), true)

            val photoWidth = compressedBitmap.width
            val photoHeight = compressedBitmap.height

            if (address != null) {
                Log.d(TAG, "addWatermark: ${address.length}")
            }
            val canvas = Canvas(compressedBitmap)
            val paintText = TextPaint().apply {
                color = Color.BLACK
                textSize = 40f
            }
            val bgPaint = Paint().apply {
                color = Color.parseColor("#D2FFFFFF")
                style = Paint.Style.FILL
            }
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()

            val yPos = photoHeight - 20
            canvas.drawRect(0f, yPos - 200f, photoWidth.toFloat(), yPos + paintText.descent() - paintText.ascent(), bgPaint)


            // Menambahkan alamat pada watermark
            val staticLayout = StaticLayout.Builder
                .obtain(address.toString(), 0, address!!.length, paintText, photoWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

            canvas.save()
            canvas.translate(0f, photoHeight - staticLayout.height - 20f)
            staticLayout.draw(canvas)
            canvas.restore()


            val fileOutputStream = FileOutputStream(currentPhotoPath)
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.close()
            setImage(compressedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setImage(bitmap: Bitmap?) {
        binding.imgCaptured.setImageBitmap(bitmap)
    }

    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request for location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            return null
        }
        val provider = locationManager.getBestProvider(Criteria(), false)
        return provider?.let { locationManager.getLastKnownLocation(it) }
    }
    
    companion object {
        private const val TAG = "MainActivity2"
    }
}