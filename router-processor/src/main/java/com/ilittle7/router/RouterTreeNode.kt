package com.ilittle7.router

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.CodeBlock
import java.net.URI
import java.util.*

open class RouterTreeNode {
    companion object {
        private val paramRegex = """^\{([^{}]+)}$""".toRegex()
        private const val POSTFIX_STR = "**"
        
        private object WildcardKey
        private object DefaultBaseUriKey
        private object PostfixKey
    }

    private var routerClass: KSClassDeclaration? = null
    private var paramNameList: MutableList<String> = mutableListOf()
    private val map = TreeMap<Any, RouterTreeNode> { o1, o2 ->
        when {
            o1 == o2 -> 0
            o1 !is String && o2 !is String -> o1.hashCode().compareTo(o2.hashCode())
            o1 !is String -> 1
            o2 !is String -> -1
            else -> o1.compareTo(o2)
        }
    }

    fun parse(rawUri: String, clazz: KSClassDeclaration) {
        val splitRawUri: List<String> = rawUri.split("/")
        val replacedUri: String = splitRawUri.joinToString(separator = "/") { segment ->
            segment.replace(paramRegex) { it.groupValues[1] }
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

        val uriPath = uri.path
        if (uriPath.isNullOrEmpty() || uriPath == "/") {
            if (baseUriNode.routerClass == null) {
                baseUriNode.routerClass = clazz
            }
            return
        }

        // ["scheme", "", "host:8080", "user", "register", "{id}"] -> "/user/register/{id}"
        // ["", "adv", "{id}"] -> "/adv/{id}"
        val actualUriPath = splitRawUri.subList(beginIndex, splitRawUri.size)
            .joinToString(separator = "/", prefix = "/")

        // "/user/register/{id}" -> ["", "user", "register", "{id}"]
        val pathSegments = actualUriPath.split("/")
        baseUriNode.addNode(pathSegments.subList(1, pathSegments.size), clazz, mutableListOf())
    }

    private fun addNode(pathSegments: List<String>, clazz: KSClassDeclaration, tmpParamNameList: MutableList<String>) {
        if (pathSegments.isEmpty()) return
        val segment = pathSegments[0]
        val leafNode: RouterTreeNode = when {
            segment.matches(paramRegex) -> {
                tmpParamNameList += segment.replace(paramRegex) { it.groupValues[1] }
                optWildcardNode()
            }
            segment == POSTFIX_STR -> optPostSegmentsNode()
            else -> optSegmentNode(segment)
        }

        if (pathSegments.size == 1) {
            if (leafNode.routerClass == null) {
                leafNode.paramNameList = tmpParamNameList
                leafNode.routerClass = clazz
            }
        } else {
            check(segment != POSTFIX_STR) { "The \"$POSTFIX_STR\" can't appear in the middle of a path" }
            leafNode.addNode(pathSegments.subList(1, pathSegments.size), clazz, tmpParamNameList)
        }
    }

    fun generateCode(builder: CodeBlock.Builder) {
        for (entry in map.entries) {
            val segment = entry.key
            val node = entry.value
            val routerName = node.routerClass?.simpleName?.asString()?.let { "${it}Router" }
            
            when (segment) {
                DefaultBaseUriKey -> builder.add("defaultBaseUri(")
                WildcardKey -> builder.add("wildcard(")
                PostfixKey -> builder.add("postfix(")
                else -> builder.add("%S(", segment)
            }
            
            if (routerName != null) {
                builder.add("router = %L", routerName)
                if (node.paramNameList.isNotEmpty()) {
                    builder.add(", pathParamName = listOf(")
                    node.paramNameList.forEachIndexed { index, paramName ->
                        builder.add("%S", paramName)
                        if (index < node.paramNameList.size - 1) builder.add(", ")
                    }
                    builder.add(")")
                }
            }
            builder.add("){")
            node.generateCode(builder)
            builder.add("}\n")
        }
    }

    private fun optSegmentNode(segment: String) = map[segment] ?: RouterTreeNode().also { map[segment] = it }
    private fun optWildcardNode() = map[WildcardKey] ?: RouterTreeNode().also { map[WildcardKey] = it }
    private fun optPostSegmentsNode() = map[PostfixKey] ?: RouterTreeNode().also { map[PostfixKey] = it }
    private fun optDefaultBaseUriNode() = map[DefaultBaseUriKey] ?: RouterTreeNode().also { map[DefaultBaseUriKey] = it }
}
