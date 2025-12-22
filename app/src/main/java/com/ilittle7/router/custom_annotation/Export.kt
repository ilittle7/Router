package com.ilittle7.router.custom_annotation

import com.ilittle7.router.Interceptors
import com.ilittle7.router.interceptor.ExportInterceptor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Interceptors([ExportInterceptor::class])
annotation class Export