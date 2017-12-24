package com.supercilex.autohotspot

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class Client(app: Application) : AndroidViewModel(app) {
    var hotspot: Boolean = false
        set(value) {
            if (isConnected) {
                client.sendPayload(
                        endpointId!!,
                        Payload.fromBytes(byteArrayOf(if (value) 1 else 0))
                ).addOnCompleteListener {
                    Log.d("Client", "send payload: ${it.exception.toString()}")
                }
            }
        }

    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> get() = _connectionStatus
    private val _hotspotStatus = MutableLiveData<Boolean>()
    val hotspotStatus: LiveData<Boolean> get() = _hotspotStatus

    private val client: ConnectionsClient =
            Nearby.getConnectionsClient(getApplication<AutoHotspot>())
    private var isConnected = false
        set(value) {
            field = value
            _connectionStatus.value = value
        }
    private var endpointId: String? = null

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("Client", "Endpoint found: $endpointId, service name: ${info.serviceId}," +
                    " name: ${info.endpointName}")
            if (info.serviceId != "${getApplication<AutoHotspot>().packageName}.server") {
                throw IllegalStateException("Unknown service id: ${info.serviceId}")
            }

            client.stopDiscovery()
            client.requestConnection(
                    "Hotspot server",
                    endpointId,
                    lifecycleCallback
            ).addOnCompleteListener {
                Log.d("Client", "request connection: ${it.exception.toString()}")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("Client", "Endpoint lost: $endpointId")
        }
    }
    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d("Client", "Connection initiated: $endpointId")
            client.acceptConnection(endpointId, payloadCallback).addOnCompleteListener {
                Log.d("Client", "accept connection: ${it.exception.toString()}")
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            Log.d("Client", "Connection result: $endpointId, resolution: ${resolution.status}")
            if (resolution.status.isSuccess) {
                this@Client.endpointId = endpointId
                isConnected = true
                hotspot = hotspot
            } else {
                startDiscovery()
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("Client", "Connection disconnected: $endpointId")
            this@Client.endpointId = null
            isConnected = false
            startDiscovery()
        }
    }
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d("Client", "Payload received: $endpointId, payload: ${payload.asBytes()}")
            _hotspotStatus.value = payload.asBytes()!!.single().toInt() == 1
        }

        override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
        ) = Unit
    }

    fun init() {
        if (!isConnected) startDiscovery()
    }

    private fun startDiscovery() {
        Log.d("Client", "Starting discovery")
        client.startDiscovery(
                "${getApplication<AutoHotspot>().packageName}.server",
                discoveryCallback,
                DiscoveryOptions(Strategy.P2P_CLUSTER)
        ).addOnCompleteListener {
            Log.d("Client", "start discovery: ${it.exception.toString()}")
        }
    }

    public override fun onCleared() {
        Log.d("Client", "Killing endpoint: $endpointId")
        endpointId?.let { client.disconnectFromEndpoint(it) }
    }
}
