package com.sundalink.mapstracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private var selectedDayId: String? = null
    private val dayList = mutableListOf<Day>()
    private val activityList = mutableListOf<Activity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJadwalBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    runOnUiThread { onSuccess(days) }
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
                    runOnUiThread {
                        activityList.clear()
                        activityList.addAll(activities)
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
}
