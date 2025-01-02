package com.sundalink.mapstracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val username: TextView = findViewById(R.id.username)
        username.text = sharedPreferences.getString("name", "Unknown")

        val usertype: TextView = findViewById(R.id.usertype)
        usertype.text = sharedPreferences.getString("user_type", "Unknown")

        val logoutButton: Button = findViewById(R.id.btnlogout)
        logoutButton.setOnClickListener {
            logout()
        }

        val mapsButton = findViewById<ConstraintLayout>(R.id.mapsbutton)

        mapsButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
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