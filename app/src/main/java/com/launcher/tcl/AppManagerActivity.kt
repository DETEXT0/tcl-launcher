package com.launcher.tcl

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppManagerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var subtitleText: TextView
    private lateinit var btnShowAll: Button
    private lateinit var btnClose: Button

    private lateinit var adapter: AppManagerAdapter
    private var allApps: List<AppInfo> = emptyList()
    private val hiddenPackages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)

        prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        hiddenPackages.addAll(prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet())

        recyclerView = findViewById(R.id.managerRecycler)
        subtitleText = findViewById(R.id.managerSubtitle)
        btnShowAll   = findViewById(R.id.btnShowAll)
        btnClose     = findViewById(R.id.btnCloseManager)

        loadApps()

        btnShowAll.setOnClickListener {
            hiddenPackages.clear()
            savePreferences()
            adapter.notifyDataSetChanged()
            updateSubtitle()
        }

        btnClose.setOnClickListener {
            finish()
        }

        btnShowAll.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) 1.0f else 0.8f
        }
        btnClose.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) 1.0f else 0.8f
        }
    }

    private fun loadApps() {
        allApps = AppUtils.getAllInstalledApps(this)
        updateSubtitle()

        adapter = AppManagerAdapter(allApps, hiddenPackages) { _, _ ->
            savePreferences()
            updateSubtitle()
        }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
        recyclerView.requestFocus()
    }

    private fun updateSubtitle() {
        val visibleCount = allApps.count { it.packageName !in hiddenPackages }
        subtitleText.text = "$visibleCount visibles de ${allApps.size} apps · Presiona OK para activar/desactivar"
    }

    private fun savePreferences() {
        prefs.edit().putStringSet("hidden_apps", HashSet(hiddenPackages)).apply()
    }
}
