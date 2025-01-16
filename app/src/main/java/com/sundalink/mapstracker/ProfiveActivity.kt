package com.sundalink.mapstracker

import android.content.Intent
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profive)
        window.statusBarColor = resources.getColor(R.color.myprimary, theme)
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
                .load("http://93.127.162.185:4000/api/v1/files/$userPhoto") // Load gambar dari URL
                .circleCrop() // Membuat gambar berbentuk lingkaran
                .into(imageView)
        }

        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
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
}