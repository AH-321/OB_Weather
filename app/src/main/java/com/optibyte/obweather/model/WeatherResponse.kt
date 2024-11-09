package com.optibyte.obweather.model

data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<WeatherInfo>
)

data class WeatherInfo(
    val description: String
)


data class Main(
    val temp: Double,
    val humidity: Int
)
