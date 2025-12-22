package com.ilittle7.router

import com.squareup.kotlinpoet.CodeBlock
import java.net.URI
import java.util.*
import javax.lang.model.element.TypeElement

open class RouterTreeNode {
    companion object {
        private val paramRegex = """^\{([^{}]+)}$""".toRegex()
    }

    private var routerElement: TypeElement? = null
    private var paramNameList: MutableList<String> = mutableListOf()
    private val map = TreeMap<Any, RouterTreeNode>() { o1, o2 ->
        return@TreeMap when {
            o1 == o2 -> 0
            o1 !is String && o2 !is String -> o1.hashCode().compareTo(o2.hashCode())
            o1 !is String -> 1
            o2 !is String -> -1
            else -> o1.compareTo(o2)
        }
    }

    fun parse(rawUri: String, element: TypeElement) {
        val splitRawUri: List<String> = rawUri.split("/")
        val replacedUri: String = splitRawUri.joinToString(separator = "/") { segment ->
            segment.replace(paramRegex) {
                val groupValue = it.groupValues[1]
                groupValue
            }
        }

        val uri: URI = URI.create(replacedUri)

        val beginIndex: Int
        val baseUriNode = if (uri.scheme == null && uri.authority == null) {
            beginIndex = 1
            optDefaultBaseUriNode()
        } else {
            beginIndex = 3
            check(uri.scheme != null) { "Custom router path $uri has no scheme" }
            val actualBaseUri = "${uri.scheme.orEmpty()}://${uri.authority.orEmpty()}/"
            map[actualBaseUri] ?: RouterTreeNode().also { map[actualBaseUri] = it }
        }

        val uriPath: String? = uri.path

        if (uriPath.isNullOrEmpty() || uriPath == "/") {
            if (baseUriNode.routerElement == null) {
                baseUriNode.routerElement = element
            }
            return
        }

        // ["scheme", "", "host:8080", "user", "register", "{id}"] -> "/user/register/{id}"
        // ["", "adv", "{id}"] -> "/adv/{id}"
        val actualUriPath = splitRawUri.subList(beginIndex, splitRawUri.size)
            .joinToString(separator = "/", prefix = "/")

        // "/user/register/{id}" -> ["", "user", "register", "{id}"]
        val pathSegments = actualUriPath.split("/")

        baseUriNode.addNode(pathSegments.subList(1, pathSegments.size), element, mutableListOf())
    }

    private fun addNode(
        pathSegments: List<String>,
        element: TypeElement,
        tmpParamNameList: MutableList<String>,
    ) {
        if (pathSegments.isEmpty()) throw IllegalStateException("can not build the router path tree with an empty path.")
        if (pathSegments.size == 1) {
            val segment = pathSegments[0]
            val leafNode: RouterTreeNode = when {
                segment.matches(paramRegex) -> {
                    tmpParamNameList += segment.replace(paramRegex) { it.groupValues[1] }
                    optWildcardNode()
                }
                segment == POSTFIX -> optPostSegmentsNode()
                else -> optSegmentNode(segment)
            }

            if (leafNode.routerElement == null) {
                leafNode.paramNameList = tmpParamNameList
                leafNode.routerElement = element
            } else {
                throw IllegalStateException("Ambiguous router path set，class name is: ${leafNode.routerElement!!.qualifiedName} and ${element.qualifiedName}")
            }
        } else {
            val segment = pathSegments[0]
            check(segment != POSTFIX) { """The "$POSTFIX" can't appear in medium of the router path""" }
            if (segment.matches(paramRegex)) {
                tmpParamNameList += segment.replace(paramRegex) { it.groupValues[1] }
                optWildcardNode()
            } else {
                optSegmentNode(segment)
            }.addNode(pathSegments.subList(1, pathSegments.size), element, tmpParamNameList)
        }
    }

    fun generateCode(builder: CodeBlock.Builder) {
        builder.apply {
            map.forEach { (segment, node) ->
                val routerName = node.routerElement?.toRouterClassName()
                when (segment) {
                    DefaultBaseUri -> addStatement("defaultBaseUri(")
                    Wildcard -> addStatement("wildcard(")
                    Postfix -> addStatement("postfix(")
                    else -> addStatement("%S(", segment)
                }
                if (routerName != null) {
                    addStatement("router = %L,", routerName)
                    val paramNameList = node.paramNameList
                    if (paramNameList.isNotEmpty()) {
                        addStatement("pathParamName = listOf(")
                        paramNameList.forEach { paramName ->
                            addStatement("%S,", paramName)
                        }
                        addStatement(")")
                    }
                }
                addStatement("){")
                node.generateCode(this)
                addStatement("}")
            }
        }
    }

    private fun optSegmentNode(segment: String): RouterTreeNode {
        return map[segment] ?: RouterTreeNode().also { map[segment] = it }
    }

    private fun optWildcardNode(): RouterTreeNode {
        return map[Wildcard] ?: Wildcard().also { map[Wildcard] = it }
    }

    private fun optPostSegmentsNode(): RouterTreeNode {
        return map[Postfix] ?: Postfix().also { map[Postfix] = it }
    }

    private fun optDefaultBaseUriNode(): RouterTreeNode {
        return map[DefaultBaseUri] ?: DefaultBaseUri().also { map[DefaultBaseUri] = it }
    }

    override fun toString(): String {
        return "RouterTreeNode(routerElement=${routerElement?.simpleName}, map=$map)"
    }
}

private class Wildcard : RouterTreeNode() {
    companion object Key
}

private class DefaultBaseUri : RouterTreeNode() {
    companion object Key
}

private const val POSTFIX = "**"

private class Postfix : RouterTreeNode() {
    companion object Key
}