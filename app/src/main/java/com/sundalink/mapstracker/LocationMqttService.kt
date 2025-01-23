package com.sundalink.mapstracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import info.mqtt.android.service.Ack
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject

class LocationMqttService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mqttClient: MqttAndroidClient
    private val mqttServerUri = "tcp://93.127.162.185:1883"
    private val mqttUsername = "sundalink"
    private val mqttPassword = "@Sundalink123"
    private val mqttTopic = "sundalink/sw"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupMqttClient()
        startForegroundService()
        startLocationUpdates()
    }

    private fun setupMqttClient() {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        mqttClient = MqttAndroidClient(applicationContext, mqttServerUri, deviceId, Ack.AUTO_ACK)

        val options = MqttConnectOptions().apply {
            userName = mqttUsername
            password = mqttPassword.toCharArray()
            isAutomaticReconnect = true
            isCleanSession = true
        }

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("LocationMqttService", "Connected to MQTT Broker")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("LocationMqttService", "Failed to connect to MQTT Broker: ${exception?.message}")
            }
        })
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val channelName = "Location Tracking"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java) // Ganti MainActivity dengan activity utama Anda
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mabrur")
            .setContentText("Haji dan Umroh")
            .setSmallIcon(R.drawable.baseline_mosque_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationMqttService", "Location permission not granted")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            com.google.android.gms.location.LocationRequest.Builder(1000)
                .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                .build(),
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    for (location in locationResult.locations) {
                        sendLocationToMqtt(location)
                    }
                }
            },
            null
        )
    }

    private fun sendLocationToMqtt(location: Location) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val sharedPreferences2 = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val emergency = sharedPreferences.getBoolean("isEmergency", false)
        val avatar = sharedPreferences2.getString("avatarid", "unknown")
        val age = sharedPreferences2.getString("age", "unknown")
        val name = sharedPreferences2.getString("name", "unknown")
        val gender = sharedPreferences2.getString("gender", "unknown")
        val phone = sharedPreferences2.getString("phone", "unknown")
        val userid = sharedPreferences2.getString("user_id", "unknown")

        val payload = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("device", deviceId)
            put("emergency", emergency)
            put("avatar", avatar)
            put("name", name)
            put("gender", gender)
            put("phone", phone)
            put("age", age)
            put("userid", userid)
        }

        val message = MqttMessage(payload.toString().toByteArray()).apply {
            qos = 1 // Quality of Service level
        }

        try {
            mqttClient.publish(mqttTopic, message)
            Log.d("LocationMqttService", "Location sent to MQTT: $payload")
        } catch (e: Exception) {
            Log.e("LocationMqttService", "Failed to send message to MQTT: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.disconnect()
            }
        } catch (e: Exception) {
            Log.e("LocationMqttService", "Error during disconnect: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
