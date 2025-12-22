@file:JvmName("Routers")

package com.ilittle7.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

// region route functions
fun Activity.route(path: String) = ActivityLauncher(this).route(path)

fun Activity.route(uri: Uri) = ActivityLauncher(this).route(uri)

fun Activity.route(intent: Intent) = ActivityLauncher(this).route(intent)

fun Activity.routeForResult(path: String, requestCode: Int) =
    ActivityLauncher(this).route(path, true, requestCode)

fun Activity.routeForResult(uri: Uri, requestCode: Int) =
    ActivityLauncher(this).route(uri, true, requestCode)

fun Activity.routeForResult(intent: Intent, requestCode: Int) =
    ActivityLauncher(this).route(intent, true, requestCode)

fun Fragment.route(path: String): Response {
    if (this.activity == null) {
        return Response(false, null, FragmentHasNoActivityException(), null)
    }
    return FragmentLauncher(this).route(path)
}

fun Fragment.route(uri: Uri): Response {
    if (this.activity == null) {
        return Response(false, null, FragmentHasNoActivityException(), null)
    }
    return FragmentLauncher(this).route(uri)
}

fun Fragment.route(intent: Intent): Response {
    if (this.activity == null) {
        return Response(false, null, FragmentHasNoActivityException(), null)
    }
    return FragmentLauncher(this).route(intent)
}

fun Fragment.routeForResult(path: String, requestCode: Int): Response {
    if (this.activity == null) {
        return Response(false, null, FragmentHasNoActivityException(), null)
    }
    return FragmentLauncher(this).route(path, true, requestCode)
}

fun Fragment.routeForResult(uri: Uri, requestCode: Int): Response {
    if (this.activity == null) {
        return Response(false, null, FragmentHasNoActivityException(), null)
    }
    return FragmentLauncher(this).route(uri, true, requestCode)
}

fun Fragment.routeForResult(intent: Intent, requestCode: Int): Response {
    if (this.activity == null) {
        return Response(false, null, FragmentHasNoActivityException(), null)
    }
    return FragmentLauncher(this).route(intent, true, requestCode)
}

fun Context.route(path: String) = ContextLauncher(this).route(path)

fun Context.route(uri: Uri) = ContextLauncher(this).route(uri)

fun Context.route(intent: Intent) = ContextLauncher(this).route(intent)
// endregion

fun LauncherWrapper.route(
    path: String,
    isForResult: Boolean = false,
    requestCode: Int? = null
) = route(Uri.parse(path), isForResult, requestCode)

fun LauncherWrapper.route(
    uri: Uri,
    isForResult: Boolean = false,
    requestCode: Int? = null
) = route(Intent().apply {
    data = uri
    action = Intent.ACTION_VIEW
}, isForResult, requestCode)

fun LauncherWrapper.route(
    intent: Intent,
    isForResult: Boolean = false,
    requestCode: Int? = null
): Response {
    val iRouter = routerManager[intent] ?: FallbackRouter

    return if (isForResult) {
        checkNotNull(requestCode) { "Request code is not set" }
        iRouter.startForResult(this, requestCode, intent)
    } else {
        iRouter.start(this, intent)
    }
}

// router path: /a/**, real path: /a/b/c, return: ["b", "c"]
@Suppress("UNCHECKED_CAST")
val Intent.postSegments: List<String>?
    get() = getSerializableExtra(postPathSegmentKey) as? List<String>

val Intent.postfix: String?
    get() = postSegments?.joinToString("/")

var Intent.activityOptions: Bundle?
    get() = getBundleExtra(activityOptionsKey)
    set(value) {
        if (value == null) {
            removeExtra(activityOptionsKey)
        } else {
            putExtra(activityOptionsKey, value)
        }
    }

/**
 * Get the launcher intent.
 *
 * It can return null if the dialog is not launched by router
 *
 * Note: Don't use this property in constructor
 */
val DialogFragment.routerIntent: Intent? get() = arguments?.getParcelable(dialogFragmentIntentKey)

/**
 * Get path param from real intent
 */
inline operator fun <reified T> Intent.get(key: String): T? {
    return when {
        T::class == Int::class -> routerPathParamInt(key)
        T::class == Float::class -> routerPathParamFloat(key)
        else -> routerPathParamString(key)
    } as T?
}

@Suppress("UNCHECKED_CAST")
fun Intent.routerPathParamInt(key: String): Int? {
    val map = getSerializableExtra(pathParamKey) as? Map<String, String> ?: return null
    return try {
        map[key]?.toInt()
    } catch (e: NumberFormatException) {
        Log.e(TAG, "Router param can't format to Int", e)
        null
    }
}

@Suppress("UNCHECKED_CAST")
fun Intent.routerPathParamFloat(key: String): Float? {
    val map = getSerializableExtra(pathParamKey) as? Map<String, String> ?: return null
    return try {
        map[key]?.toFloat()
    } catch (e: NumberFormatException) {
        Log.e(TAG, "Router param can't format to Float", e)
        null
    }
}

@Suppress("UNCHECKED_CAST")
fun Intent.routerPathParamString(key: String): String? {
    val map = getSerializableExtra(pathParamKey) as? Map<String, String> ?: return null
    return map[key]
}