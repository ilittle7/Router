package com.ilittle7.router.utils

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.io.InputStream

@Suppress("SpellCheckingInspection")
object InjectUtils {
    // refer hack class when object init
    fun referHackWhenInit(inputStream: InputStream, targetList: List<String>): ByteArray {
        val cr = ClassReader(inputStream)
        // val cw = ClassWriter(cr, 0)
        // Fix: https://github.com/JailedBird/ArouterGradlePlugin/issues/4
        // Resolution: https://github.com/didi/DroidAssist/issues/38#issuecomment-1080378515
        val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
        val cv = InjectClassVisitor(Opcodes.ASM7, cw, targetList)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    private class InjectClassVisitor(
        api: Int,
        classWriter: ClassVisitor,
        val targetList: List<String>
    ) : ClassVisitor(api, classWriter) {

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, desc, signature, exceptions)
            return if (name == ScanSetting.GENERATE_TO_METHOD_NAME) {
                InjectMethodVisitor(api, mv, access, name, desc, targetList)
            } else {
                mv
            }
        }
    }

    private class InjectMethodVisitor(
        api: Int,
        methodVisitor: MethodVisitor?,
        access: Int,
        name: String?,
        descriptor: String?,
        val targetList: List<String>
    ) : AdviceAdapter(api, methodVisitor, access, name, descriptor) {
        override fun onMethodEnter() {
            targetList.forEach {
                val className = it.replace("/", ".")
                mv.visitLdcInsn(className)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/ilittle7/router/transform/ActualRouterManager",
                    ScanSetting.REGISTER_METHOD_NAME,
                    "(Ljava/lang/String;)V",
                    false
                )
            }
        }
    }
}
