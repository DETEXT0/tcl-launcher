package com.launcher.tcl

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager

object AppUtils {

    // Paquetes del sistema y utilidades que deben ocultarse por defecto
    val DEFAULT_HIDDEN_PACKAGES = setOf(
        "com.launcher.tcl",
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.tcl.tvinput",
        "com.android.tv.settings",
        "com.tcl.notereminder",
        "com.tcl.usercenter"
    )

    /**
     * Obtiene todas las aplicaciones instaladas que pueden ser iniciadas,
     * combinando Leanback (Android TV) y aplicaciones móviles estándar (sideloaded).
     */
    fun getAllInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val seenPackages = mutableSetOf<String>()
        val result = mutableListOf<AppInfo>()

        // 1. Aplicaciones con categoría LEANBACK_LAUNCHER (específicas para TV)
        val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvApps = pm.queryIntentActivities(tvIntent, PackageManager.GET_META_DATA)
        for (info in tvApps) {
            val pkg = info.activityInfo.packageName
            if (pkg != context.packageName && seenPackages.add(pkg)) {
                result.add(
                    AppInfo(
                        name = info.loadLabel(pm).toString(),
                        packageName = pkg,
                        icon = info.loadIcon(pm)
                    )
                )
            }
        }

        // 2. Aplicaciones con categoría LAUNCHER estándar (apps normales / sideloaded)
        val standardIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val standardApps = pm.queryIntentActivities(standardIntent, PackageManager.GET_META_DATA)
        for (info in standardApps) {
            val pkg = info.activityInfo.packageName
            if (pkg != context.packageName && seenPackages.add(pkg)) {
                result.add(
                    AppInfo(
                        name = info.loadLabel(pm).toString(),
                        packageName = pkg,
                        icon = info.loadIcon(pm)
                    )
                )
            }
        }

        return result.sortedBy { it.name.lowercase() }
    }

    /**
     * Obtiene las aplicaciones a mostrar en la pantalla principal del launcher,
     * excluyendo las de sistema por defecto y las ocultadas por el usuario.
     */
    fun getInstalledApps(context: Context, prefs: SharedPreferences? = null): List<AppInfo> {
        val allApps = getAllInstalledApps(context)
        val userHidden = prefs?.getStringSet("hidden_apps", emptySet()) ?: emptySet()

        return allApps.filter { app ->
            app.packageName !in DEFAULT_HIDDEN_PACKAGES && app.packageName !in userHidden
        }
    }
}
