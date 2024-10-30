package com.optibyte.obweather.model

data class ForecastResponse(
    val list: List<ForecastItem> // List of forecast data for each time interval
)

data class ForecastItem(
    val dt: Long, // Timestamp of the forecasted time
    val main: Main,
    val weather: List<WeatherInfo>
)