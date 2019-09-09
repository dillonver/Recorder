package com.dillon.recorder

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log


class CallProtectService : Service() {

    override fun onCreate() {
        val i = Intent(this, CallListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i)
        } else {
            startService(i)
        }
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
