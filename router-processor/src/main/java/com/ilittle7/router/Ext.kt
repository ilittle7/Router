package com.ilittle7.router

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

fun TypeElement.toRouterClassName() = "${qualifiedName.toString().replace('.', '_')}Router"

/**
 * Kotlin poet will explore some new [TypeMirror] APIs based on metadata
 */
@Suppress("DEPRECATION")
val TypeMirror.typeName: TypeName
    get() = asTypeName()