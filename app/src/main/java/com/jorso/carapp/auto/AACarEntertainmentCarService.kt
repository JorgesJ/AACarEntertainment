package com.jorso.carapp.auto

import android.content.Intent
import android.os.IBinder
import android.app.Service

class AACarEntertainmentCarService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
