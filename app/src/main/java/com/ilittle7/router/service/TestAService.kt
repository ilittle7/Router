package com.ilittle7.router.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ilittle7.router.Router
import timber.log.Timber

@Router
class TestAService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.e("TestAService::onStartCommand[$intent, $flags, $startId]")
        return super.onStartCommand(intent, flags, startId)
    }
}