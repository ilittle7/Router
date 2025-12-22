package com.ilittle7.router

import kotlin.reflect.KClass

/**
 * Specify the class is a router.
 *
 * @property path the uri string of this router.
 * @property interceptorArr the interceptors applied to this router.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Router(
    val path: Array<String> = [],
    val interceptorArr: Array<KClass<*>> = []
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Interceptors(val interceptorArr: Array<KClass<*>>)

/**
 * Specify the priority of the Interceptor or InterceptorFactory.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Priority(val priority: Int)

/**
 * Specify the Interceptor is a global interceptor. it will be run on all router
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GlobalInterceptor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class FallbackInterceptor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class RouterBaseUri(val baseUri: String)