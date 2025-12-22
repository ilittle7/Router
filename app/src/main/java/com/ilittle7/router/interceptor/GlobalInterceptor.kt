package com.ilittle7.router.interceptor

import android.util.Log
import com.ilittle7.router.Chain
import com.ilittle7.router.GlobalInterceptor
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Priority
import com.ilittle7.router.Response

@GlobalInterceptor
@Priority(10)
object HideInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        val intent = chain.requestIntent
        Log.i("router", intent.data.toString())
        return if (intent.hasExtra("export")) Response(false, intent) else chain.proceed(intent)
    }
}

