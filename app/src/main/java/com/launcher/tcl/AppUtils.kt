package com.launcher.tcl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppUtils {

    // Paquetes a ocultar (sistema y el propio launcher)
    private val HIDDEN_PACKAGES = setOf(
        "com.launcher.tcl",
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.tcl.tvinput",
        "com.android.tv.settings"
    )

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        return resolveInfos
            .filter { it.activityInfo.packageName !in HIDDEN_PACKAGES }
            .map {
                AppInfo(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .sortedBy { it.name }
    }
}
