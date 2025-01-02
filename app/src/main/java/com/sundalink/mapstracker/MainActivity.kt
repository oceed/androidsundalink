// MainActivity.kt
package com.sundalink.mapstracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login_button)

        // Check if token exists
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)
        if (token != null) {
            navigateToHome()
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
//            login(username, password)
            val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("name", username)
                putString("user_type", "Jamaah") // Save user type
                apply()
            }
            navigateToHome()
        }
    }

    private fun login(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://192.168.1.143:8080/api/v1/auth/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonObject = JSONObject().apply {
                    put("identifier", username)
                    put("password", password)
                }
                Log.d("json", jsonObject.toString())

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonObject.toString())
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(response)

                    if (responseJson.getBoolean("success")) {
                        val data = responseJson.getJSONObject("data")
                        val user = data.getJSONObject("user")
                        val token = data.getString("token")

                        // Default user type
                        var userType = "user not set"

                        // Check for "jamaah" or "mutayib" in user
                        if (user.has("jamaah")) {
                            userType = "jamaah"
                        } else if (user.has("mutayib")) {
                            userType = "mutayib"
                        }

                        // Save user details to SharedPreferences
                        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putString("jwt_token", token)
                            putString("user_id", user.getString("id"))
                            putString("name", user.getString("name"))
                            putString("username", user.getString("username"))
                            putString("email", user.getString("email"))
                            putString("phone", user.getString("phone"))
                            putString("address", user.getString("address"))
                            putString("role_name", user.getJSONObject("role").getString("name"))
                            putString("user_type", userType) // Save user type
                            apply()
                        }

                        withContext(Dispatchers.Main) {
                            navigateToHome()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, responseJson.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    val errorMessage = if (!errorResponse.isNullOrEmpty()) {
                        try {
                            JSONObject(errorResponse).getString("message")
                        } catch (e: Exception) {
                            "Unknown error: $errorResponse"
                        }
                    } else {
                        "HTTP ${conn.responseCode}: ${conn.responseMessage}"
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Login failed: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
