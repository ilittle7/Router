package com.ilittle7.router

import kotlin.reflect.KClass

/**
 * 修改为 BINARY，确保在 KSP 增量编译轮次中信息不丢失
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Router(
    val path: Array<String> = [],
    val interceptorArr: Array<KClass<*>> = []
)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Interceptors(val interceptorArr: Array<KClass<*>>)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Priority(val priority: Int)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class GlobalInterceptor

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class FallbackInterceptor

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class RouterBaseUri(val baseUri: String)

@Deprecated("Internal use only", level = DeprecationLevel.HIDDEN)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class GeneratedMetadata(
    val type: String,
    val targetClass: KClass<*>
)
