package com.ilittle7.router

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStreamWriter

private const val METADATA_PACKAGE = "com.ilittle7.router.internal.metadata"
private const val METADATA_ANNOTATION = "com.ilittle7.router.GeneratedMetadata"

class KspRouterProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private var routerManagerGenerated = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 1. 扫描当前脏文件的原始注解
        val routerSymbols = resolver.getSymbolsWithAnnotation(Router::class.qualifiedName!!).filterIsInstance<KSClassDeclaration>().toList()
        val globalItcSymbols = resolver.getSymbolsWithAnnotation(GlobalInterceptor::class.qualifiedName!!).filterIsInstance<KSClassDeclaration>().toList()
        val fallbackItcSymbols = resolver.getSymbolsWithAnnotation(FallbackInterceptor::class.qualifiedName!!).filterIsInstance<KSClassDeclaration>().toList()
        val baseUriSymbols = resolver.getSymbolsWithAnnotation(RouterBaseUri::class.qualifiedName!!).filterIsInstance<KSClassDeclaration>().toList()

        // 2. 如果本轮有任何新符号，生成 Metadata 类并返回（由 KSP 开启下一轮）
        val hasNewRawSymbols = routerSymbols.isNotEmpty() || globalItcSymbols.isNotEmpty() || fallbackItcSymbols.isNotEmpty()
        if (hasNewRawSymbols) {
            routerSymbols.forEach { generateMetadata(it, "ROUTER") }
            globalItcSymbols.forEach { generateMetadata(it, "GLOBAL_ITC") }
            fallbackItcSymbols.forEach { generateMetadata(it, "FALLBACK_ITC") }
        }

        // 3. 处理 BaseUri (Isolating 逻辑)
        val baseUriClass = baseUriSymbols.firstOrNull()
        if (baseUriClass != null) {
            val baseUriValue = baseUriClass.getAnnotationsByType(RouterBaseUri::class).firstOrNull()?.baseUri
            if (baseUriValue != null) {
                generateRouterConfig(baseUriValue, baseUriClass)
            }
        }

        // 4. 核心聚合：只有在所有 Metadata 都已就绪（本轮没有产生新 Metadata）时才生成 RouterManager
        if (!routerManagerGenerated) {
            val allMetadata = resolver.getSymbolsWithAnnotation(METADATA_ANNOTATION)
                .filterIsInstance<KSClassDeclaration>()
                .toList()
            
            // 检查本轮是否产生了新的 Metadata 文件，如果有，必须等到下一轮，因为新文件可能尚未全量索引
            val hasJustGeneratedMetadata = hasNewRawSymbols 
            
            if (allMetadata.isNotEmpty() && !hasJustGeneratedMetadata) {
                logger.info("Router: Aggregating ${allMetadata.size} metadata files into RouterManager")
                generateFinalRouterManager(allMetadata)
                routerManagerGenerated = true
            }
        }

        return emptyList()
    }

    private fun generateMetadata(target: KSClassDeclaration, type: String) {
        val uniqueName = target.qualifiedName?.asString()?.replace(".", "_") ?: target.simpleName.asString()
        val className = "__${uniqueName}Metadata"
        
        val fileSpec = FileSpec.builder(METADATA_PACKAGE, className)
            .addAnnotation(AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "DEPRECATION_ERROR")
                .build())
            .addType(
                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.INTERNAL)
                    .addAnnotation(AnnotationSpec.builder(ClassName("kotlin", "Deprecated"))
                        .addMember("message = %S", "Internal use only")
                        .addMember("level = %T.HIDDEN", DeprecationLevel::class)
                        .build())
                    .addAnnotation(AnnotationSpec.builder(ClassName("com.ilittle7.router", "GeneratedMetadata"))
                        .addMember("type = %S", type)
                        .addMember("targetClass = %T::class", target.toClassName())
                        .build())
                    .build()
            )
            .build()
        fileSpec.writeTo(codeGenerator, Dependencies(false, target.containingFile!!))
    }

    private fun generateFinalRouterManager(metadataClasses: List<KSClassDeclaration>) {
        val routerDecls = mutableListOf<KSClassDeclaration>()
        val globalDecls = mutableListOf<KSClassDeclaration>()
        val fallbackDecls = mutableListOf<KSClassDeclaration>()

        metadataClasses.forEach { metadata ->
            val anno = metadata.annotations.firstOrNull { it.shortName.asString() == "GeneratedMetadata" } ?: return@forEach
            val type = anno.arguments.find { it.name?.asString() == "type" }?.value as String
            val targetClassType = anno.arguments.find { it.name?.asString() == "targetClass" }?.value as KSType
            val targetDecl = targetClassType.declaration as KSClassDeclaration

            when (type) {
                "ROUTER" -> routerDecls.add(targetDecl)
                "GLOBAL_ITC" -> globalDecls.add(targetDecl)
                "FALLBACK_ITC" -> fallbackDecls.add(targetDecl)
            }
        }

        val packageName = "com.ilittle7.router.gen"
        val moduleName = options["moduleName"] ?: (routerDecls.firstOrNull() ?: globalDecls.firstOrNull() ?: fallbackDecls.firstOrNull())?.packageName?.asString()?.replace(".", "_") ?: "Default"
        val routerManagerName = "RouterManager_$moduleName"
        val fileSpec = FileSpec.builder(packageName, routerManagerName)
            .addAnnotation(AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "DEPRECATION_ERROR")
                .build())

        val rootNode = RouterTreeNode()
        val routerObjectNameMap = mutableMapOf<TypeName, String>()
        val itcFactoryMap = mutableMapOf<TypeName, String>()

        routerDecls.distinctBy { it.qualifiedName?.asString() }.forEach { routerClass ->
            val routerAnnotation = routerClass.annotations.first { it.shortName.asString() == "Router" }
            val paths = routerAnnotation.arguments.find { it.name?.asString() == "path" }?.value as? List<String> ?: emptyList()
            paths.forEach { path -> rootNode.parse(path, routerClass) }
            
            val routerObjectName = "${routerClass.simpleName.asString()}Router"
            routerObjectNameMap[routerClass.toClassName()] = routerObjectName
            
            val routerObjectSpec = TypeSpec.objectBuilder(routerObjectName)
            when {
                routerClass.isSubclassOf(ClassNames.ACTIVITY.canonicalName) -> {
                    routerObjectSpec.superclass(ClassNames.ACTIVITY_ROUTER)
                    routerObjectSpec.addSuperclassConstructorParameter("%T::class", routerClass.toClassName())
                }
                routerClass.isSubclassOf(ClassNames.ROUTER_ACTION.canonicalName) -> {
                    routerObjectSpec.superclass(ClassNames.ACTION_ROUTER)
                    val lambda = if (routerClass.classKind == ClassKind.OBJECT) "{ %T }" else "{ %T() }"
                    routerObjectSpec.addSuperclassConstructorParameter(lambda, routerClass.toClassName())
                    routerObjectSpec.addSuperclassConstructorParameter("%T::class", routerClass.toClassName())
                }
                routerClass.isSubclassOf(ClassNames.DIALOG_FRAGMENT.canonicalName) -> {
                    routerObjectSpec.superclass(ClassNames.DIALOG_FRAGMENT_ROUTER)
                    routerObjectSpec.addSuperclassConstructorParameter("%T::class", routerClass.toClassName())
                    val lambda = if (routerClass.classKind == ClassKind.OBJECT) "{ %T }" else "{ %T() }"
                    routerObjectSpec.addSuperclassConstructorParameter(lambda, routerClass.toClassName())
                }
                else -> {
                    routerObjectSpec.superclass(ClassNames.SERVICE_ROUTER)
                    routerObjectSpec.addSuperclassConstructorParameter("%T::class", routerClass.toClassName())
                }
            }
            
            val customInterceptors = getCustomInterceptors(routerClass)
            if (customInterceptors.isNotEmpty()) {
                routerObjectSpec.addProperty(
                    PropertySpec.builder("interceptors", ClassNames.INTERCEPTOR_FACTORY_PAIR_LIST, KModifier.OVERRIDE)
                        .initializer(CodeBlock.builder().addInterceptorList(customInterceptors, itcFactoryMap, fileSpec).build())
                        .build()
                )
            }
            fileSpec.addType(routerObjectSpec.build())
        }

        val managerSpec = TypeSpec.classBuilder(routerManagerName)
            .superclass(ClassNames.ABS_ROUTER_MANAGER)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(ClassName("kotlin", "Deprecated"))
                .addMember("message = %S", "Internal use only")
                .addMember("level = %T.HIDDEN", DeprecationLevel::class)
                .build())
            .addSuperclassConstructorParameter(CodeBlock.builder().addInterceptorList(globalDecls.map { it.getPriority() to it.asType(emptyList()) }.sortedByDescending { it.first }, itcFactoryMap, fileSpec).build())
            .addSuperclassConstructorParameter(CodeBlock.builder().addInterceptorList(fallbackDecls.map { it.getPriority() to it.asType(emptyList()) }.sortedByDescending { it.first }, itcFactoryMap, fileSpec).build())
            .addSuperclassConstructorParameter(CodeBlock.builder().apply {
                addStatement("sortedMapOf(")
                routerObjectNameMap.forEach { (type, routerName) -> addStatement("%T::class.java.name to %L,", type, routerName) }
                addStatement(")")
            }.build())
            .addSuperclassConstructorParameter(CodeBlock.builder().apply {
                addStatement("%T {", ClassNames.ROUTER_TREE)
                rootNode.generateCode(this)
                addStatement("}")
            }.build())

        fileSpec.addType(managerSpec.build())
        
        // 关联所有原始文件和元数据文件作为依赖，确保全量聚合的稳定性
        val allSources = (routerDecls + globalDecls + fallbackDecls + metadataClasses).mapNotNull { it.containingFile }.distinct().toTypedArray()
        val dependencies = Dependencies(true, *allSources)
        fileSpec.build().writeTo(codeGenerator, dependencies)
        generateSpiFile("com.ilittle7.router.AbsRouterManager", "$packageName.$routerManagerName", dependencies)
    }

    private fun generateRouterConfig(baseUri: String, baseUriClass: KSClassDeclaration) {
        val packageName = "com.ilittle7.router.gen"
        val className = "GeneratedRouterConfig"
        val fileSpec = FileSpec.builder(packageName, className)
            .addType(TypeSpec.classBuilder(className)
                .addSuperinterface(ClassName("com.ilittle7.router", "IRouterConfig"))
                .addModifiers(KModifier.PUBLIC)
                .addProperty(PropertySpec.builder("baseUri", String::class.asTypeName().copy(nullable = true), KModifier.OVERRIDE).initializer("%S", baseUri).build())
                .build())
            .build()
        fileSpec.writeTo(codeGenerator, Dependencies(false, baseUriClass.containingFile!!))
        generateSpiFile("com.ilittle7.router.IRouterConfig", "$packageName.$className", Dependencies(false, baseUriClass.containingFile!!))
    }

    private fun generateSpiFile(serviceInterface: String, implementation: String, dependencies: Dependencies) {
        val resourceFile = "META-INF/services/$serviceInterface"
        try {
            codeGenerator.createNewFile(dependencies, "", resourceFile, "").use { it.writer().use { w -> w.write(implementation) } }
        } catch (e: Exception) {}
    }

    private fun CodeBlock.Builder.addInterceptorList(list: List<Pair<Int, KSType>>, factoryMap: MutableMap<TypeName, String>, fileSpec: FileSpec.Builder): CodeBlock.Builder {
        add("listOf(\n")
        indent()
        list.forEach { (priority, type) ->
            val decl = type.declaration as KSClassDeclaration
            when {
                decl.isSubclassOf(ClassNames.INTERCEPTOR_FACTORY.canonicalName) -> {
                    val instance = if (decl.classKind == ClassKind.OBJECT) "%T" else "%T()"
                    add("%L to $instance,\n", priority, type.toTypeName())
                }
                decl.isSubclassOf(ClassNames.INTERCEPTOR.canonicalName) -> {
                    if (decl.classKind == ClassKind.OBJECT) {
                        fileSpec.tryToAddItcFactoryProperty(type, factoryMap)
                        add("%L to %L,\n", priority, factoryMap[type.toTypeName()])
                    } else {
                        add("%L to %T(%T()),\n", priority, ClassNames.DEFAULT_ITC_FACTORY, type.toTypeName())
                    }
                }
            }
        }
        unindent()
        add(")")
        return this
    }

    private fun FileSpec.Builder.tryToAddItcFactoryProperty(type: KSType, factoryMap: MutableMap<TypeName, String>) {
        val typeName = type.toTypeName()
        if (typeName !in factoryMap) {
            val name = (typeName as ClassName).simpleNames.joinToString("_").replaceFirstChar { it.lowercase() } + "Factory"
            addProperty(PropertySpec.builder(name, ClassNames.INTERCEPTOR_FACTORY, KModifier.PRIVATE).initializer("%T(%T)", ClassNames.DEFAULT_ITC_FACTORY, typeName).build())
            factoryMap[typeName] = name
        }
    }

    private fun getCustomInterceptors(routerClass: KSClassDeclaration): List<Pair<Int, KSType>> {
        val interceptors = mutableListOf<Pair<Int, KSType>>()
        val routerAnnotation = routerClass.annotations.find { it.shortName.asString() == "Router" }
        val interceptorClasses = routerAnnotation?.arguments?.find { it.name?.asString() == "interceptorArr" }?.value as? List<KSType>
        interceptorClasses?.forEach { interceptors.add(0 to it) }

        routerClass.annotations.forEach { annotation ->
            val annotationDecl = annotation.annotationType.resolve().declaration as? KSClassDeclaration
            val interceptorsAnno = annotationDecl?.annotations?.find { it.shortName.asString() == "Interceptors" }
            val nestedInterceptors = interceptorsAnno?.arguments?.find { it.name?.asString() == "interceptorArr" }?.value as? List<KSType>
            nestedInterceptors?.forEach { interceptors.add(0 to it) }
        }

        // 关键修复：手动寻找伴生对象并判断是否为拦截器
        routerClass.declarations.filterIsInstance<KSClassDeclaration>()
            .find { it.isCompanionObject }
            ?.let { companion ->
                if (companion.isSubclassOf(ClassNames.INTERCEPTOR.canonicalName)) {
                    interceptors.add(0 to companion.asType(emptyList()))
                }
            }
        
        return interceptors.map { (p, t) -> (t.declaration as KSClassDeclaration).getPriority() to t }.sortedByDescending { it.first }
    }

    private fun KSClassDeclaration.getPriority(): Int {
        val annot = annotations.find { it.shortName.asString() == "Priority" }
        return annot?.arguments?.find { it.name?.asString() == "priority" }?.value as? Int ?: 0
    }

    private fun KSClassDeclaration.isSubclassOf(name: String): Boolean {
        if (this.qualifiedName?.asString() == name) return true
        return superTypes.any { (it.resolve().declaration as? KSClassDeclaration)?.isSubclassOf(name) == true }
    }
}

class KspRouterProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment): SymbolProcessor = KspRouterProcessor(env.codeGenerator, env.logger, env.options)
}
