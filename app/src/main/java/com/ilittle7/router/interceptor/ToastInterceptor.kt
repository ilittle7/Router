package com.ilittle7.router.interceptor

import android.widget.Toast
import com.ilittle7.router.Chain
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Priority
import com.ilittle7.router.Response
import timber.log.Timber

@Priority(2)
object ToastInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        Timber.e("before route")
        Toast.makeText(
            chain.launcherWrapper.context,
            "ToastInterceptor: ${chain.requestIntent}",
            Toast.LENGTH_LONG
        ).show()
        val resp: Response = chain.proceed(chain.requestIntent)
        Timber.e("after route")
        return resp
    }
}