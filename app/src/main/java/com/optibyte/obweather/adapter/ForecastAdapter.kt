package com.optibyte.obweather.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.optibyte.obweather.R
import com.optibyte.obweather.model.ForecastItem

class ForecastAdapter : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    private var forecastList: List<ForecastItem> = emptyList()

    fun updateData(newForecast: List<ForecastItem>) {
        forecastList = newForecast
        notifyDataSetChanged()  // Notify RecyclerView to refresh the data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecastItem = forecastList[position]
        holder.bind(forecastItem)
    }

    override fun getItemCount(): Int {
        return forecastList.size
    }

    inner class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val temperatureTextView: TextView = itemView.findViewById(R.id.temperatureTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)

        fun bind(forecastItem: ForecastItem) {
            // Format the date and set the temperature and description
            dateTextView.text = convertTimestampToDate(forecastItem.dt)
            temperatureTextView.text = "${forecastItem.main.temp}°C"
            descriptionTextView.text = forecastItem.weather.firstOrNull()?.description ?: "N/A"
        }

        private fun convertTimestampToDate(timestamp: Long): String {
            // Convert timestamp to date format, e.g., "Oct 29, 2024"
            val date = java.util.Date(timestamp * 1000)  // Convert seconds to milliseconds
            val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            return format.format(date)
        }
    }
}
