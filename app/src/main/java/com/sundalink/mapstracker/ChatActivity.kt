package com.sundalink.mapstracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String,
    val message: String,
    val timestamp: String,
    val senderId: String,
    val senderName: String,
    val isSelf: Boolean
)

class ChatActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false

    private lateinit var socket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.statusBarColor = resources.getColor(R.color.myprimary, theme)

        Log.d("ChatActivity", "onCreate: Initializing UI components")
        val btmNavChat = findViewById<ConstraintLayout>(R.id.btmnavchat)
        val btmNavProf = findViewById<ConstraintLayout>(R.id.profilenavbar)
        val btmNavJadwal = findViewById<ConstraintLayout>(R.id.btmnavjadwal)
        val btmnavhome = findViewById<ConstraintLayout>(R.id.btmnavhome)
        val kloterText = findViewById<TextView>(R.id.klotertext)

        val btmnav = findViewById<LinearLayout>(R.id.btmnav)
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // Jika keyboard terlihat (keypadHeight > 200, bisa disesuaikan tergantung pada perangkat)
            if (keypadHeight > screenHeight * 0.15) {
                btmnav.visibility = View.GONE // Sembunyikan bottom navigation
            } else {
                btmnav.visibility = View.VISIBLE // Tampilkan bottom navigation
            }
        }

        btmNavChat.setOnClickListener {
            Log.d("ChatActivity", "Navigating to ChatActivity")
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        btmNavProf.setOnClickListener {
            Log.d("ChatActivity", "Navigating to ProfiveActivity")
            val intent = Intent(this, ProfiveActivity::class.java)
            startActivity(intent)
        }

        btmnavhome.setOnClickListener {
            Log.d("ChatActivity", "Navigating to HomeActivity")
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        btmNavJadwal.setOnClickListener {
            Log.d("ChatActivity", "Navigating to JadwalActivity")
            val intent = Intent(this, JadwalActivity::class.java)
            startActivity(intent)
        }

        val emergencyButton = findViewById<ConstraintLayout>(R.id.sosbtn)
        sharedPreferencess = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        isEmergency = sharedPreferencess.getBoolean("isEmergency", false)
        updateButtonText(emergencyButton)
        emergencyButton.setOnClickListener {
            Log.d("ChatActivity", "Emergency button clicked")
            toggleEmergency(emergencyButton)
        }

        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        recyclerView = findViewById(R.id.recyclerView)
        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        kloterText.text = sharedPreferences.getString("umroh_schedule_name", "unknown")

        messageAdapter = MessageAdapter(this, messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter

        Log.d("ChatActivity", "RecyclerView and Adapter initialized")

        fetchMessages()
        setupSocket()

        val sendBackButton: ImageButton = findViewById(R.id.sendBack)
        sendBackButton.setOnClickListener {
            Log.d("ChatActivity", "Navigating back to MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        sendButton.setOnClickListener {
            val messageText = inputMessage.text.toString().trim()
            Log.d("ChatActivity", "Send button clicked with input: $messageText")
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                inputMessage.text.clear()
            } else {
                Log.d("ChatActivity", "Empty message, not sending")
            }
        }
    }

    private fun fetchMessages() {
        val token = sharedPreferences.getString("jwt_token", "") ?: ""
        val umrohScheduleId = sharedPreferences.getString("umroh_schedule", "") ?: ""
        val url = "https://api.mabrur.info/api/v1/umroh-schedules/$umrohScheduleId/chat-messages"

        Log.d("ChatActivity", "Fetching messages from: $url")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatActivity", "Error fetching messages: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("ChatActivity", "Fetch response: $responseBody")

                    responseBody?.let {
                        try {
                            val json = JSONObject(it)
                            if (json.getBoolean("success")) {
                                val data = json.getJSONArray("data")
                                parseMessages(data)
                            } else {

                            }
                        } catch (e: JSONException) {
                            Log.e("ChatActivity", "JSON Parsing Error: ${e.message}")
                        }
                    }
                } else {
                    Log.e("ChatActivity", "Error fetching messages, code: ${response.code}")
                }
            }
        })
    }

    private fun parseMessages(data: JSONArray) {
        Log.d("ChatActivity", "Parsing messages data: $data")
        messages.clear()
        val selfId = sharedPreferences.getString("user_id", "")
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val sender = item.getJSONObject("sender")
            val message = ChatMessage(
                id = item.getString("id"),
                message = item.getString("message"),
                timestamp = item.getString("timestamp"),
                senderId = sender.getString("id"),
                senderName = sender.getString("name"),
                isSelf = sender.getString("id") == selfId
            )
            messages.add(message)
        }
        messages.sortBy { it.timestamp }
        runOnUiThread {
            messageAdapter.notifyDataSetChanged()
            scrollToBottom()
        }
    }


    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun setupSocket() {
        try {
            socket = IO.socket("wss://api.mabrur.info")

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("ChatActivity", "Connected to WebSocket")
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("ChatActivity", "Disconnected from WebSocket, attempting to reconnect")
                socket.connect() // Reconnect
            }

            socket.on("chatMessage", Emitter.Listener { args ->
                try {
                    val message = args[0] as JSONObject
                    Log.d("ChatActivity", "Received chatMessage: $message")

                    val sender = message.getJSONObject("sender") // Ambil objek sender dari message

                    val chatMessage = ChatMessage(
                        id = message.getString("id"),
                        message = message.getString("message"),
                        timestamp = message.getString("timestamp"),
                        senderId = message.getString("sender_id"),
                        senderName = sender.getString("name"), // Ambil nama dari sender
                        isSelf = message.getString("sender_id") == sharedPreferences.getString("user_id", "")
                    )

                    runOnUiThread {
                        messages.add(chatMessage)
                        messageAdapter.notifyItemInserted(messages.size - 1)
                        recyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error processing chatMessage: ${e.message}", e)
                }
            })

            socket.connect()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error while setting up socket: ${e.message}", e)
        }
    }

    private fun scrollToBottom() {
        Log.d("ChatActivity", "Scrolling to bottom")
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun toggleEmergency(textview: ConstraintLayout) {
        isEmergency = !isEmergency
        sharedPreferencess.edit().putBoolean("isEmergency", isEmergency).apply()
        Log.d("ChatActivity", "Toggled emergency mode: $isEmergency")
        updateButtonText(textview)
    }

    private fun updateButtonText(textview: ConstraintLayout) {
        Log.d("ChatActivity", "Updating emergency button UI")
        if (isEmergency) {
            textview.background = ColorDrawable(Color.parseColor("#DC3F34"))
        } else {
            textview.background = ColorDrawable(Color.parseColor("#42BF4B"))
        }
    }

    private fun sendMessage(message: String) {
        val json = JSONObject().apply {
            put("id", getCurrentTimestamp())
            put("message", message)
            put("sender_id", sharedPreferences.getString("user_id", ""))
            put("umroh_schedule_id", sharedPreferences.getString("umroh_schedule", ""))
            put("timestamp", getCurrentTimestamp())
            put("type", "create")
        }

        Log.d("ChatActivity", "Sending message JSON: $json")
        if (message.isNotEmpty() && !json.optString("sender_id").isNullOrEmpty()) {
            socket.emit("chatMessage", json)
        } else {
            Log.e("ChatActivity", "Invalid data, message not sent")
        }
    }
}
