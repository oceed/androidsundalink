package com.sundalink.mapstracker

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PanduanActivity : AppCompatActivity() {
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panduan)

        val btmNavChat = findViewById<ConstraintLayout>(R.id.btmnavchat)
        val btmNavProf = findViewById<ConstraintLayout>(R.id.profilenavbar)
        val btmNavJadwal = findViewById<ConstraintLayout>(R.id.btmnavjadwal)
        val btmnavhome = findViewById<ConstraintLayout>(R.id.btmnavhome)
        val doaumrohbtnn = findViewById<ConstraintLayout>(R.id.doaumrohbtn)

        doaumrohbtnn.setOnClickListener {
            val intent = Intent(this, doaumroh::class.java)
            startActivity(intent)
        }

        btmNavChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            finish()
        }

        btmNavProf.setOnClickListener {
            val intent = Intent(this, ProfiveActivity::class.java)
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