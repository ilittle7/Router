package com.ilittle7.router

@CompileSensitive
interface InterceptorFactory {
    fun get(): Interceptor
}

@CompileSensitive
class DefaultItcFactory(val interceptor: Interceptor) :
    InterceptorFactory {
    override fun get() = interceptor
}