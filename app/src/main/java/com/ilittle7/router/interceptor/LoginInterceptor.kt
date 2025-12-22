package com.ilittle7.router.interceptor

import com.ilittle7.router.Chain
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Response
import com.ilittle7.router.route
import com.ilittle7.router.*

object LoginInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        return if (User.isLogin) {
            chain.proceed(chain.requestIntent)
        } else {
            return chain.launcherWrapper.route("/login")
        }
    }
}