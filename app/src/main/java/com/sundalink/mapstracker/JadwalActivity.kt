package com.sundalink.mapstracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.sundalink.mapstracker.databinding.ActivityJadwalBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class JadwalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJadwalBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencess: SharedPreferences
    private var isEmergency = false
    private var selectedDayId: String? = null
    private val dayList = mutableListOf<Day>()
    private val activityList = mutableListOf<Activity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJadwalBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        val scheduleId = sharedPreferences.getString("umroh_schedule", null)
        val token = sharedPreferences.getString("jwt_token", null)

        if (scheduleId == null || token == null) {
            Toast.makeText(this, "Data tidak ditemukan di SharedPreferences", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup RecyclerView
        val dayAdapter = DayAdapter(dayList) { day ->
            selectedDayId = day.id
            fetchActivities(day.id, scheduleId, token)
        }
        val activityAdapter = ActivityAdapter(activityList)

        binding.recyclerViewDays.apply {
            layoutManager = LinearLayoutManager(this@JadwalActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = dayAdapter
        }

        binding.recyclerViewActivities.apply {
            layoutManager = LinearLayoutManager(this@JadwalActivity)
            adapter = activityAdapter
        }

        // Fetch Days
        fetchDays(scheduleId, token) { days ->
            dayList.clear()
            dayList.addAll(days)
            dayAdapter.notifyDataSetChanged()

            // Automatically select the first day
            if (dayList.isNotEmpty()) {
                selectedDayId = dayList[0].id
                dayAdapter.setInitialSelectedDay(selectedDayId) // Atur hari pertama sebagai yang dipilih
                fetchActivities(dayList[0].id, scheduleId, token)
            }
        }
    }

    private fun fetchDays(scheduleId: String, token: String, onSuccess: (List<Day>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = createOkHttpClient()
            val request = Request.Builder()
                .url("http://93.127.162.185:4000/api/v1/umroh-schedules/$scheduleId/daily-programs")
                .addHeader("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val jsonObject = JSONObject(body ?: "")
                    val data = jsonObject.getJSONArray("data")
                    val days = parseDays(data)


                    val sortedDays = days.sortedBy { it.day }
                    runOnUiThread { onSuccess(sortedDays) }
                } else {
                    Log.e("FetchDays", "Error: ${response.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchActivities(dayId: String, scheduleId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = createOkHttpClient()
            val request = Request.Builder()
                .url("http://93.127.162.185:4000/api/v1/umroh-schedules/$scheduleId/daily-programs/$dayId/activities")
                .addHeader("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val jsonObject = JSONObject(body ?: "")
                    val data = jsonObject.getJSONArray("data")
                    val activities = parseActivities(data)

                    // Urutkan activities berdasarkan `time`
                    val sortedActivities = activities.sortedBy { it.time }

                    // Cari `summary_places` dari Day berdasarkan ID
                    val selectedDay = dayList.find { it.id == dayId }
                    val summaryPlaces = selectedDay?.summaryPlaces ?: "Tidak ada summary"

                    // Tambahkan summaryPlaces ke setiap activity
                    val updatedActivities = sortedActivities.map { activity ->
                        activity.copy(summaryPlaces = summaryPlaces)
                    }

                    runOnUiThread {
                        activityList.clear()
                        activityList.addAll(updatedActivities)
                        binding.recyclerViewActivities.adapter?.notifyDataSetChanged()
                    }
                } else {
                    Log.e("FetchActivities", "Error: ${response.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseDays(jsonArray: JSONArray): List<Day> {
        val days = mutableListOf<Day>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            days.add(
                Day(
                    id = obj.getString("id"),
                    day = obj.getInt("day"),
                    summaryPlaces = obj.getString("summary_places")
                )
            )
        }
        return days
    }

    private fun parseActivities(jsonArray: JSONArray): List<Activity> {
        val activities = mutableListOf<Activity>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            activities.add(
                Activity(
                    time = obj.getString("time"),
                    description = obj.getString("description")
                )
            )
        }
        return activities
    }

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
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
