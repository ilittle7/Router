# Router 框架自动生成的混淆规则

# 1. 保留生成的路由注册类和包名，防止 ServiceLoader 找不到实现类
-keep class com.ilittle7.router.gen.** { *; }

# 2. 保留路由管理和配置的核心接口/基类
-keep class com.ilittle7.router.AbsRouterManager { *; }
-keep interface com.ilittle7.router.IRouterConfig { *; }

# 3. 保留所有由 KSP 生成的 SPI 实现类及其无参构造函数（供 ServiceLoader 实例化）
# R8 会自动根据这些类的保留情况来处理 META-INF/services 配置文件
-keep class * implements com.ilittle7.router.AbsRouterManager {
    public <init>();
}
-keep class * implements com.ilittle7.router.IRouterConfig {
    public <init>();
}

# 4. 保护 Kotlin object 的单例字段
-keepclassmembers class * {
    public static ** INSTANCE;
}
