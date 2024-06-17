package net.ankio.auto.api

import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import net.ankio.auto.BuildConfig

object XPrefManager {
    private const val DEBUG = "setting_debug"
    private const val TAG = "setting"

    val pref: SharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID, TAG)

    val debug: Boolean
        get() = pref.getBoolean(DEBUG, false)
}