plugins {
    `java-gradle-plugin`
    kotlin("jvm")
}

kotlin {
    jvmToolchain(11)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()   // 生成源码包
    withJavadocJar()   // 【新增】生成 Javadoc 包，解决 Javadocs must be provided 报错
}

gradlePlugin {
    plugins {
        create("RouterPlugin") {
            id = "io.github.ilittle7.router"
            implementationClass = "com.ilittle7.router.RouterPlugin"
        }
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:7.4.2")
    compileOnly("commons-io:commons-io:2.8.0")
    compileOnly("commons-codec:commons-codec:1.15")
    compileOnly("org.ow2.asm:asm-commons:9.4")
    compileOnly("org.ow2.asm:asm-tree:9.4")
}

afterEvaluate {
    publishing {
        publications {
            // 核心修复：对本模块产生的所有 Publication（包括主构件和 Marker 构件）进行统一填充
            withType<MavenPublication> {
                // 解决 Project name is missing 和 Project description is missing
                pom {
                    name.set("Router Plugin")
                    description.set("A lightweight Android Router framework gradle plugin")
                    url.set("https://github.com/ilittle7/Router")

                    // 解决 License information is missing
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    // 解决 Developers information is missing
                    developers {
                        developer {
                            id.set("ilittle7")
                            name.set("ilittle7")
                            email.set("ilittle7@163.com")
                        }
                    }

                    // 解决 SCM URL is not defined
                    scm {
                        connection.set("scm:git:github.com/ilittle7/Router.git")
                        developerConnection.set("scm:git:ssh://github.com/ilittle7/Router.git")
                        url.set("https://github.com/ilittle7/Router")
                    }
                }
            }

            // 仅针对主构件设置特殊的 artifactId，Marker 构件的 ID 会由 Gradle 自动管理，不要去动它
            matching { it.name == "pluginMaven" }.all {
                val pub = this as MavenPublication
                pub.artifactId = "router-plugin"
            }
        }
    }
}
//
//// 发布配置
//publishing {
//    publications {
//        // 使用 withType(MavenPublication::class) 确保所有构件（包括 Marker）都得到配置
//        withType<MavenPublication> {
//            // 注入 POM 描述信息，解决 Project name/description is missing 等 6 个报错
//            print("has extra: ${rootProject.extra.has("customizePom")}")
//            if (rootProject.extra.has("customizePom")) {
//                val customizePom = rootProject.extra["customizePom"] as groovy.lang.Closure<*>
//                customizePom.call(this.pom)
//            }
//        }
//
//        // 针对主要的插件 jar 包进行 artifactId 定制
//        matching { it.name == "pluginMaven" }.all {
//            val pub = this as MavenPublication
//            pub.artifactId = "router-plugin"
//        }
//    }
//}