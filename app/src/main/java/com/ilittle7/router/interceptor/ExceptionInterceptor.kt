package com.ilittle7.router.interceptor

import android.widget.Toast
import com.ilittle7.router.Chain
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Priority
import com.ilittle7.router.Response

@Priority(1)
class ExceptionInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        val resp = chain.proceed(chain.requestIntent)
        if (resp.success.not()) {
            Toast.makeText(chain.launcherWrapper.context, "error", Toast.LENGTH_SHORT).show()
        }
        return resp
    }
}