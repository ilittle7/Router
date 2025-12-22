package com.ilittle7.router.interceptor

import com.ilittle7.router.Chain
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Priority
import com.ilittle7.router.Response
import timber.log.Timber

@Priority(11)
class ExportInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        Timber.e("ExportInterceptor.onIntercept()")
        val intent = chain.requestIntent
        if (intent.hasExtra("export")) {
            intent.removeExtra("export")
        }
        return chain.proceed(intent)
    }
}