package com.ilittle7.router.interceptor

import com.ilittle7.router.Chain
import com.ilittle7.router.FallbackInterceptor
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Priority
import com.ilittle7.router.Response
import timber.log.Timber

@FallbackInterceptor
@Priority(3)
class TestAInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        Timber.e("onIntercept()")
        val intent = chain.proceed(chain.requestIntent)
        Timber.e("onReverseIntercept()")
        return intent
    }
}