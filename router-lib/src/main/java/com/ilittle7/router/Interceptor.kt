package com.ilittle7.router

@CompileSensitive
interface Interceptor {
    fun onIntercept(chain: Chain): Response
}