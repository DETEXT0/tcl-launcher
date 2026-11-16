package com.launcher.tcl

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var wallpaperView: ImageView
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var weatherText: TextView
    private lateinit var weatherIcon: TextView
    private lateinit var appsRecycler: RecyclerView
    private lateinit var appAdapter: AppAdapter

    private val clockHandler = Handler(Looper.getMainLooper())
    private val weatherHandler = Handler(Looper.getMainLooper())

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }

    private val weatherRunnable = object : Runnable {
        override fun run() {
            fetchWeather()
            weatherHandler.postDelayed(this, 10 * 60 * 1000) // cada 10 min
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pantalla completa sin barras
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)

        wallpaperView = findViewById(R.id.wallpaperView)
        clockText = findViewById(R.id.clockText)
        dateText = findViewById(R.id.dateText)
        weatherText = findViewById(R.id.weatherText)
        weatherIcon = findViewById(R.id.weatherIcon)
        appsRecycler = findViewById(R.id.appsRecycler)

        setupWallpaper()
        setupAppGrid()
        setupChangeWallpaperButton()
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockRunnable)
        weatherHandler.post(weatherRunnable)
        setupWallpaper()
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
        weatherHandler.removeCallbacks(weatherRunnable)
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE\nd 'de' MMMM", Locale("es", "ES"))
        clockText.text = timeFormat.format(now.time)
        dateText.text = dateFormat.format(now.time).uppercase()
    }

    private fun setupWallpaper() {
        val wallpaperPath = prefs.getString("wallpaper_path", null)
        if (wallpaperPath != null) {
            val file = File(wallpaperPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(wallpaperPath)
                wallpaperView.setImageBitmap(bitmap)
                return
            }
        }
        // Fondo por defecto (Snoopy)
        wallpaperView.setImageResource(R.drawable.wallpaper_default)
    }

    private fun setupAppGrid() {
        val apps = AppUtils.getInstalledApps(this)
        appAdapter = AppAdapter(apps) { appInfo ->
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            launchIntent?.let { startActivity(it) }
        }
        appsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        appsRecycler.adapter = appAdapter
        // Enfocar el primer ítem para navegación con control remoto
        appsRecycler.requestFocus()
    }

    private fun setupChangeWallpaperButton() {
        val btnChange = findViewById<View>(R.id.btnChangeWallpaper)
        btnChange.setOnClickListener {
            startActivityForResult(
                Intent(this, WallpaperPickerActivity::class.java),
                REQUEST_WALLPAPER
            )
        }
        btnChange.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) 1.0f else 0.6f
        }
    }

    private fun fetchWeather() {
        val city = prefs.getString("weather_city", "Bogota") ?: "Bogota"
        WeatherFetcher.fetch(city) { temp, condition, icon ->
            runOnUiThread {
                weatherText.text = "$temp°C · $condition"
                weatherIcon.text = icon
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WALLPAPER && resultCode == RESULT_OK) {
            val path = data?.getStringExtra("wallpaper_path")
            if (path != null) {
                prefs.edit().putString("wallpaper_path", path).apply()
                setupWallpaper()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Evitar que el botón HOME salga del launcher
        if (keyCode == KeyEvent.KEYCODE_HOME) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        // No salir del launcher con back
    }

    companion object {
        const val REQUEST_WALLPAPER = 100
    }
}
