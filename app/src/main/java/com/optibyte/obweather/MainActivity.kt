package com.optibyte.obweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.optibyte.obweather.api.WeatherService
import com.optibyte.obweather.model.WeatherResponse
import com.optibyte.obweather.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.location.LocationManager
import android.content.Context
import android.location.Location
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var locationManager: LocationManager
    private var location: Location? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            WeatherApp(location)
        }
    }
}

@Composable
fun WeatherApp(location: Location?) {
    var weatherInfo by remember { mutableStateOf("Loading...") }
    var locationInput by remember { mutableStateOf("") } // State for manual location input
    var isFetchingWeather by remember { mutableStateOf(false) } // State to indicate fetching weather
    var isLocationSelected by remember { mutableStateOf(false) } // State to check if location is selected

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Show the TextField only if location is not selected
        if (!isLocationSelected) {
            TextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text("Enter city name or coordinates (lat, lon)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                isFetchingWeather = true
                fetchWeather(locationInput) { newWeatherInfo ->
                    weatherInfo = newWeatherInfo
                    isFetchingWeather = false
                    // Set location selected to true if fetching is successful
                    isLocationSelected = weatherInfo != "Failed to load weather data"
                }
            }) {
                Text("Get Weather")
            }
        } else {
            // When a location is selected, show weather info only
            Text(
                text = weatherInfo,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Optionally, provide a button to refresh or change location
            Button(onClick = {
                isLocationSelected = false // Reset to allow new input
                locationInput = "" // Clear the previous input
                weatherInfo = "Loading..." // Reset weather info
            }) {
                Text("Change Location")
            }
        }

        if (isFetchingWeather) {
            CircularProgressIndicator() // Show loading indicator while fetching
        }
    }
}



private fun fetchWeather(locationInput: String, callback: (String) -> Unit) {
    val weatherService = RetrofitClient.instance.create(WeatherService::class.java)
    val call: Call<WeatherResponse>

    // Check if the input is numeric (for coordinates) or a city name
    if (locationInput.all { it.isDigit() || it == '.' || it == '-' || it == ',' }) {
        // Assuming coordinates are provided in "latitude,longitude" format
        val coords = locationInput.split(",").map { it.trim() }
        if (coords.size == 2) {
            val lat = coords[0].toDoubleOrNull()
            val lon = coords[1].toDoubleOrNull()
            if (lat != null && lon != null) {
                call = weatherService.getWeatherByCoordinates(lat, lon, "6674d7db7b7dd34c35e2dccd7dae9daa")
            } else {
                callback("Invalid coordinates format")
                return
            }
        } else {
            callback("Invalid coordinates format")
            return
        }
    } else {
        // Assuming a city name is provided
        call = weatherService.getWeather(locationInput, "6674d7db7b7dd34c35e2dccd7dae9daa")
    }

    call.enqueue(object : Callback<WeatherResponse> {
        override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
            if (response.isSuccessful) {
                val weather = response.body()
                val newWeatherInfo = "Temperature: ${weather?.main?.temp}°C, Humidity: ${weather?.main?.humidity}%"
                callback(newWeatherInfo)
            } else {
                callback("Failed to load weather data: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
            callback("Failed to load weather data: ${t.localizedMessage}")
        }
    })
}
