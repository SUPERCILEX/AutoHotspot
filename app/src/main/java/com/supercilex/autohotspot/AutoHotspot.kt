package com.supercilex.autohotspot

import android.app.Application

class AutoHotspot : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: AutoHotspot
            private set
    }
}
