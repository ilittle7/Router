package com.ilittle7.router

import com.ilittle7.router.utils.InjectUtils
import com.ilittle7.router.utils.ScanSetting
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipException

abstract class TransformTask : DefaultTask() {
    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        println("RouterGradlePlugin task start:")
        val leftSlash = File.separator == "/"
        val classList = mutableListOf<String>()
        val start = System.currentTimeMillis()
        JarOutputStream(output.asFile.get().outputStream()).use { jarOutput ->
            // Scan directory (Copy and Collection)
            allDirectories.get().forEach { directory ->
                val directoryPath =
                    if (directory.asFile.absolutePath.endsWith(File.separatorChar)) {
                        directory.asFile.absolutePath
                    } else {
                        directory.asFile.absolutePath + File.separatorChar
                    }

                directory.asFile.walk().forEach { file ->
                    if (file.isFile) {
                        val entryName = if (leftSlash) {
                            file.path.substringAfter(directoryPath)
                        } else {
                            file.path.substringAfter(directoryPath).replace(File.separatorChar, '/')
                        }
                        if (entryName.isNotEmpty()) {
                            // Use stream to detect register, Take care, stream can only be read once,
                            // So, When Scan and Copy should open different stream;
                            if (shouldProcessClass(entryName) && !entryName.contains("$")) {
                                classList.add(entryName.replace(ScanSetting.DOT_CLASS, ""))
                            }
                            // Copy
                            file.inputStream().use { input ->
                                jarOutput.saveEntry(entryName, input)
                            }
                        }
                    }
                }
            }

            // debugCollection(targetList)
            var originInject: ByteArray? = null

            // Scan Jar, Copy & Scan & Code Inject
            val jars = allJars.get().map { it.asFile }
            for (sourceJar in jars) {
                // println("Jar file is $sourceJar")
                val jar = JarFile(sourceJar)
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    try {
                        // Exclude directory
                        if (entry.isDirectory || entry.name.isEmpty()) {
                            continue
                        }
                        if (entry.name != ScanSetting.GENERATE_TO_CLASS_FILE_NAME) {
                            if (shouldProcessClass(entry.name) && !entry.name.contains("$")) {
                                classList.add(entry.name.replace(ScanSetting.DOT_CLASS, ""))
                            }
                            // Copy
                            jar.getInputStream(entry).use { input ->
                                jarOutput.saveEntry(entry.name, input)
                            }
                        } else {
                            println("Find inject byte code, Skip ${entry.name}")
                            jar.getInputStream(entry).use { inputs ->
                                originInject = inputs.readAllBytes()
                                println("Find before originInject is ${originInject?.size}")
                            }
                        }
                    } catch (e: Exception) {
                        // Format Optimize: exclude [java.util.zip.ZipException: duplicate entry: META-INF/MANIFEST.MF]
                        if (e is ZipException && e.message?.contains("META-INF/MANIFEST.MF") == true) {
                            // Skip META-INF/MANIFEST.MF
                        } else {
                            println("[Warning] Merge [jar:entry] ${jar.name}:${entry.name}, error is $e ")
                        }
                    }
                }
                jar.close()
            }
            println("All router manager class list:${classList}")
            // Do inject
            println("Start inject byte code")
            if (originInject == null) { // Check
                error("Can not find ARouter inject point, Do you import ARouter?")
            }
            val resultByteArray = InjectUtils.referHackWhenInit(
                ByteArrayInputStream(originInject), classList
            )
            jarOutput.saveEntry(
                ScanSetting.GENERATE_TO_CLASS_FILE_NAME,
                ByteArrayInputStream(resultByteArray)
            )
            println("Inject byte code successful")
        }
        println("Router plugin inject time spend ${System.currentTimeMillis() - start} ms")
    }

    private fun shouldProcessClass(entryName: String): Boolean {
        return entryName.startsWith(ScanSetting.ROUTER_CLASS_PACKAGE_NAME)
    }

    private fun JarOutputStream.saveEntry(entryName: String, inputStream: InputStream) {
        this.putNextEntry(JarEntry(entryName))
        IOUtils.copy(inputStream, this)
        this.closeEntry()
    }
}