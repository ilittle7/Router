package com.ilittle7.router

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

abstract class BaseProcessor : AbstractProcessor() {
    lateinit var filer: Filer

    override fun init(env: ProcessingEnvironment) {
        super.init(env)
        sMessager = env.messager
        filer = env.filer
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedOptions() = setOf("org.gradle.annotation.processing.isolating")

    protected inline fun kFile(fileName: String, block: FileSpec.Builder.() -> Unit) =
        FileSpec.builder("com.ilittle7.router.gen", fileName).apply(block).build().writeTo(filer)

    // util method
    protected fun TypeElement.toTypeName(): TypeName {
        return ClassName.bestGuess(this.qualifiedName.toString())
    }

    protected inline fun <reified T : Annotation> T.getKClassArray(block: T.() -> Array<out KClass<*>>): List<TypeMirror> {
        try {
            block()
        } catch (e: MirroredTypesException) {
            return e.typeMirrors
        }
        throw IllegalStateException("Parameter block is wrong")
    }

    protected inline fun <reified T : Annotation> T.getKClassArray(
        arrClassTypeList: List<ClassName>,
        block: T.() -> Array<out KClass<*>>
    ): List<TypeMirror> {
        return getKClassArray(block).onEach { typeMirror ->
            check(arrClassTypeList.any { className ->
                typeMirror.isSubTypeOf(className)
            }) {
                "The parameters of @${T::class.simpleName} is not the subtype of ${arrClassTypeList.map(ClassName::simpleName)}"
            }
        }
    }

    protected inline fun <reified T : Annotation> T.getKClass(block: T.() -> KClass<*>): TypeMirror {
        try {
            block()
        } catch (e: MirroredTypeException) {
            return e.typeMirror
        }
        throw IllegalStateException("Parameter block is wrong")
    }

    protected fun TypeMirror.asElement(): Element = processingEnv.typeUtils.asElement(this)

    protected fun ClassName.asNullable(): ClassName = copy(true) as ClassName

    /**
     * 判断是否为 Kotlin Object
     * 逻辑：带有 @Metadata 注解，且包含一个 public static final 的 INSTANCE 字段
     */
    val TypeMirror.isKotlinObject: Boolean
        get() {
            val element = asElement() as? TypeElement ?: return false
            // 必须是 Kotlin 类
            if (element.getAnnotation(Metadata::class.java) == null) return false

            return element.enclosedElements.any {
                it.kind == ElementKind.FIELD &&
                        it.simpleName.toString() == "INSTANCE" &&
                        it.modifiers.containsAll(setOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
            }
        }

    /**
     * 判断是否为 Companion Object
     * 逻辑：带有 @Metadata 注解，且类名通常以 "Companion" 结尾，或者其父元素将其识别为 Companion
     */
    val TypeMirror.isCompanionObject: Boolean
        get() {
            val element = asElement() as? TypeElement ?: return false
            val metadata = element.getAnnotation(Metadata::class.java) ?: return false
            // Metadata 的 kind 为 2 通常表示是一个 Class (包含 Companion)
            // 在不依赖库的情况下，我们检查其是否为静态内部类且名称为 Companion
            return element.simpleName.toString() == "Companion" &&
                    element.modifiers.contains(Modifier.STATIC)
        }

    /**
     * 获取类的 Companion Object
     * 逻辑：查找内部类中名为 "Companion" 的元素
     */
    protected val TypeMirror.getCompanion: TypeMirror?
        get() {
            val currentTypeElement = asElement() as? TypeElement ?: return null
            return currentTypeElement.enclosedElements.find {
                it.kind == ElementKind.CLASS && it.simpleName.toString() == "Companion"
            }?.asType()
        }

    protected fun TypeMirror.isSubTypeOf(className: ClassName): Boolean {
        if (className == ClassNames.OBJECT) return true
        val directSupertypes = processingEnv.typeUtils.directSupertypes(this)
        val isSubtype = directSupertypes.any { it.typeName == className }
        if (isSubtype) return true

        if (directSupertypes.size == 1 &&
            directSupertypes.first().typeName == ClassNames.OBJECT
        ) return false

        for (supertypeMirror in directSupertypes) {
            if (supertypeMirror.isSubTypeOf(className)) return true
        }
        return false
    }
}