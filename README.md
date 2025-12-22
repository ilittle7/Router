# Router
Router是一个根据uri来开启一个Activity或者执行一个行为的框架。
### 特点
1. 支持通过字符串、`Uri`或者`Intent`跳转
2. 支持多module使用
3. 支持拦截器，全局拦截器
4. 支持自定义注解，定义一个拦截器集合
5. 支持拦截器优先级
6. 支持kotlin的`object`和`companion object`作为Interceptor
7. 拦截器使用方式类似okhttp
8. 内部使用`Intent`作为Request
9. kotlin风格的API，并对java做了兼容
10. 代码检查和自动修复（Lint）
11. 支持含有通配符或者前缀匹配

### Gradle依赖配置

接下来在工程中的每个使用Router的module的`build.gradle`里面添加如下依赖：
```groovy
dependencies {
    implementation 'io.github.ilittle7:router-lib:1.3.0'
    implementation 'io.github.ilittle7:router-annotation:1.3.0'
    kapt 'io.github.ilittle7:router-processor:1.3.0'
}
```
在使用kapt的module的*build.gradle*中，添加kapt插件：
```groovy
apply plugin: 'kotlin-kapt'
```
在app模块的*build.gradle*中添加router插件：
```groovy
apply plugin: 'butter-router'
```
### API

#### Router
一个router表示由一个uri指向的`Activity`或者行为。`@Router`指定的类都是一个router。

`@Router`注解可以添加在一个`Activity`、`Service`或者一个`RouterAction`上面：
```kotlin
@Router(["a://b/c"])
class DemoActivity: Activity()
```
当开启这个`DemoActivity`时就可以使用`route`方法:
```kotlin
route("a://b/c")
```
java 中：
```java
Routers.route(context, "a://b/c");
```
这里也尊重传统的打开Activity的方式，如下写法和用uri的方式等效：
```kotlin
route(Intent(context, DemoActivity::class.java))
```

####  RouterBaseUri
`@RouterBaseUri` 的作用是简化写法。
```kotlin
@RouterBaseUri("a://b/")
class MyApplication : Application()
```
这时`route`方法就可以这么写：
```kotlin
route("/some_path")
```
并且`@Router`注解也可以直接写path
```kotlin
@Router(["/some_path"])
class DemoActivity: AppCompatActivity()
```
> **注意**：`@RouterBaseUri`在所有module中最多只能写一个，所以加在Application上即可。
#### 拦截器
拦截器的作用是在跳转一个router之前或之后添加一些行为。下面是一个弹出土司的例子:
```kotlin
object ToastInterceptor : Interceptor {
    override fun onIntercept(chain: Chain): Response {
        Toast.makeText(
            chain.launcherWrapper.context,
            "ToastInterceptor: ${chain.requestIntent}",
            Toast.LENGTH_LONG
        ).show()
        return chain.proceed(chain.requestIntent)
    }
}
```
应用`ToastInterceptor`：
```kotlin
@Router(["/demo"], [ToastInterceptor::class])
class DemoActivity: AppCompatActivity()
```

拦截器的执行顺序可以由`@Priority`来决定。优先级数字越大的拦截器越早被调用，不加默认为0。通常拦截器的执行时序如下图：

![Interceptor执行时序图](https://github.com/ButterCam/router/blob/master/resources/interceptor_sequence.png)
#### 自定义拦截器注解
自定义拦截器注解表示一个拦截器集合，目的是为了增强复用性，下面是一个例子：
```kotlin
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Interceptors([LoginInterceptor::class])
annotation class LoginRequired
```
应用`@LoginRequired`：
```kotlin
@LoginRequired
@Router("/demo", [ToastInterceptor::class])
class DemoActivity: AppCompatActivity()
```
#### 全局拦截器
全局拦截器表示影响所有router的拦截器，包括不同module中的router。用`@GlobalInterceptor`指定：
```kotlin
@GlobalInterceptor
@Priority(10)
object SomeInterceptor : Interceptor
```
#### RouterAction
`RouterAction`可以将一个函数表示为一个router：
```kotlin
@Router(["/toastAction"])
class ToastRouterAction : RouterAction {
    override fun run(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        Toast.makeText(launcherWrapper.context, "toast by ToastAction", Toast.LENGTH_SHORT).show()
        return Response(success = true, intent = intent)
    }
}
```
#### Response
Response代表了整个route过程的结果。内部的Intent默认情况下就是作为Request的Intent。

在Interceptor和RouterAction实现中都可以干涉默认的返回结果。
#### 开启dialog
下面的例子为一个简单的作为Router的dialog：
```kotlin
@Router(["/dialog/test"])
class TestDialogFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val routerIntent = routerIntent
        view.findViewById<TextView>(R.id.dialog_text).append(" ${routerIntent?.data}")
    }
}
```
有以下几点需要说明：
1. Dialog必须为androidx的DialogFragment（或其子类）的子类
2. `routerIntent`是一个DialogFragment的扩展属性，用来获取开启当前dialog的Intent。
3. Router内部会判断启动当前dialog的类型，可以为FragmentActivity或者是已经attach的Fragment对象，若不是会直接返回失败Response
#### routeForResult函数
`routeForResult()`表示`startActivityForResult()`。如果目标是一个`RouterAction`那么就会运行这个Action的`runForResult`函数。
#### 判断一个uri是否指向一个router
一般写法：
```kotlin
if(someUri in RouterCollection){
	// ...
}
```
这里`someUri`也可以传path或者Intent，用法同`route()`函数
#### FallbackRouter
FallbackRouter是在所有router都不匹配所给的uri的时候执行的一个默认router。它执行`startActivity()`方法，目的是`route(Intent(context, NotRouterActivity::class.java))`也可以打开相应Activity。

FallbackRouter本身并不是一个公开的API，但可以给它添加拦截器：
```kotlin
@FallbackInterceptor
class SomeInterceptor: Interceptor
```
#### 路径通配符
路径通配符的一般用法如下：
```kotlin
@Router("/profile/{userId}")
class UserActivity: Activity()
```
此时如下写法就可以开启`UserActivity`:
```kotlin
route("/profile/114514")
```
此外在`UserActivity`内部还可以通过如下方式拿到`userId`：
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
	super.onCreate(savedInstanceState)
	val userId:String = intent["userId"] ?: ""
}
```
##### 前缀匹配
路径通配符还支持前缀匹配，例如`"/a/**"`可以匹配第一节path为`"/a"`的任意path。

前缀匹配优先级低于更“详细”的router path声明。例如`"/a/b/**"`会优先匹配`"/a/b/c"`

此外可以在router对象里通过`intent.postSegments`扩展属性拿到被匹配的路径后半部分
##### 路径匹配优先级与冲突
优先级：具体路径 > 通配符路径 > 前缀匹配

冲突：具有相同路径判定的path被视为path冲突，会在编译期抛出异常。下面几对path都是冲突的：
- `/a` - `/a`
- `/a/{b}` - `/a/{c}`
- `/{a}/**` - `/{b}/**`

##### 更详细的匹配参考
假设app内被声明的path有如下几个：
- `/a/b`
- `/a/b/c`
- `/a/b/**`
- `/a/{p1}`
- `/a/{p2}/c`
- `/a/**`

那么下面列出的运行时传入的path的匹配结果如下：
- `/a/b`->`/a/b`
- `/a/b/c`->`/a/b/c`
- `/a/c`->`/a/{p1}`
- `/a/c/c`->`/a/{p2}/c`
- `/a/b/d`->`/a/b/**`
- `/a/c/d`->`/a/**`

#### 传递ActivityOptions
下面是安卓原本的传递方式和router传递方式的对比：
```kotlin
// 传统方式
val aoc: ActivityOptionsCompat = ...
context.startActivity(Intent(context, TargetActivity::class.java), aoc.toBundle())

// Router方式，需要把options放到Intent里面
// 这样做是为了可以在拦截器中控制options
val ao: ActivityOptionsCompat = ...
context.route(Intent(context, TransitionActivity::class.java).apply {
    activityOptions = ao.toBundle()
})
```