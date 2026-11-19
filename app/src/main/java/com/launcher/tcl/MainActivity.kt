package com.launcher.tcl

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var dayText: TextView
    private lateinit var dateText: TextView
    private lateinit var weatherText: TextView
    private lateinit var weatherIcon: TextView
    private lateinit var weatherMinMax: TextView
    private lateinit var appsRecycler: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var musicWidget: View
    private lateinit var musicTitle: TextView
    private lateinit var musicArtist: TextView
    private lateinit var musicAlbumArt: ImageView
    private lateinit var btnPlayPause: ImageView

    private val clockHandler = Handler(Looper.getMainLooper())
    private val weatherHandler = Handler(Looper.getMainLooper())
    private val musicHandler = Handler(Looper.getMainLooper())

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }

    private val weatherRunnable = object : Runnable {
        override fun run() {
            fetchWeather()
            weatherHandler.postDelayed(this, 10 * 60 * 1000)
        }
    }

    private val musicRunnable = object : Runnable {
        override fun run() {
            updateMusicWidget()
            musicHandler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)

        wallpaperView    = findViewById(R.id.wallpaperView)
        clockText        = findViewById(R.id.clockText)
        dayText          = findViewById(R.id.dayText)
        dateText         = findViewById(R.id.dateText)
        weatherText      = findViewById(R.id.weatherText)
        weatherIcon      = findViewById(R.id.weatherIcon)
        weatherMinMax    = findViewById(R.id.weatherMinMax)
        appsRecycler     = findViewById(R.id.appsRecycler)
        musicWidget      = findViewById(R.id.musicWidget)
        musicTitle       = findViewById(R.id.musicTitle)
        musicArtist      = findViewById(R.id.musicArtist)
        musicAlbumArt    = findViewById(R.id.musicAlbumArt)
        btnPlayPause     = findViewById(R.id.btnPlayPause)

        setupWallpaper()
        setupAppGrid()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockRunnable)
        weatherHandler.post(weatherRunnable)
        musicHandler.post(musicRunnable)
        setupWallpaper()
        refreshApps()
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
        weatherHandler.removeCallbacks(weatherRunnable)
        musicHandler.removeCallbacks(musicRunnable)
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val timeFormat  = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayFormat   = SimpleDateFormat("EEEE", Locale("es", "ES"))
        val dateFormat  = SimpleDateFormat("d 'DE' MMMM", Locale("es", "ES"))
        clockText.text = timeFormat.format(now.time)
        dayText.text   = dayFormat.format(now.time).uppercase()
        dateText.text  = dateFormat.format(now.time)
    }

    private fun setupWallpaper() {
        val path = prefs.getString("wallpaper_path", null)
        if (path != null && File(path).exists()) {
            wallpaperView.setImageBitmap(BitmapFactory.decodeFile(path))
        } else {
            wallpaperView.setImageResource(R.drawable.wallpaper_default)
        }
    }

    private fun setupAppGrid() {
        val apps = AppUtils.getInstalledApps(this, prefs)
        appAdapter = AppAdapter(apps,
            onAppClick = { app ->
                packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
            },
            onAppLongClick = { app ->
                // Mantener OK: abrir gestor de apps
                startActivity(Intent(this, AppManagerActivity::class.java))
                true
            }
        )
        appsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        appsRecycler.adapter = appAdapter
        appsRecycler.requestFocus()
    }

    private fun refreshApps() {
        val apps = AppUtils.getInstalledApps(this, prefs)
        appAdapter.updateApps(apps)
    }

    private fun setupButtons() {
        // Cambiar fondo
        findViewById<ImageView>(R.id.btnChangeWallpaper).setOnClickListener {
            startActivityForResult(Intent(this, WallpaperPickerActivity::class.java), REQUEST_WALLPAPER)
        }

        // Ajustes del sistema
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        // WiFi
        findViewById<ImageView>(R.id.btnWifi).setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        // Entradas HDMI
        findViewById<ImageView>(R.id.btnInputs).setOnClickListener {
            try {
                val intent = Intent().apply {
                    setClassName("com.tcl.tvinput", "com.tcl.tvinput.ui.TvInputActivity")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback: abrir ajustes generales
                startActivity(Intent(Settings.ACTION_SETTINGS))
                Toast.makeText(this, "Usa el control para cambiar entrada", Toast.LENGTH_SHORT).show()
            }
        }

        // Gestionar apps
        findViewById<ImageView>(R.id.btnManageApps).setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }

        // Música: play/pause
        btnPlayPause.setOnClickListener {
            sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
        findViewById<ImageView>(R.id.btnPrev).setOnClickListener {
            sendMediaCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
        findViewById<ImageView>(R.id.btnNext).setOnClickListener {
            sendMediaCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
        }

        // Focus effects para todos los botones
        listOf(R.id.btnChangeWallpaper, R.id.btnSettings, R.id.btnWifi, R.id.btnInputs,
               R.id.btnManageApps, R.id.btnPrev, R.id.btnPlayPause, R.id.btnNext).forEach { id ->
            findViewById<View>(id).setOnFocusChangeListener { v, hasFocus ->
                v.alpha = if (hasFocus) 1.0f else 0.7f
            }
        }
    }

    private fun sendMediaCommand(keyCode: Int) {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = msm.getActiveSessions(null)
            controllers.firstOrNull()?.dispatchMediaButtonEvent(event)
        } catch (e: Exception) {
            // Sin permiso de notificaciones, enviar broadcast
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, event)
            }
            sendBroadcast(intent)
        }
    }

    private fun updateMusicWidget() {
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = msm.getActiveSessions(null)
            val active = controllers.firstOrNull { c ->
                c.playbackState?.state == PlaybackState.STATE_PLAYING ||
                c.playbackState?.state == PlaybackState.STATE_PAUSED
            }

            if (active != null) {
                val meta = active.metadata
                val title  = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Sin título"
                val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "Artista"
                val art    = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                val isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING

                musicTitle.text  = title
                musicArtist.text = artist
                if (art != null) musicAlbumArt.setImageBitmap(art)
                btnPlayPause.setImageResource(
                    if (isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )
                musicWidget.visibility = View.VISIBLE
            } else {
                musicWidget.visibility = View.GONE
            }
        } catch (e: Exception) {
            musicWidget.visibility = View.GONE
        }
    }

    private fun fetchWeather() {
        val city = prefs.getString("weather_city", "Bogota") ?: "Bogota"
        WeatherFetcher.fetch(city) { temp, condition, icon, maxT, minT ->
            runOnUiThread {
                weatherText.text   = "$temp°C · $condition"
                weatherIcon.text   = icon
                weatherMinMax.text = "Máx: $maxT° / Mín: $minT°"
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
        if (keyCode == KeyEvent.KEYCODE_HOME) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() { /* No salir */ }

    companion object {
        const val REQUEST_WALLPAPER = 100
    }
}
