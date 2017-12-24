package com.supercilex.autohotspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat

class BootReciever : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isServer) {
            ContextCompat.startForegroundService(context, Intent(context, Server::class.java))
        }
    }
}
