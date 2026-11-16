package com.launcher.tcl

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

object WeatherFetcher {

    // API gratuita de Open-Meteo (sin clave de API)
    // Coordenadas por defecto: Bogotá
    private const val DEFAULT_LAT = 4.711
    private const val DEFAULT_LON = -74.0721

    private val client = OkHttpClient()

    fun fetch(city: String, callback: (temp: String, condition: String, icon: String) -> Unit) {
        // Usamos Open-Meteo con geocoding para obtener coordenadas
        Thread {
            try {
                // Paso 1: geocoding
                val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${city}&count=1&language=es&format=json"
                val geoRequest = Request.Builder().url(geoUrl).build()
                val geoResponse = client.newCall(geoRequest).execute()
                val geoBody = geoResponse.body?.string() ?: ""

                var lat = DEFAULT_LAT
                var lon = DEFAULT_LON

                if (geoBody.isNotEmpty()) {
                    val geoJson = JSONObject(geoBody)
                    val results = geoJson.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val first = results.getJSONObject(0)
                        lat = first.getDouble("latitude")
                        lon = first.getDouble("longitude")
                    }
                }

                // Paso 2: clima actual
                val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=relativehumidity_2m&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
                val weatherRequest = Request.Builder().url(weatherUrl).build()
                val weatherResponse = client.newCall(weatherRequest).execute()
                val weatherBody = weatherResponse.body?.string() ?: ""

                if (weatherBody.isNotEmpty()) {
                    val weatherJson = JSONObject(weatherBody)
                    val current = weatherJson.getJSONObject("current_weather")
                    val temp = current.getDouble("temperature").toInt().toString()
                    val code = current.getInt("weathercode")
                    val condition = getCondition(code)
                    val icon = getIcon(code)
                    callback(temp, condition, icon)
                }
            } catch (e: IOException) {
                callback("--", "Sin conexión", "🌡️")
            } catch (e: Exception) {
                callback("--", "Error", "🌡️")
            }
        }.start()
    }

    private fun getCondition(code: Int): String = when (code) {
        0 -> "Despejado"
        1, 2, 3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55 -> "Llovizna"
        61, 63, 65 -> "Lluvia"
        71, 73, 75 -> "Nieve"
        80, 81, 82 -> "Chubascos"
        95 -> "Tormenta"
        96, 99 -> "Tormenta con granizo"
        else -> "Variable"
    }

    private fun getIcon(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2, 3 -> "⛅"
        45, 48 -> "🌫️"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
        71, 73, 75 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌤️"
    }
}
