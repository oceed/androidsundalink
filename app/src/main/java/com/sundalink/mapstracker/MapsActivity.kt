package com.sundalink.mapstracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.JsonObject
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.viewannotation.geometry
import info.mqtt.android.service.Ack
import info.mqtt.android.service.MqttAndroidClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import kotlin.math.max

class MapsActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mqttClient: MqttAndroidClient
    private var annotationManager: PointAnnotationManager? = null
    private val deviceMarkers = mutableMapOf<String, PointAnnotation>()
    private lateinit var deviceId: String
    private var isCameraFocused = false
    private var currentBubbleView: View? = null
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        window.statusBarColor = resources.getColor(R.color.myprimary, theme)
        mapView = findViewById(R.id.mapView)
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val btmNavChat = findViewById<ConstraintLayout>(R.id.btmnavchat)
        val btmNavProf = findViewById<ConstraintLayout>(R.id.profilenavbar)
        val btmNavJadwal = findViewById<ConstraintLayout>(R.id.btmnavjadwal)
        val btmnavhome = findViewById<ConstraintLayout>(R.id.btmnavhome)

        btmNavChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        btmNavProf.setOnClickListener {
            val intent = Intent(this, ProfiveActivity::class.java)
            startActivity(intent)
        }

        btmnavhome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        btmNavJadwal.setOnClickListener {
            val intent = Intent(this, JadwalActivity::class.java)
            startActivity(intent)
        }

        mapView.getMapboxMap().addOnStyleLoadedListener {
            runOnUiThread {
                annotationManager = mapView.annotations.createPointAnnotationManager()
                annotationManager?.addClickListener { annotation ->
                    showInfoBubble(annotation)
                    true
                }
            }
        }

        findViewById<ImageButton>(R.id.sendBack).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        setupMQTT()
        val emergencyButton = findViewById<ConstraintLayout>(R.id.sosbtn)
        sharedPreferencess = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        isEmergency = sharedPreferencess.getBoolean("isEmergency", false)
        updateButtonText(emergencyButton)
        emergencyButton.setOnClickListener {
            toggleEmergency(emergencyButton)
        }
    }

    private fun addOrUpdateMarker(userid: String, name: String, age: String, phone: String, gender: String, emergency: Boolean, device: String, latitude: Double, longitude: Double, title: String, avatarUrl: String, borderColor: Int, bubbleData: JsonObject, borderWidth: Float = 70f) {
        if (annotationManager == null) return

        val point = com.mapbox.geojson.Point.fromLngLat(longitude, latitude)
        val marker = deviceMarkers[device]

        if (marker != null) {
            if (marker.point != point) {
                marker.point = point
                annotationManager?.update(marker)
                Log.d("marker", "marker diupdate")
            }

            // Jika status berubah, ganti ikon
            val currentStatus = marker.getData()?.asJsonObject?.get("emergency")?.asBoolean
            if (currentStatus != emergency) {
                annotationManager?.delete(marker)
                deviceMarkers.remove(device)

                createNewMarker(userid, name, age, phone, gender, emergency, device, latitude, longitude, title, avatarUrl, borderColor, bubbleData, borderWidth)
                Log.d("marker", "marker dibuat karena emergency: $currentStatus")
            }
        } else {
            createNewMarker(userid, name, age, phone, gender, emergency, device, latitude, longitude, title, avatarUrl, borderColor, bubbleData, borderWidth)
            // Muat avatar sebagai ikon marker menggunakan Glide
            Log.d("marker", "marker dibuat karena tidak ada marker sebelumnya")
        }
    }

    private fun createNewMarker(userid: String, name: String, age: String, phone: String, gender: String, emergency: Boolean, device: String, latitude: Double, longitude: Double, title: String, avatarUrl: String, borderColor: Int, bubbleData: JsonObject, borderWidth: Float = 70f) {
        val point = com.mapbox.geojson.Point.fromLngLat(longitude, latitude)
        if (isDestroyed || isFinishing) {
            return
        }
        Glide.with(this)
            .asBitmap()
            .load(avatarUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop() // Membuat gambar menjadi lingkaran
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Tambahkan border ke gambar bulat
                    val bitmapWithBorder = addCircularBorderToBitmap(resource, borderColor, borderWidth)

                    // Skalakan bitmap ke ukuran yang diinginkan
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmapWithBorder, 100, 100, true)

                    runOnUiThread {
                        val pointAnnotationOptions = PointAnnotationOptions()
                            .withPoint(point)
                            .withIconImage(scaledBitmap)
                            .withData(bubbleData)

                        val newMarker = annotationManager!!.create(pointAnnotationOptions)
                        deviceMarkers[device] = newMarker
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Tidak ada yang perlu dilakukan jika gambar dibatalkan
                }
            })
    }

    private fun setupMQTT() {
        val serverUri = "tcp://93.127.162.185:1883"
        val clientId = "AndroidClient-${deviceId}"
        val username = "sundalink"
        val passwordd = "@Sundalink123"
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = false
            userName = username
            password = passwordd.toCharArray()
        }

        mqttClient = MqttAndroidClient(applicationContext, serverUri, clientId, Ack.AUTO_ACK)
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e("MQTT", "Connection Lost: ${cause?.message}")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("MQTT", "Message arrived on topic: $topic, message: ${message.toString()}")
                if (message != null) {
                    val payload = String(message.payload)
                    CoroutineScope(Dispatchers.IO).launch {
                        handleMQTTMessage(payload)
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "Delivery complete for token: $token")
            }
        })

        if (!mqttClient.isConnected) {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected successfully")
                    mqttClient.subscribe("sundalink/sw", 1)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Connection failed: ${exception?.message}")
                }
            })
        } else {
            Log.d("MQTT", "Already connected, skipping reconnect.")
        }
    }

    private fun handleMQTTMessage(payload: String) {
        try {
            val data = JSONObject(payload)
            if (data.has("latitude") && data.has("longitude")) {
                val latitude = data.getDouble("latitude")
                val longitude = data.getDouble("longitude")
                val device = data.optString("device", "Unknown Device")
                val heartrate = data.optInt("heart_rate", -1)
                val emergency = data.optBoolean("emergency", false)
                val age = data.optString("age", "unknown")
                val avatar = data.optString("avatar", "464fa819-21b6-44ad-bd45-bf4b39f64b62")
                val name = data.optString("name", "unknown")
                val gender = data.optString("gender", "unknown")
                val phone = data.optString("phone", "unknown")
                val userid = data.optString("userid", "unknown")
                val avatarurl = "http://93.127.162.185:4000/api/v1/files/$avatar"
                var borderColor = 0
                if (emergency) {
                    borderColor = Color.parseColor("#DC3F34")
                } else {
                    borderColor = ContextCompat.getColor(this, R.color.myprimary)
                }

                val markerTitle: String
                val bubbleData: JsonObject

                if (heartrate != -1) { // Pesan dari smartwatch
                    markerTitle = "Device: $device\nHeart Rate: $heartrate\nEmergency: $emergency"
                    bubbleData = JsonObject().apply {
                        addProperty("device", device)
                        addProperty("heartrate", heartrate)
                        addProperty("emergency", emergency)
                    }
                } else { // Pesan dari Android
                    markerTitle = "Device: $device\nEmergency: $emergency"
                    bubbleData = JsonObject().apply {
                        addProperty("name", name)
                        addProperty("age", age)
                        addProperty("phone", phone)
                        addProperty("gender", gender)
                        addProperty("device", device)
                        addProperty("userid", userid)
                        addProperty("emergency", emergency)
                    }
                }

                runOnUiThread {
                    addOrUpdateMarker(userid, name, age, phone, gender, emergency, device, latitude, longitude, markerTitle, avatarurl, borderColor, bubbleData)

                    if (device == deviceId && !isCameraFocused) {
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(com.mapbox.geojson.Point.fromLngLat(longitude, latitude))
                                .zoom(14.0)
                                .build()
                        )
                        isCameraFocused = true
                    }
                }
            } else {
                Log.e("MQTT", "Invalid payload: missing latitude or longitude")
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error handling MQTT message: ${e.message}")
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

    /**
     * Fungsi untuk menambahkan border lingkaran pada bitmap
     */
    private fun addCircularBorderToBitmap(bitmap: Bitmap, borderColor: Int, borderWidth: Float): Bitmap {
        val size = max(bitmap.width, bitmap.height) + (borderWidth * 2).toInt()

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Hitung posisi lingkaran
        val radius = size / 2f
        val center = size / 2f

        // Gambar border (lingkaran luar)
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = borderColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, borderPaint)

        // Gambar bitmap di tengah lingkaran
        val imagePaint = Paint().apply {
            isAntiAlias = true
        }
        val imageRadius = radius - borderWidth
        val rect = RectF(borderWidth, borderWidth, size - borderWidth, size - borderWidth)
        canvas.drawBitmap(bitmap, null, rect, imagePaint)

        return output
    }

    private fun showInfoBubble(annotation: PointAnnotation) {
        val markerPosition = annotation.geometry as com.mapbox.geojson.Point

        // Hapus bubble yang sudah ditampilkan sebelumnya
        currentBubbleView?.let { mapView.removeView(it) }

        val data = annotation.getData()?.asJsonObject ?: return

        // Tentukan apakah berasal dari smartwatch (jika ada "heartrate")
        val isSmartwatch = data.has("heartrate")

        val bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_tooltip, null)
        currentBubbleView = bubbleView // Simpan referensi bubble yang ditampilkan

        if (isSmartwatch) {
            // Bubble untuk smartwatch: hanya menampilkan deviceId dan heartrate
            val deviceId = data.get("device")?.asString ?: "Unknown Device"
            val heartrate = data.get("heartrate")?.asInt ?: -1

            val titleView: TextView = bubbleView.findViewById(R.id.markerTitle)
            val ageView: TextView = bubbleView.findViewById(R.id.agemarker)
            val phoneView: TextView = bubbleView.findViewById(R.id.phonemarker)
            val labeldevice: TextView = bubbleView.findViewById(R.id.smartphonetext)

            titleView.text = "Zein (Jamaah)"
            labeldevice.text = "SMARTWATCH"
            ageView.text = "Device ID: $deviceId"
            phoneView.text = "Heart Rate: $heartrate"

            // Sembunyikan elemen yang tidak relevan
            bubbleView.findViewById<TextView>(R.id.gendermarker).visibility = View.GONE
        } else {
            // Bubble untuk perangkat Android: tampilkan data lengkap
            val name = data.get("name")?.asString ?: "Unknown"
            val age = data.get("age")?.asString ?: "Unknown"
            val phone = data.get("phone")?.asString ?: "Unknown"
            val gender = data.get("gender")?.asString ?: "Unknown"

            val titleView: TextView = bubbleView.findViewById(R.id.markerTitle)
            val ageView: TextView = bubbleView.findViewById(R.id.agemarker)
            val phoneView: TextView = bubbleView.findViewById(R.id.phonemarker)
            val genderView: TextView = bubbleView.findViewById(R.id.gendermarker)

            titleView.text = name
            ageView.text = "Umur: $age"
            phoneView.text = "Telpon: $phone"
            genderView.text = "Gender: $gender"
        }

        mapView.addView(bubbleView)

        // Fungsi untuk memperbarui posisi bubble
        fun updateBubblePosition() {
            bubbleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val screenPosition = mapView.getMapboxMap().pixelForCoordinate(markerPosition)

            val bubbleWidth = bubbleView.measuredWidth
            val bubbleHeight = bubbleView.measuredHeight

            val screenWidth = mapView.width
            val screenHeight = mapView.height

            // Cek apakah marker masih di dalam layar
            if (screenPosition.x < 0 || screenPosition.x > screenWidth || screenPosition.y < 0 || screenPosition.y > screenHeight) {
                // Hapus bubble jika marker keluar dari layar
                currentBubbleView?.let {
                    mapView.removeView(it)
                    currentBubbleView = null
                }
                return
            }

            // Jika marker masih di dalam layar, perbarui posisi bubble
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = (screenPosition.x - bubbleWidth / 2).toInt()
                topMargin = (screenPosition.y - bubbleHeight).toInt()
            }
            bubbleView.layoutParams = params
        }

        // Perbarui posisi bubble setiap kali kamera peta bergerak
        mapView.getMapboxMap().addOnCameraChangeListener {
            updateBubblePosition()
        }

        // Perbarui posisi pertama kali setelah bubble ditambahkan
        updateBubblePosition()

        // Tambahkan listener untuk menutup bubble saat peta diklik
        mapView.getMapboxMap().addOnMapClickListener {
            currentBubbleView?.let { view ->
                mapView.removeView(view)
                currentBubbleView = null
            }
            true
        }

        // Tambahkan listener untuk menutup bubble ketika bubble itu sendiri di klik
        bubbleView.setOnClickListener {
            mapView.removeView(bubbleView)
            currentBubbleView = null
        }
    }

}
