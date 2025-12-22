package com.ilittle7.router.factory

import com.ilittle7.router.interceptor.ToastInterceptor
import com.ilittle7.router.Interceptor
import com.ilittle7.router.InterceptorFactory

class TestObjFactory : InterceptorFactory {
    override fun get(): Interceptor {
        return ToastInterceptor
    }
}