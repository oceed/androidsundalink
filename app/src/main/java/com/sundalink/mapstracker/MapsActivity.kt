package com.sundalink.mapstracker

import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.JsonObject
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
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
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import info.mqtt.android.service.Ack
import info.mqtt.android.service.MqttAndroidClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Request
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max

class MapsActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mqttClient: MqttAndroidClient
    private var annotationManager: PointAnnotationManager? = null
    private val deviceMarkers = mutableMapOf<String, PointAnnotation>()
    private val pendingMarkers: MutableSet<String> = mutableSetOf()
    private lateinit var deviceId: String
    private var isCameraFocused = false
    private var currentBubbleView: View? = null
    private val routeLineApi: MapboxRouteLineApi by lazy {
        val customColorResources = RouteLineColorResources.Builder()
            .routeDefaultColor(Color.parseColor("#C88219"))  // Biru muda
            .routeLowCongestionColor(Color.parseColor("#C88219"))  // Hijau
            .routeModerateCongestionColor(Color.parseColor("#C88219"))  // Kuning
            .routeHeavyCongestionColor(Color.parseColor("#C88219"))  // Oranye
            .routeSevereCongestionColor(Color.parseColor("#C88219"))  // Merah
            .routeUnknownCongestionColor(Color.parseColor("#05A41C"))  // Abu-abu
            .restrictedRoadColor(Color.parseColor("#C88219"))  // Merah tua
            .routeClosureColor(Color.parseColor("#C88219"))  // Hitam
            .alternativeRouteDefaultColor(Color.parseColor("#C88219"))  // Abu-abu muda
            .routeLineTraveledCasingColor(Color.parseColor("#C88219"))
            .build()

        MapboxRouteLineApi(MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(RouteLineResources.Builder()
                .routeLineColorResources(customColorResources)
                .build())
            .build())
    }

    private val routeLineView: MapboxRouteLineView by lazy {
        MapboxRouteLineView(MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .build())
    }

    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.routes.isNotEmpty()) {
            val routeLines = routeUpdateResult.routes.map { route ->
                RouteLine(route, null)
            }
            routeLineApi.setRoutes(routeLines) { value ->
                mapView.getMapboxMap().getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            val currentRoute = mapboxNavigation.getNavigationRoutes().firstOrNull()?.directionsRoute
            currentRoute?.let {
                updateTripProgressView(it, routeProgress)
            }
        }
    }


    private val tripProgressFormatter: TripProgressUpdateFormatter by lazy {
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(this).build()

        TripProgressUpdateFormatter.Builder(this)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(this))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(this))
            .build()
    }

    private val tripProgressApi: MapboxTripProgressApi by lazy {
        MapboxTripProgressApi(tripProgressFormatter)
    }

    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var closeButton: Button
    private lateinit var tripProgressView: MapboxTripProgressView
    private lateinit var navinfo: CardView
    private lateinit var parentnavinfo: LinearLayout
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false
    lateinit var mapboxNavigation: MapboxNavigation
    private val lastStateMap = mutableMapOf<String, Boolean>() // Untuk melacak state emergency terakhir
    private val lastUpdateTimeMap = mutableMapOf<String, Long>() // Untuk mencegah pembaruan terlalu cepat
    private val DEBOUNCE_INTERVAL = 1000L // 1 detik untuk membatasi pembaruan marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

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
        tripProgressView = findViewById(R.id.navigationView)
        parentnavinfo = findViewById(R.id.parentnavinfo)
        navinfo = findViewById(R.id.navinfo)
        closeButton = findViewById(R.id.closeButton)
        tripProgressView.visibility = View.GONE
        parentnavinfo.visibility = View.GONE
        navinfo.visibility = View.GONE

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

        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        )

        mapboxNavigation.registerRoutesObserver(routesObserver)


        setupMQTT()

        val emergencyButton = findViewById<ConstraintLayout>(R.id.sosbtn)
        sharedPreferencess = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        isEmergency = sharedPreferencess.getBoolean("isEmergency", false)
        updateButtonText(emergencyButton)
        emergencyButton.setOnClickListener {
            toggleEmergency(emergencyButton)
        }

        closeButton.setOnClickListener {
            mapboxNavigation.setNavigationRoutes(emptyList()) // Hapus rute
            mapboxNavigation.stopTripSession()
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            routeLineApi.clearRouteLine { value ->
                mapView.getMapboxMap().getStyle()?.let { style ->
                    routeLineView.renderClearRouteLineValue(style, value)
                }
            }

            hideTripViewAnimated() // Sembunyikan TripView dengan animasi
        }
    }

    private fun addOrUpdateMarker(userid: String, name: String, age: String, phone: String, gender: String, emergency: Boolean, device: String, latitude: Double, longitude: Double, title: String, avatarUrl: String, borderColor: Int, bubbleData: JsonObject, borderWidth: Float = 70f) {
        if (annotationManager == null) return

        val point = com.mapbox.geojson.Point.fromLngLat(longitude, latitude)
        val marker = deviceMarkers[device]

        if (marker != null) {
            val markerData = marker.getData()?.asJsonObject
            val isSmartwatch = bubbleData.has("heartrate")

            if (marker.point != point) {
                marker.point = point
                annotationManager?.update(marker)
                Log.d("marker", "marker diupdate")
            }

            // Jika status berubah, ganti ikon
            val currentStatus = marker.getData()?.asJsonObject?.get("emergency")?.asBoolean
            if (currentStatus != emergency) {
                annotationManager?.delete(marker)
//                deviceMarkers.remove(device)

                createNewMarker(userid, name, age, phone, gender, emergency, device, latitude, longitude, title, avatarUrl, borderColor, bubbleData, borderWidth)
                Log.d("marker", "marker dibuat karena emergency: $currentStatus")
                Log.d("MQTT Debug", "currentStatus: $currentStatus, emergency: $emergency, device: $device")
            }
            if (isSmartwatch) {
                val currentHeartrate = markerData?.get("heartrate")?.asInt
                val newHeartrate = bubbleData.get("heartrate").asInt
                if (currentHeartrate != newHeartrate) {
                    markerData?.addProperty("heartrate", newHeartrate) // Perbarui data heartrate
                    marker.setData(markerData)
                    annotationManager?.update(marker)
                    Log.d("marker", "Heartrate diupdate untuk device $device: $newHeartrate")
                }
            }
        } else {
            if (pendingMarkers.contains(device)) {
                Log.d("marker", "Marker sedang dalam proses pembuatan untuk device: $device")
                return
            }

            // Tandai bahwa marker sedang dibuat
            pendingMarkers.add(device)
            createNewMarker(userid, name, age, phone, gender, emergency, device, latitude, longitude, title, avatarUrl, borderColor, bubbleData, borderWidth)
            // Muat avatar sebagai ikon marker menggunakan Glide
            Log.d("marker", "marker dibuat karena tidak ada marker sebelumnya $marker")
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
                        pendingMarkers.remove(device)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    pendingMarkers.remove(device)
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
                    mqttClient.subscribe("sundalink/sw", 0)
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
                val avatar = data.optString("avatar", "")
                val name = data.optString("name", "unknown")
                val gender = data.optString("gender", "unknown")
                val battery = data.optString("battery", "unknown")
                val phone = data.optString("phone", "unknown")
                val userid = data.optString("userid", "unknown")
                val avatarurl = if (avatar.isNotEmpty()) {
                    "http://93.127.162.185:4000/api/v1/files/$avatar"
                } else {
                    "https://api.mabrur.info/api/v1/files/dummy/smartwatch"
                }
                var borderColor = 0

                // Periksa debounce interval
                val currentTime = System.currentTimeMillis()
                val lastUpdateTime = lastUpdateTimeMap[device] ?: 0
                if (currentTime - lastUpdateTime < DEBOUNCE_INTERVAL) {
                    Log.d("MQTT Debug", "Skipping marker update for device: $device due to debounce")
                    return
                }

                lastUpdateTimeMap[device] = currentTime

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
                        addProperty("battery", battery)
                        addProperty("latitude", latitude)
                        addProperty("longitude", longitude)
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
                        addProperty("battery", battery)
                        addProperty("latitude", latitude)
                        addProperty("longitude", longitude)
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


    private fun showInfoBubble(annotation: PointAnnotation) {
        val markerPosition = annotation.geometry as com.mapbox.geojson.Point

        // Hapus bubble sebelumnya (jika ada)
        currentBubbleView?.let { view ->
            if (view.parent != null) mapView.removeView(view)
            currentBubbleView = null
        }

        val data = annotation.getData()?.asJsonObject ?: return
        val isSmartwatch = data.has("heartrate")

        // Inflate layout bubble
        val bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_tooltip, null)
        currentBubbleView = bubbleView
        bubbleView.isClickable = true
        bubbleView.isFocusable = true

        // Tambahkan bubble ke MapView sebelum melakukan update
        mapView.addView(bubbleView)
        bubbleView.bringToFront()

        // Ambil referensi view dalam bubble
        val relativeLayout: RelativeLayout = bubbleView.findViewById(R.id.relativebuble)
        val smartphoneTextView: TextView = bubbleView.findViewById(R.id.smartphonetext)
        val imageView: ImageView = bubbleView.findViewById(R.id.imageLocation)

        // Fungsi untuk memperbarui posisi bubble
        fun updateBubblePosition() {
            bubbleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val screenPosition = mapView.getMapboxMap().pixelForCoordinate(markerPosition)

            val bubbleWidth = bubbleView.measuredWidth
            val bubbleHeight = bubbleView.measuredHeight

            if (screenPosition.run { x < 0 || x > mapView.width || y < 0 || y > mapView.height }) {
                Log.d("Bubble", "Bubble keluar layar, menyembunyikan...")

                if (currentBubbleView?.visibility != View.GONE) {
                    currentBubbleView?.visibility = View.GONE
                    Log.w("Bubble", "Bubble disembunyikan.")
                }
                return
            } else {
                if (currentBubbleView?.visibility == View.GONE) {
                    currentBubbleView?.visibility = View.VISIBLE
                    Log.w("Bubble", "Bubble kembali ke layar, menampilkan...")
                }
            }
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

        // Handler untuk update real-time
        val updateHandler = Handler(Looper.getMainLooper())
        val updateInterval = 1000L

        val updateRunnable = object : Runnable {
            override fun run() {
                val updatedData = annotation.getData()?.asJsonObject
                if (updatedData != null) {
                    if (isSmartwatch) {
                        val heartrate = updatedData.get("heartrate")?.asInt ?: -1
                        val phoneView: TextView = bubbleView.findViewById(R.id.phonemarker)
                        phoneView.text = "Heart Rate: $heartrate"
                    }

                    val updatedPosition = annotation.geometry as com.mapbox.geojson.Point
                    if (markerPosition != updatedPosition) {
                        updateBubblePosition()
                    }
                }
                updateHandler.postDelayed(this, updateInterval)
            }
        }

        // Jalankan pembaruan pertama kali
        updateHandler.post(updateRunnable)

        // Update posisi bubble saat kamera bergerak
        mapView.getMapboxMap().addOnCameraChangeListener {
            updateBubblePosition()
        }

        // Perbarui posisi awal
        updateBubblePosition()

        // Listener untuk menutup bubble saat peta diklik
        mapView.getMapboxMap().addOnMapClickListener { point ->
            currentBubbleView?.let { view ->
                val screenPosition = mapView.getMapboxMap().pixelForCoordinate(point)
                val bubbleRect = Rect()
                view.getGlobalVisibleRect(bubbleRect)

                if (!bubbleRect.contains(screenPosition.x.toInt(), screenPosition.y.toInt())) {
                    mapView.removeView(view)
                    currentBubbleView = null
                    updateHandler.removeCallbacks(updateRunnable)
                }
            }
            true
        }

        Log.d("MQTT Debug", "Received JSON: $data")


        val deviceLatitude = data.get("latitude")?.asJsonPrimitive?.asDouble ?: return
        val deviceLongitude = data.get("longitude")?.asJsonPrimitive?.asDouble ?: return

        // Tambahkan listener pada smartphoneTextView
        bubbleView.setOnClickListener {
            runOnUiThread {
                bubbleView.visibility = View.GONE
                val userMarker = deviceMarkers[deviceId] ?: run {
                    Log.e("MQTT Debug", "Marker untuk deviceId $deviceId tidak ditemukan")
                    return@runOnUiThread
                }
                val destinationLatitude = userMarker.geometry.latitude()
                val destinationLongitude = userMarker.geometry.longitude()

                val origin = Point.fromLngLat(destinationLongitude, destinationLatitude)
                val destination = Point.fromLngLat(deviceLongitude, deviceLatitude)
                Log.d("MQTT Debug", "asalatitude: $deviceLatitude, asalongitude: $deviceLongitude, destilatitude: $destinationLatitude, destilong: $destinationLongitude")

                // Minta rute dari Mapbox menggunakan MapboxDirections
                val routeOptions = RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(this@MapsActivity)
                    .annotationsList(listOf(DirectionsCriteria.ANNOTATION_DISTANCE, DirectionsCriteria.ANNOTATION_DURATION)) // Tambahkan anotasi
                    .coordinatesList(listOf(origin, destination))
                    .profile(DirectionsCriteria.PROFILE_WALKING)
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .build()

                mapboxNavigation.requestRoutes(
                    routeOptions,
                    object : NavigationRouterCallback {
                        override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                            if (routes.isNotEmpty()) {
                                mapboxNavigation.setNavigationRoutes(routes)
                                Log.d("Direction", "Rute ditemukan dan ditampilkan")

                                if (ActivityCompat.checkSelfPermission(
                                        this@MapsActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                        this@MapsActivity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED) {
                                    return
                                }

                                val firstRoute = routes.first().directionsRoute
                                mapboxNavigation.startTripSession()

                                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)

                                showTripViewAnimated()

                                Log.d("Direction", "Route distance: ${firstRoute.distance()} km")
                                Log.d("Direction", "Route duration: ${firstRoute.duration()} minutes")

                                Log.d("Direction", "navigationview dijalankan")
                            } else {
                                Toast.makeText(this@MapsActivity, "Tidak ada rute ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                            Toast.makeText(this@MapsActivity, "Gagal mendapatkan rute", Toast.LENGTH_SHORT).show()
                            Log.e("Direction", "Error: $reasons")
                        }

                        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                            Log.d("Direction", "Request rute dibatalkan")
                        }
                    }
                )
            }
        }

        // Ubah warna jika dalam keadaan darurat
        if (data.get("emergency")?.asBoolean == true) {
            relativeLayout.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
            smartphoneTextView.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
            imageView.imageTintList = ContextCompat.getColorStateList(this, R.color.red)
        }

        // Tampilkan informasi berdasarkan perangkat
        if (isSmartwatch) {
            val deviceId = data.get("device")?.asString ?: "Unknown Device"
            val battery = data.get("battery")?.asInt ?: -1
            val heartrate = data.get("heartrate")?.asInt ?: -1

            val titleView: TextView = bubbleView.findViewById(R.id.markerTitle)
            val ageView: TextView = bubbleView.findViewById(R.id.agemarker)
            val phoneView: TextView = bubbleView.findViewById(R.id.phonemarker)
            val batteryView: TextView = bubbleView.findViewById(R.id.gendermarker)
            val labeldevice: TextView = bubbleView.findViewById(R.id.smartphonetext)

            titleView.text = "Zein (Jamaah)"
            labeldevice.text = "Smartwatch"
            ageView.text = "Device ID: $deviceId"
            phoneView.text = "Heart Rate: $heartrate"
            batteryView.text = "Battery: $battery%"

            bubbleView.findViewById<TextView>(R.id.batterymarker).visibility = View.GONE
        } else {
            val name = data.get("name")?.asString ?: "Unknown"
            val battery = data.get("battery")?.asInt ?: -1
            val age = data.get("age")?.asString ?: "Unknown"
            val phone = data.get("phone")?.asString ?: "Unknown"
            val gender = data.get("gender")?.asString ?: "Unknown"

            val titleView: TextView = bubbleView.findViewById(R.id.markerTitle)
            val ageView: TextView = bubbleView.findViewById(R.id.agemarker)
            val phoneView: TextView = bubbleView.findViewById(R.id.phonemarker)
            val genderView: TextView = bubbleView.findViewById(R.id.gendermarker)
            val batteryView: TextView = bubbleView.findViewById(R.id.batterymarker)

            titleView.text = name
            ageView.text = "Umur: $age"
            phoneView.text = "Telpon: $phone"
            batteryView.text = "Battery: $battery%"
            genderView.text = "Gender: $gender"
        }
    }

    private fun updateTripProgressView(route: DirectionsRoute, routeProgress: RouteProgress) {
        // Pastikan tripProgressApi mendapatkan data yang benar
        val tripProgressUpdate = tripProgressApi.getTripProgress(routeProgress)

        // Log untuk debugging
        Log.d("TripView Debug", "Distance Remaining: ${routeProgress.distanceRemaining} meters")
        Log.d("TripView Debug", "Duration Remaining: ${routeProgress.durationRemaining} seconds")

        // Perbarui tampilan trip progress
        tripProgressView.render(tripProgressUpdate)
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

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation.onDestroy()
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.disconnect()
            }
            if (::mqttClient.isInitialized) {
                mqttClient.unregisterResources()
            }
            if (::mapboxNavigation.isInitialized) {
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.onDestroy()
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error during onDestroy: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.disconnect()
            }
            if (::mqttClient.isInitialized) {
                mqttClient.unregisterResources()
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error during onPause: ${e.message}")
        }
    }

    override fun onStart() {
        super.onStart()
        setupMQTT()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, mulai navigasi
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
//                mapboxNavigation.startTripSession()
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk navigasi", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTripViewAnimated() {
        navinfo.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400) // Durasi lebih lama agar lebih smooth
                .setInterpolator(OvershootInterpolator()) // Efek "melenting" saat muncul
                .start()
        }

        tripProgressView.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        parentnavinfo.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    private fun hideTripViewAnimated() {
        navinfo.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator()) // Efek percepatan keluar
            .withEndAction { navinfo.visibility = View.GONE }
            .start()

        tripProgressView.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { tripProgressView.visibility = View.GONE }
            .start()

        parentnavinfo.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(300)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { parentnavinfo.visibility = View.GONE }
            .start()
    }

}

