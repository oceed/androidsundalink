package com.sundalink.mapstracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class splashactivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashactivity)
        window.statusBarColor = resources.getColor(R.color.myprimary, theme)

        // Referensi ke layout utama (contoh: root layout)
        val splashScreen = findViewById<LinearLayout>(R.id.linearLayout5)

        // Animasi fade in
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 1500 // Durasi animasi dalam milidetik
            fillAfter = true
        }

        // Animasi fade out
        val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 1500
            startOffset = 2000 // Tunggu selama 2 detik sebelum fade out
            fillAfter = true
        }

        // Jalankan fade in
        splashScreen.startAnimation(fadeIn)

        // Lanjutkan dengan fade out dan pindah ke MainActivity
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Pindah ke MainActivity setelah animasi selesai
                val intent = Intent(this@splashactivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Mulai fade out setelah fade in selesai
        Handler(Looper.getMainLooper()).postDelayed({
            splashScreen.startAnimation(fadeOut)
        }, 1500)
    }
}