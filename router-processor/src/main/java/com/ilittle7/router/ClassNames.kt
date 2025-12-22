package com.ilittle7.router

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

object ClassNames {
    private const val LIB_COMPILE_PATH = "com.ilittle7.router"
    val ROUTER_ACTION = ClassName.bestGuess("$LIB_COMPILE_PATH.RouterAction")
    val INTERCEPTOR = ClassName.bestGuess("$LIB_COMPILE_PATH.Interceptor")
    val INTERCEPTOR_FACTORY = ClassName.bestGuess("$LIB_COMPILE_PATH.InterceptorFactory")
    val DEFAULT_ITC_FACTORY = ClassName.bestGuess("$LIB_COMPILE_PATH.DefaultItcFactory")
    val ABS_ROUTER_MANAGER = ClassName.bestGuess("$LIB_COMPILE_PATH.AbsRouterManager")
    val ACTIVITY_ROUTER = ClassName.bestGuess("$LIB_COMPILE_PATH.ActivityRouter")
    val ACTION_ROUTER = ClassName.bestGuess("$LIB_COMPILE_PATH.ActionRouter")
    val SERVICE_ROUTER = ClassName.bestGuess("$LIB_COMPILE_PATH.ServiceRouter")
    val DIALOG_FRAGMENT_ROUTER = ClassName.bestGuess("$LIB_COMPILE_PATH.DialogFragmentRouter")
    val ROUTER_TREE = ClassName.bestGuess("$LIB_COMPILE_PATH.RouterTree")

    val ACTIVITY = ClassName.bestGuess("android.app.Activity")
    val APP_COMPAT_ACTIVITY = ClassName.bestGuess("androidx.appcompat.app.AppCompatActivity")
    val SERVICE = ClassName.bestGuess("android.app.Service")
    val DIALOG_FRAGMENT = ClassName.bestGuess("androidx.fragment.app.DialogFragment")
    val URI = ClassName.bestGuess("android.net.Uri")

    val OBJECT = ClassName.bestGuess("java.lang.Object")
    val UNIT = Unit::class.asClassName()
    val LIST = List::class.asClassName()
    val STRING = String::class.asClassName()
    val INT = Int::class.asClassName()
    val PAIR = Pair::class.asClassName()

    // parameterized ClassName
    val PRIORITY_INTERCEPTOR_FACTORY_PAIR = PAIR.parameterizedBy(
        INT,
        INTERCEPTOR_FACTORY
    )
    val INTERCEPTOR_FACTORY_PAIR_LIST = LIST.parameterizedBy(
        PRIORITY_INTERCEPTOR_FACTORY_PAIR
    )
}