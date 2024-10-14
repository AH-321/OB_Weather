package com.optibyte.obweather.model

data class WeatherResponse(
    val name: String,
    val main: Main
)

data class Main(
    val temp: Double,
    val humidity: Int
)