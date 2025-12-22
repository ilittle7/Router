package com.ilittle7.router.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ilittle7.router.Router
import com.ilittle7.router.custom_annotation.LoginRequired
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.collections.iterator

@LoginRequired
@Router(["/testService"])
class TestService : Service() {
    private val scope = MainScope()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val map = mapOf(
            "p1" to intent?.getStringExtra("p1").orEmpty(),
            "p2" to intent?.getStringExtra("p2").orEmpty()
        )
        scope.launch {
            for ((k, v) in map) {
                delay(1000)
                Timber.e(
                    "test service output #$k to value: #$v, current thread is ${Thread.currentThread()}"
                )
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.e("onDestroy()")
        scope.cancel()
        super.onDestroy()
    }
}