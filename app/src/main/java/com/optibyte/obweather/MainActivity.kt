package com.optibyte.obweather

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.bumptech.glide.Glide

class MainActivity : ComponentActivity() {

    private var location: Location? = null
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Views (to update UI after location fetch)
    private lateinit var locationTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var weatherDescriptionTextView: TextView
    private lateinit var weatherIcon: ImageView


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastKnownLocation { loc -> onLocationReady(loc) }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val weatherIconMap = mapOf(
        // Daytime icons
        "clear sky" to "https://openweathermap.org/img/wn/01d@2x.png",
        "few clouds" to "https://openweathermap.org/img/wn/02d@2x.png",
        "scattered clouds" to "https://openweathermap.org/img/wn/03d@2x.png",
        "broken clouds" to "https://openweathermap.org/img/wn/04d@2x.png",
        "shower rain" to "https://openweathermap.org/img/wn/09d@2x.png",
        "rain" to "https://openweathermap.org/img/wn/10d@2x.png",
        "thunderstorm" to "https://openweathermap.org/img/wn/11d@2x.png",
        "snow" to "https://openweathermap.org/img/wn/13d@2x.png",
        "mist" to "https://openweathermap.org/img/wn/50d@2x.png"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize views
        locationTextView = findViewById(R.id.locationTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)
        weatherDescriptionTextView = findViewById(R.id.weatherDescriptionTextView)
        weatherIcon = findViewById(R.id.weatherIcon)
        val forecastRecyclerView = findViewById<RecyclerView>(R.id.forecastRecyclerView)

        forecastAdapter = ForecastAdapter()
        forecastRecyclerView.adapter = forecastAdapter
        forecastRecyclerView.layoutManager = LinearLayoutManager(this)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLastKnownLocation { loc -> onLocationReady(loc) }
        }
    }

    private fun onLocationReady(loc: Location?) {
        if (loc == null) {
            Toast.makeText(this, "Could not retrieve location.", Toast.LENGTH_SHORT).show()
            return
        }

        location = loc
        val cityName = getCityName(loc)
        if (cityName != null) {
            fetchWeather(cityName) { weatherInfo, forecastInfo ->
                weatherInfo?.let { weather ->
                    locationTextView.text = cityName
                    temperatureTextView.text = "${weather.main.temp}°C"
                    weatherDescriptionTextView.text = weather.weather?.get(0)?.description ?: "N/A"
                    Log.d("MainActivity", "API response: $weather")

                    val weatherDescription = weather.weather?.get(0)?.description
                    val weatherIconUrl = weatherIconMap[weatherDescription]
                    if (weatherIconUrl != null) {
                        Glide.with(this)
                            .load(weatherIconUrl)
                            .into(weatherIcon)
                    } else {
                        Log.e("MainActivity", "Weather icon URL not found for description: $weatherDescription")
                    }
                }

                forecastInfo?.let { forecast ->
                    forecastAdapter.updateData(forecast.list)
                } ?: Toast.makeText(this, "Forecast data not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Unable to find city name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastKnownLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                callback(location)
            }.addOnFailureListener {
                Log.e("Location", "Failed to get location: ${it.message}")
                callback(null)
            }
        } else {
            callback(null)
        }
    }


    private fun getCityName(location: Location): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return if (addresses != null && addresses.isNotEmpty()) {
            addresses[0].locality ?: addresses[0].adminArea
        } else {
            null
        }
    }

    private fun fetchWeather(cityName: String, callback: (WeatherResponse?, ForecastResponse?) -> Unit) {
        val weatherService = RetrofitClient.instance.create(WeatherService::class.java)
        val callWeather = weatherService.getWeather(cityName, "0e8777c62f2ec2221b65684e35f7a56e")
        val callForecast = weatherService.getForecast(cityName, "0e8777c62f2ec2221b65684e35f7a56e")


        callWeather.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                val weatherResponse = if (response.isSuccessful) response.body() else null

                callForecast.enqueue(object : Callback<ForecastResponse> {
                    override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                        val forecastResponse = if (response.isSuccessful) response.body() else null
                        callback(weatherResponse, forecastResponse)
                    }

                    override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                        Log.e("Forecast API", "Error: ${t.message}")
                        Toast.makeText(this@MainActivity, "Failed to load forecast data", Toast.LENGTH_SHORT).show()
                        callback(weatherResponse, null)
                    }
                })
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("Weather API", "Error: ${t.message}")
                Toast.makeText(this@MainActivity, "Failed to load weather data", Toast.LENGTH_SHORT).show()
                callback(null, null)
            }
        })

    }
}