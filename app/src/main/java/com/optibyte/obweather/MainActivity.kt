package com.optibyte.obweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.optibyte.obweather.api.RetrofitClient
import com.optibyte.obweather.api.WeatherService
import com.optibyte.obweather.model.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var locationManager: LocationManager
    private var location: Location? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastKnownLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // Use the XML layout

        // Initialize views from the XML layout
        val locationTextView = findViewById<TextView>(R.id.locationTextView)
        val temperatureTextView = findViewById<TextView>(R.id.temperatureTextView)
        val weatherDescriptionTextView = findViewById<TextView>(R.id.weatherDescriptionTextView)
        val forecastRecyclerView = findViewById<RecyclerView>(R.id.forecastRecyclerView)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLastKnownLocation()
        }

        // Optionally update the UI once location is fetched
        location?.let {
            val cityName = getCityName(it)  // Get the city name from coordinates
            if (cityName != null) {
                fetchWeather(cityName) { weatherInfo ->
                    if (weatherInfo != null) {
                        val temperature = "Temperature: ${weatherInfo.main.temp}°C"
                        val description = weatherInfo.weather?.get(0)?.description ?: "N/A"

                        locationTextView.text = cityName  // Update with the actual city name
                        temperatureTextView.text = temperature
                        weatherDescriptionTextView.text = description
                    } else {
                        Toast.makeText(this, "Weather data not available", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Unable to find city name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
    }

    private fun getCityName(location: Location): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return if (addresses != null && addresses.isNotEmpty()) {
            addresses[0].locality ?: addresses[0].adminArea // Fallback to admin area if locality is null
        } else {
            null
        }
    }

    private fun fetchWeather(cityName: String, callback: (WeatherResponse?) -> Unit) {
        val weatherService = RetrofitClient.instance.create(WeatherService::class.java)
        val call = weatherService.getWeather(cityName, "0e8777c62f2ec2221b65684e35f7a56e") // Replace with your actual API key

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    Log.e("Weather API", "Error response code: ${response.code()}")
                    Log.e("Weather API", "Error response body: ${response.errorBody()?.string()}")
                    callback(null)
                    Toast.makeText(this@MainActivity, "Failed to load weather data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                callback(null)
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}



