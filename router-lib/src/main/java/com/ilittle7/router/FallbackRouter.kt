package com.ilittle7.router

import android.content.Intent
import android.util.Log

object FallbackRouter : AbsRouter() {
    override val interceptors: List<Pair<Int, InterceptorFactory>>
        get() = routerManager.fallbackInterceptorList

    override fun start(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        return try {
            Chain(launcherWrapper, intent, actualInterceptors, 0) { pIntent ->
                try {
                    launcherWrapper.startActivity(pIntent)
                    Response(success = true, intent = pIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Can't start: ", e)
                    Response(success = false, intent = pIntent)
                }
            }.proceed(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Can't start: ", e)
            Response(success = false, intent = intent)
        }
    }

    override fun startForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent
    ): Response {
        return try {
            Chain(launcherWrapper, intent, actualInterceptors, 0) { pIntent ->
                try {
                    launcherWrapper.startActivityForResult(pIntent, requestCode)
                    Response(success = true, intent = pIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Can't start: ", e)
                    Response(success = false, intent = pIntent)
                }
            }.proceed(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Can't start: ", e)
            Response(success = false, intent = intent)
        }
    }
}