package com.ilittle7.router

import android.net.Uri
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@CompileSensitive
sealed class RouterTree {
    companion object {
        // Used by gradle transform
        @Suppress("unused")
        @JvmStatic
        fun emptyTree(): RouterTree = TreeNode()

        operator fun invoke(block: RouterTree.() -> Unit): RouterTree = TreeNode().also(block)
    }

    abstract operator fun String.invoke(
        router: AbsRouter? = null,
        pathParamName: List<String>? = null,
        block: (RouterTree.() -> Unit)? = null
    )

    /**
     * Add a wildcard tree node
     */
    abstract fun wildcard(
        router: AbsRouter? = null,
        pathParamName: List<String>? = null,
        block: (RouterTree.() -> Unit)? = null,
    )

    /**
     * Add a default base uri tree node
     */
    abstract fun defaultBaseUri(
        router: AbsRouter? = null,
        block: (RouterTree.() -> Unit)? = null,
    )

    /**
     * Add a wildcard tree node which represents the any length path segments
     */
    abstract fun postfix(
        router: AbsRouter? = null,
        pathParamName: List<String>? = null,
        block: (RouterTree.() -> Unit)? = null,
    )

    /**
     * Add other RouterTree's value to this tree
     */
    abstract fun merge(other: RouterTree)

    /**
     * Search a router in this tree
     */
    internal abstract operator fun get(uri: Uri, matchInfo: MatchInfo): AbsRouter?

    internal abstract operator fun contains(uri: Uri): Boolean
}

private open class TreeNode(
    var router: AbsRouter? = null,
    val pathParamName: List<String>? = null,
) : RouterTree() {
    // { segment/wildcard/postfix/defaultBaseUri -> TreeNode }
    private val map = TreeMap<Any, TreeNode>() { o1, o2 ->
        return@TreeMap when {
            o1 == o2 -> 0
            o1 !is String && o2 !is String -> o1.hashCode().compareTo(o2.hashCode())
            o1 !is String -> 1
            o2 !is String -> -1
            else -> o1.compareTo(o2)
        }
    }

    override operator fun String.invoke(
        router: AbsRouter?,
        pathParamName: List<String>?,
        block: (RouterTree.() -> Unit)?
    ) {
        val treeNode = TreeNode(router, pathParamName)
        // just add, it will not repeat because of compile check
        map[this] = treeNode
        block?.let { treeNode.it() }
    }

    override fun wildcard(
        router: AbsRouter?,
        pathParamName: List<String>?,
        block: (RouterTree.() -> Unit)?
    ) {
        val wcTreeNode = Wildcard(router, pathParamName)
        map[Wildcard] = wcTreeNode
        block?.let { wcTreeNode.it() }
    }

    override fun defaultBaseUri(
        router: AbsRouter?,
        block: (RouterTree.() -> Unit)?
    ) {
        val baseUriTreeNode = DefaultBaseUri(router)
        map[DefaultBaseUri] = baseUriTreeNode
        block?.let { baseUriTreeNode.it() }
    }

    override fun postfix(
        router: AbsRouter?,
        pathParamName: List<String>?,
        block: (RouterTree.() -> Unit)?
    ) {
        val postfix = Postfix(router, pathParamName)
        map[Postfix] = postfix
        block?.let { postfix.it() }
    }

    override fun merge(other: RouterTree) {
        val node = other as TreeNode
        node.map.forEach { (segment, otherTreeNode) ->
            if (segment in map) {
                val currentTreeNode = map[segment]!!
                val currentRouter = currentTreeNode.router
                val otherRouter = otherTreeNode.router
                if (currentRouter != null && otherRouter != null) throw IllegalStateException("Router path is repeated in different modules")
                if (currentRouter == null && otherRouter != null) {
                    currentTreeNode.router = otherRouter
                }
                currentTreeNode.merge(otherTreeNode)
            } else {
                map[segment] = otherTreeNode
            }
        }
    }

    // First handle base uri
    override operator fun get(uri: Uri, matchInfo: MatchInfo): AbsRouter? {
        val rawScheme = uri.scheme
        val rawAuthority = uri.authority

        // Intentionally use default base uri
        if ((rawScheme.isNullOrEmpty() && rawAuthority.isNullOrEmpty()) || (rawScheme == baseUri.scheme && rawAuthority == baseUri.authority)) {
            val baseUriTreeNode = map[DefaultBaseUri] ?: return null
            if (uri.isPathEmpty) return baseUriTreeNode.router
            val segments: List<String> = uri.pathSegments ?: return null
            return baseUriTreeNode.find(segments, 0, segments.size, matchInfo)
        }

        // Custom base uri is specified
        if (rawScheme.isNullOrEmpty()) {
            Log.e(TAG, "The target router uri be specified has no scheme, which is: $uri")
            return null
        }
        val customBaseUri = "$rawScheme://$rawAuthority/"
        val customBaseUriNode = map[customBaseUri] ?: return null
        if (uri.isPathEmpty) return customBaseUriNode.router
        val segments: List<String> = uri.pathSegments ?: return null
        return customBaseUriNode.find(segments, 0, segments.size, matchInfo)
    }

    // Handle path recursively
    protected fun find(
        pathSegments: List<String>,
        segmentIndex: Int,
        segmentSize: Int,
        matchInfo: MatchInfo,
    ): AbsRouter? {
        if (segmentIndex >= segmentSize) throw IllegalArgumentException("router path is empty")
        if (segmentIndex == segmentSize - 1) { // leaf node
            val segment = pathSegments[segmentIndex]

            val segmentNode = map[segment]
            if (segmentNode != null) {
                val router = segmentNode.router
                if (router != null) {
                    segmentNode.pathParamName?.forEachIndexed { index, name ->
                        matchInfo.wildcardMap[name] = matchInfo.pathParamValues[index]
                    }
                    return router
                }
            }

            val wildcardNode = map[Wildcard]
            if (wildcardNode != null) {
                val router = wildcardNode.router
                if (router != null) {
                    matchInfo.pathParamValues += segment
                    wildcardNode.pathParamName?.forEachIndexed { index, name ->
                        matchInfo.wildcardMap[name] = matchInfo.pathParamValues[index]
                    }
                    return router
                }
            }

            val postfixNode = map[Postfix]
            if (postfixNode != null) {
                val router = postfixNode.router
                if (router != null) {
                    matchInfo.postfixSegments.addAll(
                        pathSegments.subList(segmentIndex, segmentSize)
                    )
                    postfixNode.pathParamName?.forEachIndexed { index, name ->
                        matchInfo.wildcardMap[name] = matchInfo.pathParamValues[index]
                    }
                    return router
                }
            }
            return null
        }

        val segment = pathSegments[segmentIndex]

        val segmentNode = map[segment]
        if (segmentNode != null) {
            val segmentRouter =
                segmentNode.find(pathSegments, segmentIndex + 1, segmentSize, matchInfo)
            if (segmentRouter != null) {
                return segmentRouter
            }
        }

        val wildcardNode = map[Wildcard]
        if (wildcardNode != null) {
            matchInfo.pathParamValues += segment
            val wildcardRouter =
                wildcardNode.find(pathSegments, segmentIndex + 1, segmentSize, matchInfo)
            if (wildcardRouter != null) {
                return wildcardRouter
            }
            matchInfo.pathParamValues.removeLast()
        }

        val postfixNode = map[Postfix]
        if (postfixNode != null) {
            val postfixRouter = map[Postfix]?.router
            if (postfixRouter != null) {
                matchInfo.postfixSegments.addAll(
                    pathSegments.subList(segmentIndex, pathSegments.size)
                )
                postfixNode.pathParamName?.forEachIndexed { index, name ->
                    matchInfo.wildcardMap[name] = matchInfo.pathParamValues[index]
                }
                return postfixRouter
            }
        }

        return null
    }

    override fun contains(uri: Uri): Boolean {
        val rawScheme = uri.scheme
        val rawAuthority = uri.authority

        if ((rawScheme.isNullOrEmpty() && rawAuthority.isNullOrEmpty()) || (rawScheme == baseUri.scheme && rawAuthority == baseUri.authority)) {
            val baseUriTreeNode = map[DefaultBaseUri] ?: return false
            if (uri.isPathEmpty) return baseUriTreeNode.router != null
            val segments: List<String> = uri.pathSegments ?: return false
            return baseUriTreeNode.contains(segments, 0, segments.size)
        }

        if (rawScheme.isNullOrEmpty()) return false

        val customBaseUri = "$rawScheme://$rawAuthority/"
        val customBaseUriNode = map[customBaseUri] ?: return false
        if (uri.isPathEmpty) return customBaseUriNode.router != null
        val segments: List<String> = uri.pathSegments ?: return false
        return customBaseUriNode.contains(segments, 0, segments.size)
    }

    private fun contains(
        pathSegments: List<String>,
        segmentIndex: Int,
        segmentSize: Int,
    ): Boolean {
        if (segmentIndex == segmentSize - 1) { // leaf node
            return (map[pathSegments[segmentIndex]]?.router
                ?: map[Wildcard]?.router
                ?: map[Postfix]?.router) != null
        }

        val segment = pathSegments[segmentIndex]
        return map[segment]?.contains(pathSegments, segmentIndex + 1, segmentSize) == true ||
                map[Wildcard]?.contains(pathSegments, segmentIndex + 1, segmentSize) == true ||
                map[Postfix]?.router != null
    }

    private inline val Uri.isPathEmpty: Boolean
        get() {
            val path = path
            return path.isNullOrEmpty() || path == "/"
        }
}

private class Wildcard(
    router: AbsRouter?,
    pathParamName: List<String>?
) : TreeNode(router, pathParamName) {
    companion object Key
}

private class DefaultBaseUri(router: AbsRouter?) : TreeNode(router) {
    companion object Key
}

private class Postfix(router: AbsRouter?, pathParamName: List<String>?) :
    TreeNode(router, pathParamName) {
    companion object Key
}

internal data class MatchInfo(
    val pathParamValues: ArrayList<String> = ArrayList(),
    val wildcardMap: HashMap<String, String> = HashMap(),
    val postfixSegments: ArrayList<String> = ArrayList(),
)