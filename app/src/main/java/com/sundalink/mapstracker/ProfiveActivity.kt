package com.sundalink.mapstracker

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class ProfiveActivity : AppCompatActivity() {
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profive)
        window.statusBarColor = resources.getColor(R.color.myprimary, theme)

        val btmNavChat = findViewById<ConstraintLayout>(R.id.btmnavchat)
        val btmNavProf = findViewById<ConstraintLayout>(R.id.profilenavbar)
        val btmNavJadwal = findViewById<ConstraintLayout>(R.id.btmnavjadwal)
        val btmnavhome = findViewById<ConstraintLayout>(R.id.btmnavhome)

        btmNavChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            finish()
        }

        btmnavhome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        btmNavJadwal.setOnClickListener {
            val intent = Intent(this, JadwalActivity::class.java)
            startActivity(intent)
            finish()
        }

        val emergencyButton = findViewById<ConstraintLayout>(R.id.sosbtn)
        sharedPreferencess = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        isEmergency = sharedPreferencess.getBoolean("isEmergency", false)
        updateButtonText(emergencyButton)
        emergencyButton.setOnClickListener {
            toggleEmergency(emergencyButton)
        }

        val logoutButton = findViewById<LinearLayout>(R.id.btnlogoutprofile)
        val username: TextView = findViewById(R.id.usernameprofile)
        val email: TextView = findViewById(R.id.emailprofile)

        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        username.text = sharedPreferences.getString("name", "Unknown")
        email.text = sharedPreferences.getString("email", "Unknown")
        val userPhoto = sharedPreferences.getString("avatarid", "Unknown")
        if (userPhoto != null) {
            val imageView = findViewById<ImageView>(R.id.profileimage)
            Glide.with(this)
                .load("https://api.mabrur.info/api/v1/files/$userPhoto") // Load gambar dari URL
                .circleCrop() // Membuat gambar berbentuk lingkaran
                .into(imageView)
        }

        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val sharedPreferencess = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear() // Menghapus semua data di SharedPreferences
            apply()
        }

        // Navigasi kembali ke halaman login
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
}