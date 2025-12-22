package com.ilittle7.router

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import java.net.URI
import java.util.*
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class RouterProcessor : BaseProcessor() {
    companion object {
        private const val ROUTERS_OBJECTS = "RouterObjects"
    }

    override fun getSupportedAnnotationTypes() = setOf(
        GlobalInterceptor::class.qualifiedName,
        Interceptors::class.qualifiedName,
        Router::class.qualifiedName
    )

    override fun process(
        annotationTypeSet: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        if (annotationTypeSet.isEmpty()) return false
        val routerList: List<TypeElement> = roundEnv.getElementsAnnotatedWith(Router::class.java)
            .map { it as TypeElement }
        val globalItcMirrorList: List<Pair<Int, TypeMirror>> =
            roundEnv.getElementsAnnotatedWith(GlobalInterceptor::class.java)
                .map {
                    val typeElement = it as TypeElement
                    val priority = typeElement.getAnnotation(Priority::class.java)?.priority ?: 0
                    priority to typeElement.asType()
                }.sortedByDescending { it.first }

        val globalItcNameList = globalItcMirrorList.map { it.second.typeName.toString() }
        val fallbackInterceptorList: List<Pair<Int, TypeMirror>> =
            roundEnv.getElementsAnnotatedWith(FallbackInterceptor::class.java)
                .filter { // if the interceptor is a global interceptor, there is no need to add it to fallback interceptor list
                    it.asType().typeName.toString() !in globalItcNameList
                }.map {
                    val typeElement = it as TypeElement
                    val priority = typeElement.getAnnotation(Priority::class.java)?.priority ?: 0
                    priority to typeElement.asType()
                }

        val routerManagerObjectName =
            "${"RouterManager"}_${UUID.randomUUID().toString().replace('-', '_')}"
        if (routerList.isEmpty()) {
            kFile(ROUTERS_OBJECTS) {
                kObject(routerManagerObjectName) {
                    superclass(ClassNames.ABS_ROUTER_MANAGER)
                    addSuperclassConstructorParameter("setOf()")
                }
            }
            return true
        }
        val itcAntList = roundEnv.getElementsAnnotatedWith(Interceptors::class.java)
            .map { it as TypeElement }
        val baseUri: String? = roundEnv.getElementsAnnotatedWith(RouterBaseUri::class.java).apply {
            check(size < 2) { "Please write at most one @${RouterBaseUri::class.simpleName} on any java or kotlin class" }
        }.firstOrNull()?.getAnnotation(RouterBaseUri::class.java)?.baseUri
        if (baseUri != null && baseUri.isNotEmpty()) {
            val uri = URI.create(baseUri)
            check(uri.scheme != null) { "The base uri must has a scheme be specified" }
        }

        // Generate RouterObjects.kt source code file
        kFile(ROUTERS_OBJECTS) {
            // RouterManager -> sortedRouterMap
            val routerObjectNameMap = mutableMapOf<String, String>()
            val rootNode = RouterTreeNode()
            // Global property for interceptor factory
            val itcFactoryPropertyNameMap = mutableMapOf<TypeName, String>()

            // 1. generate all routers
            for (routerTypeElement in routerList) {
                checkRouterType(routerTypeElement)

                val routerPath = routerTypeElement.getAnnotation(Router::class.java).path
                // parse the path into the path tree
                routerPath.forEach { path -> rootNode.parse(path, routerTypeElement) }
                val routerObjectName = routerTypeElement.toRouterClassName()

                // generate router object
                kObject(routerObjectName) routerObj@{
                    when {
                        routerTypeElement.asType().isSubTypeOf(ClassNames.ACTIVITY) -> {
                            superclass(ClassNames.ACTIVITY_ROUTER)
                            addSuperclassConstructorParameter(
                                "%T::class", routerTypeElement.toTypeName()
                            )
                        }
                        routerTypeElement.asType().isSubTypeOf(ClassNames.ROUTER_ACTION) -> {
                            superclass(ClassNames.ACTION_ROUTER)
                            val typeName = routerTypeElement.toTypeName()
                            addSuperclassConstructorParameter(
                                if (routerTypeElement.asType().isKotlinObject) "{%T}, %T::class" else "{%T()}, %T::class",
                                typeName, typeName
                            )
                        }
                        routerTypeElement.asType().isSubTypeOf(ClassNames.DIALOG_FRAGMENT) -> {
                            superclass(ClassNames.DIALOG_FRAGMENT_ROUTER)
                            val typeName = routerTypeElement.toTypeName()
                            addSuperclassConstructorParameter(
                                "%T::class,{%T()}", typeName, typeName
                            )
                        }
                        else -> {
                            superclass(ClassNames.SERVICE_ROUTER)
                            addSuperclassConstructorParameter(
                                "%T::class", routerTypeElement.toTypeName()
                            )
                        }
                    }

                    val interceptorMirrorSet =
                        getCustomItcByRouterElement(routerTypeElement, itcAntList)
                    if (interceptorMirrorSet.isEmpty()) return@routerObj
                    checkInterceptorType(interceptorMirrorSet)

                    kInterceptorsProperty {
                        val interceptorMirrorList =
                            interceptorMirrorSet.map {
                                (it.asElement().getAnnotation(Priority::class.java)?.priority
                                    ?: 0) to it
                            }.sortedByDescending { pair -> pair.first }
                        addInterceptorList(
                            interceptorMirrorList,
                            itcFactoryPropertyNameMap,
                            this@kFile
                        )
                    }
                }

                routerObjectNameMap[routerTypeElement.qualifiedName.toString()] = routerObjectName
            }

            // 2. generate object RouterConfig, to contain baseUri field
            if (baseUri != null && baseUri.isNotEmpty())
                kObject("RouterConfig") {
                    kProperty("baseUri", ClassNames.STRING) {
                        addModifiers(KModifier.CONST)
                        initializer("%S", baseUri)
                    }
                }

            // 3. generate router manager object
            kObject(routerManagerObjectName) {
                superclass(ClassNames.ABS_ROUTER_MANAGER)
                addSuperclassConstructorParameter(kCode {
                    addInterceptorList(globalItcMirrorList, itcFactoryPropertyNameMap, this@kFile)
                })
                addSuperclassConstructorParameter(kCode {
                    addInterceptorList(
                        fallbackInterceptorList,
                        itcFactoryPropertyNameMap,
                        this@kFile
                    )
                })
                addSuperclassConstructorParameter(kCode {
                    addStatement("sortedMapOf(")
                    routerObjectNameMap.forEach { (qualifiedName, routerName) ->
                        // Local or anonymous class has no qualified name
                        addStatement("%L::class.qualifiedName!! to %L,", qualifiedName, routerName)
                    }
                    addStatement(")")
                })
                addSuperclassConstructorParameter(kCode {
                    addStatement("%T{", ClassNames.ROUTER_TREE)
                    rootNode.generateCode(builder = this)
                    addStatement("}")
                })
            }
        }

        return true
    }

    private fun FileSpec.Builder.tryToAddItcFactoryProperty(
        mirror: TypeMirror,
        itcFactoryPropertyNameMap: MutableMap<TypeName, String>
    ) {
        val interceptorTypeName = mirror.typeName as ClassName
        if (interceptorTypeName !in itcFactoryPropertyNameMap) {
            val factoryPropertyName = interceptorTypeName.canonicalName
                .replace('.', '_') + "Factory"
            kTopProperty(
                factoryPropertyName,
                ClassNames.INTERCEPTOR_FACTORY
            ) {
                addModifiers(KModifier.PRIVATE)
                initializer(
                    "%T(%T)",
                    ClassNames.DEFAULT_ITC_FACTORY,
                    interceptorTypeName
                )
            }
            itcFactoryPropertyNameMap[interceptorTypeName] =
                factoryPropertyName
        }
    }

    private fun CodeBlock.Builder.addInterceptorList(
        mirrorList: List<Pair<Int, TypeMirror>>,
        itcFactoryPropertyNameMap: MutableMap<TypeName, String>,
        kFileBuilder: FileSpec.Builder
    ) {
        addStatement("listOf(")
        mirrorList.forEach { (priority, mirror) ->
            when {
                mirror.isSubTypeOf(ClassNames.INTERCEPTOR) -> {
                    if (mirror.isKotlinObject) {
                        kFileBuilder.tryToAddItcFactoryProperty(mirror, itcFactoryPropertyNameMap)
                        addStatement(
                            "%L to %L,", priority,
                            itcFactoryPropertyNameMap[mirror.typeName]
                        )
                    } else {
                        val statementFormat =
                            if (mirror.isCompanionObject) "%L to %T(%T)" else "%L to %T(%T())"
                        addStatement(
                            "$statementFormat,",
                            priority, ClassNames.DEFAULT_ITC_FACTORY, mirror.typeName
                        )
                    }
                }
                mirror.isSubTypeOf(ClassNames.INTERCEPTOR_FACTORY) -> {
                    val statementFormat = if (mirror.isKotlinObject) "%L to %T" else "%L to %T()"
                    addStatement("$statementFormat,", priority, mirror.typeName)
                }
                else -> errorIllegalInterceptorType()
            }
        }
        addStatement(")")
    }

    private fun TypeSpec.Builder.kInterceptorsProperty(block: CodeBlock.Builder.() -> Unit) {
        addProperty(
            PropertySpec.builder(
                "interceptors", ClassNames.INTERCEPTOR_FACTORY_PAIR_LIST, KModifier.OVERRIDE
            ).apply { initializer(kCode(block)) }.build()
        )
    }

    private fun getCustomItcByRouterElement(
        routerTypeElement: TypeElement,
        interceptorAnoList: List<TypeElement>
    ): MutableSet<TypeMirror> {
        val arrayTypeList = listOf(ClassNames.INTERCEPTOR, ClassNames.INTERCEPTOR_FACTORY)
        val result: MutableSet<TypeMirror> = routerTypeElement.getAnnotation(Router::class.java)
            .getKClassArray(arrayTypeList, Router::interceptorArr).toMutableSet()

        // Add companion interceptor to the result
        val companion = routerTypeElement.asType().getCompanion
        if (companion != null && companion.isSubTypeOf(ClassNames.INTERCEPTOR)) {
            result += companion
        }

        // Add the interceptors in the custom annotation to the result
        for (itcAntElement in interceptorAnoList) {
            val antTypeName = itcAntElement.toTypeName()
            routerTypeElement.annotationMirrors
                .map { it.annotationType.typeName }
                .firstOrNull { it == antTypeName }
                ?: continue

            val itcTypeNameList = itcAntElement
                .getAnnotation(Interceptors::class.java)
                .getKClassArray(arrayTypeList, Interceptors::interceptorArr)
            check(itcTypeNameList.isNotEmpty()) { "The annotation [${Interceptors::class.simpleName}] has no [${ClassNames.INTERCEPTOR.simpleName}] be specified." }
            result += itcTypeNameList
        }
        return result
    }

    // region check function
    private fun checkRouterType(typeElement: TypeElement) {
        val typeMirror = typeElement.asType()
        check(
            typeMirror.isSubTypeOf(ClassNames.APP_COMPAT_ACTIVITY) ||
                    typeMirror.isSubTypeOf(ClassNames.ACTIVITY) ||
                    typeMirror.isSubTypeOf(ClassNames.ROUTER_ACTION) ||
                    typeMirror.isSubTypeOf(ClassNames.DIALOG_FRAGMENT) ||
                    typeMirror.isSubTypeOf(ClassNames.SERVICE)
        ) {
            "The ${typeElement.simpleName} annotated by @${Router::class.simpleName} " +
                    "is not subclass of Activity, IRouterAction or Service"
        }
    }

    private fun checkInterceptorType(itcTypeElementSet: MutableSet<TypeMirror>) {
        if (
            itcTypeElementSet.all {
                it.isSubTypeOf(ClassNames.INTERCEPTOR) ||
                        it.isSubTypeOf(ClassNames.INTERCEPTOR_FACTORY)
            }
        ) return
        errorIllegalInterceptorType()
    }

    private fun errorIllegalInterceptorType(): Nothing {
        throw IllegalStateException(
            "The interceptor array contains class which is not ${ClassNames.INTERCEPTOR.simpleName} " +
                    "or ${ClassNames.INTERCEPTOR_FACTORY.simpleName}"
        )
    }
    // endregion
}