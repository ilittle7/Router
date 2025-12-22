package com.ilittle7.router

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            println("Init RouterGradlePlugin")
            val androidComponents =
                project.extensions.getByType(AndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
//                if (variant.name.contains("debug")) {
//                    println("Skip Router Transform When Debug Build!")
//                    return@onVariants
//                }

                val taskProviderTransformAllClassesTask =
                    project.tasks.register(
                        "${variant.name}TransformTask",
                        TransformTask::class.java
                    )
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskProviderTransformAllClassesTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        TransformTask::allJars,
                        TransformTask::allDirectories,
                        TransformTask::output
                    )
            }
        }

    }
}