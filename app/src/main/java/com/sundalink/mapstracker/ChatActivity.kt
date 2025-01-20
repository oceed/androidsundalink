package com.sundalink.mapstracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
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

    val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.statusBarColor = resources.getColor(R.color.myprimary, theme)

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

        val emergencyButton = findViewById<ConstraintLayout>(R.id.sosbtn)
        sharedPreferencess = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        isEmergency = sharedPreferencess.getBoolean("isEmergency", false)
        updateButtonText(emergencyButton)
        emergencyButton.setOnClickListener {
            toggleEmergency(emergencyButton)
        }

        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        recyclerView = findViewById(R.id.recyclerView)
        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)

        messageAdapter = MessageAdapter(this, messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter

        Log.d("ChatActivity", "Initializing ChatActivity")
        fetchMessages()

        val sendBackButton: ImageButton = findViewById(R.id.sendBack)
        sendBackButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
             finish()
        }

        sendButton.setOnClickListener {
            val messageText = inputMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                Log.d("ChatActivity", "Send button clicked with message: $messageText")
                sendMessage(messageText)
                inputMessage.text.clear()
            }
        }
    }

    private fun fetchMessages() {
        val token = sharedPreferences.getString("jwt_token", "Unknown")
        val umrohScheduleId = sharedPreferences.getString("umroh_schedule", "Unknown")
        val url = "http://93.127.162.185:4000/api/v1/umroh-schedules/$umrohScheduleId/chat-messages"

        Log.d("ChatActivity", "Fetching messages from URL: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatActivity", "Error message: ${e.message}")
                Log.e("ChatActivity", "Error cause: ${e.cause}")
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("ChatActivity", "Fetch messages response: $responseBody")

                responseBody?.let {
                    val json = JSONObject(it)
                    if (json.getBoolean("success")) {
                        val data = json.getJSONArray("data")
                        runOnUiThread {
                            parseMessages(data)
                        }
                    }
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
            Log.d("ChatActivity", "Parsed message: $message")
            messages.add(message)
        }
        messages.sortBy { it.timestamp }
        messageAdapter.notifyDataSetChanged()
        recyclerView.post { scrollToBottom() }
    }

    private fun scrollToBottom() {
        recyclerView.scrollToPosition(messages.size - 1)
    }


    private fun sendMessage(message: String) {
        val token = sharedPreferences.getString("jwt_token", "") ?: ""
        val umrohScheduleId = sharedPreferences.getString("umroh_schedule", "") ?: ""
        val url = "http://93.127.162.185:4000/api/v1/umroh-schedules/$umrohScheduleId/chat-messages"
        val timestamp = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_INSTANT)
        val json = JSONObject().apply {
            put("message", message)
            put("timestamp", timestamp)
        }

        Log.d("ChatActivity", "Sending message: $json to URL: $url")

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatActivity", "Failed to send message: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("ChatActivity", "Send message response: $responseBody")

                if (response.isSuccessful) {
                    fetchMessages()
                    recyclerView.post { scrollToBottom() }
                }
            }
        })
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
