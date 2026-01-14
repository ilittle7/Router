package com.ilittle7.router

import android.net.Uri
import android.util.Log
import com.ilittle7.router.transform.ActualRouterManager

internal const val TAG = "Router"

@Suppress("CAST_NEVER_SUCCEEDS")
internal val routerManager = try {
    ActualRouterManager.create() as AbsRouterManager
} catch (e: Throwable) {
    Log.w(TAG, "init error, maybe the annotation is not be used: ", e)
    object : AbsRouterManager(emptyList(), emptyList(), sortedMapOf(), RouterTree {}) {}
}

internal val baseUri: Uri by lazy {
    val finalBaseUri = ActualRouterManager.getActualBaseUri()
    finalBaseUri?.let { Uri.parse(it) } ?: run {
        Log.w(
            TAG,
            "No baseUri be found, maybe you should add a @RouterBaseUri annotation on any class."
        )
        Uri.parse("")
    }
}

internal val pathParamKey: String by lazy {
    try {
        routerManager.getPathParamKey()
    } catch (e: Throwable) {
        throw IllegalStateException("No pathParamKey generated", e)
    }
}

internal val postPathSegmentKey: String by lazy {
    try {
        routerManager.getPostPathSegmentKey()
    } catch (e: Throwable) {
        throw IllegalStateException("No postPathSegmentKey generated", e)
    }
}

internal val activityOptionsKey: String by lazy {
    try {
        routerManager.getActivityOptionKey()
    } catch (e: Throwable) {
        throw IllegalStateException("No activityOptionsKey generated", e)
    }
}

internal val dialogFragmentIntentKey: String by lazy {
    try {
        routerManager.getDialogFragmentIntentKey()
    } catch (e: Throwable) {
        throw IllegalStateException("No activityOptionsKey generated", e)
    }
}

/**
 * Specify the class name, function or property of the target class is used
 * by apt or gradle when the code is compiling.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
internal annotation class CompileSensitive
