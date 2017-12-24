package com.supercilex.autohotspot

import android.content.Context
import android.content.SharedPreferences
import com.supercilex.autohotspot.AutoHotspot.Companion.INSTANCE

var isServer: Boolean
    get() = prefs.getBoolean("isServer", false)
    set(value) = prefs.edit().putBoolean("isServer", value).apply()

private val prefs: SharedPreferences by lazy {
    INSTANCE.getSharedPreferences("prefs", Context.MODE_PRIVATE)
}
