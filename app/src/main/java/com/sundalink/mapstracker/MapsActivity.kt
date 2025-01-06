package com.sundalink.mapstracker

import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sundalink.mapstracker.utils.SSLUtils
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapsActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mapController: MapController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mqttClient: MqttAndroidClient

    // Map to store markers by device ID
    private val deviceMarkers: MutableMap<String, Marker> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Configure OSM
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        mapController = mapView.controller as MapController
        mapController.zoomTo(15) // Default zoom level
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermissionAndFetchLocation()

        // Initialize MQTT
        setupMQTT()
    }

    private fun checkLocationPermissionAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun fetchDeviceLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)

                    mapController.setCenter(geoPoint)
                    mapController.animateTo(geoPoint)

                    val marker = Marker(mapView).apply {
                        position = geoPoint
                        title = "Your Location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.clear()
                    mapView.overlays.add(marker)
                    mapView.invalidate()
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                    Log.e("MapsActivity", "Location is null")
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("MapsActivity", "Error fetching location: ${it.message}")
            }
        } catch (e: SecurityException) {
            Log.e("MapsActivity", "Location access error: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMQTT() {
        val serverUri = "tcp://93.127.162.185:1883" // Non-SSL MQTT broker
        val clientId = "AndroidClient"
        val username = "sundalink"
        val passwordd = "@Sundalink123"

        mqttClient = MqttAndroidClient(applicationContext, serverUri, clientId)
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e("MQTT", "Connection Lost: ${cause?.message}")
                Toast.makeText(this@MapsActivity, "MQTT Connection Lost", Toast.LENGTH_SHORT).show()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("MQTT", "Message arrived on topic: $topic, message: ${message.toString()}")
                if (message != null) {
                    handleMQTTMessage(String(message.payload))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "Delivery complete for token: $token")
            }
        })

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            userName = username
            password = passwordd.toCharArray()
        }

        try {
            Log.d("MQTT", "Connecting to MQTT broker at: $serverUri with client ID: $clientId")
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Successfully connected to the MQTT broker.")
                    try {
                        mqttClient.subscribe("sundalink/sw/4bcd5678-ef01-4234-abcd-ef9876543210", 1)
                        Log.d("MQTT", "Subscribed to topic: sundalink/sw/4bcd5678-ef01-4234-abcd-ef9876543210")
                        Toast.makeText(this@MapsActivity, "Connected to MQTT Broker", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("MQTT", "Error subscribing to topic: ${e.message}")
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to connect to MQTT broker: ${exception?.message}")
                    Toast.makeText(this@MapsActivity, "Failed to Connect to MQTT Broker", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Exception while connecting to MQTT broker: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleMQTTMessage(payload: String) {
        try {
            Log.d("MQTT", "Processing MQTT message payload: $payload")
            val data = JSONObject(payload)
            val heartRate = data.getInt("heart_rate")
            val latitude = data.getDouble("latitude")
            val longitude = data.getDouble("longitude")
            val timestamp = data.getString("timestamp")
            val device = data.getString("device")
            val emergency = data.getInt("emergency")

            val geoPoint = GeoPoint(latitude, longitude)

            val marker = deviceMarkers[device] ?: Marker(mapView).apply {
                mapView.overlays.add(this)
                deviceMarkers[device] = this
            }

            marker.position = geoPoint
            marker.title = "Device: $device\nHeart Rate: $heartRate\nTimestamp: $timestamp"
            marker.icon = if (emergency == 1) {
                resources.getDrawable(R.drawable.ic_emergency, null)
            } else {
                resources.getDrawable(R.drawable.ic_normal, null)
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.setOnMarkerClickListener { item, _ ->
                item.showInfoWindow()
                true
            }

            mapView.invalidate()
        } catch (e: Exception) {
            Log.e("MQTT", "Error handling MQTT message: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        mapView.onPause()
    }

    override fun onDestroy() {
        mqttClient.disconnect()
        super.onDestroy()
    }
}