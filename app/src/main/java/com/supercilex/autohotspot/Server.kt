package com.supercilex.autohotspot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class Server : Service() {
    private val client: ConnectionsClient by lazy { Nearby.getConnectionsClient(this) }
    private val manager by lazy {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d("Server", "Connection initiated: $endpointId, info: $connectionInfo")
            client.acceptConnection(endpointId, payloadCallback).addOnCompleteListener {
                Log.d("Server", "accept connection: ${it.exception.toString()}")
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            Log.d("Server", "Connection result: $endpointId, resolution: ${resolution.status}")
            if (resolution.status.isSuccess) {
                client.sendPayload(
                        endpointId,
                        Payload.fromBytes(byteArrayOf(if (manager.javaClass.getDeclaredMethod(
                                "isWifiApEnabled"
                        ).invoke(manager) as Boolean) 1 else 0))
                ).addOnCompleteListener {
                    Log.d("Server", "send payload: ${it.exception.toString()}")
                }
            } else {
                startAdvertising()
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("Server", "Connection disconnected: $endpointId")
            startAdvertising()
        }
    }
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d("Server", "Payload received: $endpointId, payload: ${payload.asBytes()}")
            setHotspot(payload.asBytes()!!.single().toInt() == 1)
        }

        override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
        ) = Unit
    }

    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel(
                            "server",
                            "Hotspot server",
                            NotificationManager.IMPORTANCE_MIN
                    ).apply {
                        description = "Service to enable remotely enabling/disabling the hotspot"
                        setShowBadge(false)
                        enableVibration(false)
                        enableLights(false)
                    }
            )
        }

        startForeground(1, NotificationCompat.Builder(this, "server")
                .setSmallIcon(R.drawable.ic_room_service_white_48dp)
                .setContentText("Service to enable remotely enabling/disabling the hotspot")
                .setContentTitle("Hotspot server")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        )

        startAdvertising()
    }

    private fun startAdvertising() {
        Log.d("Server", "Starting advertising")
        client.startAdvertising(
                "Hotspot server",
                "$packageName.server",
                lifecycleCallback,
                AdvertisingOptions(Strategy.P2P_CLUSTER)
        ).addOnCompleteListener {
            Log.d("Server", "Start advertising: ${it.exception.toString()}")
        }
    }

    private fun setHotspot(enable: Boolean) {
        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                  .setData(Uri.parse("package:$packageName")))
            return
        }

        manager.isWifiEnabled = !enable
        manager.javaClass.getDeclaredMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
        ).invoke(manager, null, enable)
    }

    override fun onBind(intent: Intent): IBinder? = null
}
