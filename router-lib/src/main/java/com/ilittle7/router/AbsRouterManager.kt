package com.ilittle7.router

import android.content.Intent
import android.net.Uri
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
@CompileSensitive
abstract class AbsRouterManager(
    val globalInterceptorList: List<Pair<Int, InterceptorFactory>>,
    val fallbackInterceptorList: List<Pair<Int, InterceptorFactory>>,
    val sortedRouterMap: SortedMap<String, out AbsRouter>,
    val routerPathTree: RouterTree,
) : IRouterCollection {
    open fun getPathParamKey(): String = throw NotImplementedError()
    open fun getPostPathSegmentKey(): String = throw NotImplementedError()
    open fun getActivityOptionKey(): String = throw NotImplementedError()
    open fun getDialogFragmentIntentKey(): String = throw NotImplementedError()

    internal operator fun get(intent: Intent): AbsRouter? {
        val className = intent.component?.className
        return if (className != null) {
            sortedRouterMap[className]
        } else {
            val matchInfo = MatchInfo()
            routerPathTree[intent.data ?: return null, matchInfo]?.also {
                handlePathParam(intent, matchInfo)
            }
        }
    }

    private fun handlePathParam(intent: Intent, matchInfo: MatchInfo) {
        if (matchInfo.wildcardMap.isEmpty()) {
            intent.removeExtra(pathParamKey)
        } else {
            intent.putExtra(pathParamKey, matchInfo.wildcardMap)
        }

        if (matchInfo.postfixSegments.isEmpty()) {
            intent.removeExtra(postPathSegmentKey)
        } else {
            intent.putExtra(postPathSegmentKey, matchInfo.postfixSegments)
        }
    }

    override fun contains(path: String): Boolean = Uri.parse(path) in this

    override fun contains(uri: Uri): Boolean = uri in routerPathTree

    override fun contains(intent: Intent): Boolean = with(intent) {
        component?.className?.let { return it in sortedRouterMap }

        return (data ?: return false) in routerPathTree
    }
}