package com.ilittle7.router

import com.squareup.kotlinpoet.*

inline fun FileSpec.Builder.kObject(
    objectName: String,
    block: TypeSpec.Builder.() -> Unit
) = addType(TypeSpec.objectBuilder(objectName).apply(block).build())

inline fun FileSpec.Builder.kTopProperty(
    propertyName: String,
    propertyType: ClassName,
    block: PropertySpec.Builder.() -> Unit
) = addProperty(PropertySpec.builder(propertyName, propertyType).apply(block).build())

inline fun TypeSpec.Builder.kProperty(
    propertyName: String,
    propertyType: ClassName,
    block: PropertySpec.Builder.() -> Unit
) = addProperty(PropertySpec.builder(propertyName, propertyType).apply(block).build())

inline fun kCode(
    block: CodeBlock.Builder.() -> Unit
) = CodeBlock.builder().apply(block).build()