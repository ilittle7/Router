package com.ilittle7.router.interceptor

import android.widget.Toast
import com.ilittle7.router.Chain
import com.ilittle7.router.FallbackInterceptor
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Response

// @GlobalInterceptor will override @FallbackInterceptor
@FallbackInterceptor
class FallbackInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        Toast.makeText(chain.launcherWrapper.context, "Fallback Interceptor", Toast.LENGTH_SHORT).show()
        return chain.proceed(chain.requestIntent)
    }
}