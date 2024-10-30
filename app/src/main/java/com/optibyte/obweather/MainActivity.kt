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
import com.optibyte.obweather.model.ForecastResponse
import com.optibyte.obweather.model.WeatherResponse
import com.optibyte.obweather.adapter.ForecastAdapter

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var locationManager: LocationManager
    private var location: Location? = null
    private lateinit var ForecastAdapter: ForecastAdapter



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
        setContentView(R.layout.activity_main)

        // Initialize views from XML layout
        val locationTextView = findViewById<TextView>(R.id.locationTextView)
        val temperatureTextView = findViewById<TextView>(R.id.temperatureTextView)
        val weatherDescriptionTextView = findViewById<TextView>(R.id.weatherDescriptionTextView)
        val forecastRecyclerView = findViewById<RecyclerView>(R.id.forecastRecyclerView)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Initialize RecyclerView and Adapter
        forecastAdapter = ForecastAdapter()
        forecastRecyclerView.adapter = forecastAdapter
        forecastRecyclerView.layoutManager = LinearLayoutManager(this)  // Set layout manager

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLastKnownLocation()
        }

        location?.let {
            val cityName = getCityName(it)
            if (cityName != null) {
                fetchWeather(cityName) { weatherInfo, forecastInfo ->
                    weatherInfo?.let { weather ->
                        locationTextView.text = cityName
                        temperatureTextView.text = "${weather.main.temp}°C"
                        weatherDescriptionTextView.text = weather.weather?.get(0)?.description ?: "N/A"
                    }

                    forecastInfo?.let { forecast ->
                        ForecastAdapter.updateData(forecast.list)  // Update adapter with forecast data
                    } ?: Toast.makeText(this, "Forecast data not available", Toast.LENGTH_SHORT).show()
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

    private fun fetchWeather(cityName: String, callback: (WeatherResponse?, ForecastResponse?) -> Unit) {
        val weatherService = RetrofitClient.instance.create(WeatherService::class.java)
        val callWeather = weatherService.getWeather(cityName, "0e8777c62f2ec2221b65684e35f7a56e")
        val callForecast = weatherService.getForecast(cityName, "0e8777c62f2ec2221b65684e35f7a56e")

        // Make weather API call
        callWeather.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                val weatherResponse = if (response.isSuccessful) response.body() else null  // Assign weather data

                // Make forecast API call
                callForecast.enqueue(object : Callback<ForecastResponse> {
                    override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                        val forecastResponse = if (response.isSuccessful) response.body() else null  // Assign forecast data
                        callback(weatherResponse, forecastResponse)  // Pass both responses into callback
                    }

                    override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                        Log.e("Forecast API", "Error fetching forecast: ${t.message}")
                        Toast.makeText(this@MainActivity, "Failed to load forecast data", Toast.LENGTH_SHORT).show()
                        callback(weatherResponse, null)  // Pass weatherResponse, even if forecast fails
                    }
                })
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("Weather API", "Error fetching weather: ${t.message}")
                Toast.makeText(this@MainActivity, "Failed to load weather data", Toast.LENGTH_SHORT).show()
                callback(null, null)  // Pass null for both if weather call fails
            }
        })
    }

}

