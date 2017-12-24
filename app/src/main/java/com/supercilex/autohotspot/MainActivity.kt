package com.supercilex.autohotspot

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
    private val client by lazy { ViewModelProviders.of(this).get(Client::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            onCheckedChanged(isServerSwitch, isServer)
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "Needed for Nearby to work",
                    5,
                    Manifest.permission.ACCESS_FINE_LOCATION)
        }

        isServerSwitch.isChecked = isServer
        isServerSwitch.setOnCheckedChangeListener(this)
        hotspotSwitch.setOnCheckedChangeListener(this)

        client.connectionStatus.observe(this, Observer {
            hotspotSwitch.isEnabled = it!!
        })
        client.hotspotStatus.observe(this, Observer {
            hotspotSwitch.isChecked = it!!
        })
    }

    override fun onCheckedChanged(
            buttonView: CompoundButton,
            isChecked: Boolean
    ) = when (buttonView) {
        isServerSwitch -> {
            isServer = isChecked
            hotspotSwitch.isEnabled = !isChecked && client.connectionStatus.value == true
            if (isChecked) {
                client.onCleared()
                ContextCompat.startForegroundService(
                        this,
                        Intent(this, Server::class.java)
                )
            } else {
                client.init()
            }
        }
        hotspotSwitch -> client.hotspot = isChecked
        else -> throw IllegalStateException("Unknown button: $buttonView")
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(5)
    fun updateHotspotStatus() {
        onCheckedChanged(isServerSwitch, isServer)
    }
}
