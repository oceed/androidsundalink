package com.sundalink.mapstracker

import android.content.Intent
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide

class HomeActivity : AppCompatActivity() {
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        window.statusBarColor = resources.getColor(R.color.myprimary, theme)

        // Cek izin lokasi
        if (checkLocationPermission()) {
            startLocationService()
        } else {
            requestLocationPermission()
        }

        // Inisialisasi UI
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val username: TextView = findViewById(R.id.username)
        val usertype: TextView = findViewById(R.id.usertype)
        val mapsButton = findViewById<ConstraintLayout>(R.id.mapsbutton)
        val chatButton = findViewById<ConstraintLayout>(R.id.chatbutton)
        val jadwalButton = findViewById<ConstraintLayout>(R.id.jadwalbtn)
        val btmNavChat = findViewById<ConstraintLayout>(R.id.btmnavchat)
        val btmNavProf = findViewById<ConstraintLayout>(R.id.profilenavbar)

        val emergencyButton = findViewById<ConstraintLayout>(R.id.sosbtn)
        sharedPreferencess = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        isEmergency = sharedPreferencess.getBoolean("isEmergency", false)
        updateButtonText(emergencyButton)
        emergencyButton.setOnClickListener {
            toggleEmergency(emergencyButton)
        }

        username.text = sharedPreferences.getString("name", "Unknown")
        usertype.text = sharedPreferences.getString("user_type", "Unknown")
        val userPhoto = sharedPreferences.getString("avatarid", "Unknown")
        if (userPhoto != null) {
            val imageView = findViewById<ImageView>(R.id.ivUserPhoto)
            Glide.with(this)
                .load("http://93.127.162.185:4000/api/v1/files/$userPhoto") // Load gambar dari URL
                .circleCrop() // Membuat gambar berbentuk lingkaran
                .into(imageView)
        }

        chatButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        btmNavChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        btmNavProf.setOnClickListener {
            val intent = Intent(this, ProfiveActivity::class.java)
            startActivity(intent)
        }

        mapsButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        jadwalButton.setOnClickListener {
            val intent = Intent(this, JadwalActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationMqttService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService()
            } else {
                Toast.makeText(
                    this,
                    "Izin lokasi diperlukan untuk menjalankan layanan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun toggleEmergency(textview: ConstraintLayout) {
        isEmergency = !isEmergency
        sharedPreferencess.edit().putBoolean("isEmergency", isEmergency).apply()
        updateButtonText(textview)
    }

    private fun updateButtonText(textview: ConstraintLayout) {
        if (isEmergency) {
            textview.background = ColorDrawable(Color.parseColor("#DC3F34"))
        } else {
            textview.background = ColorDrawable(Color.parseColor("#42BF4B"))
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}