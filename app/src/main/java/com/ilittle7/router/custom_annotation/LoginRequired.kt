package com.ilittle7.router.custom_annotation

import com.ilittle7.router.Interceptors
import com.ilittle7.router.interceptor.LoginInterceptor
import com.ilittle7.router.interceptor.TestAInterceptor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Interceptors([LoginInterceptor::class, TestAInterceptor::class])
annotation class LoginRequired