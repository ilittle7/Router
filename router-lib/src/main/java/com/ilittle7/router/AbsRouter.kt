package com.ilittle7.router

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import kotlin.reflect.KClass

@CompileSensitive
abstract class AbsRouter {
    /**
     * The Pair.first is the priority of the Interceptor
     */
    protected open val interceptors: List<Pair<Int, InterceptorFactory>> get() = emptyList()

    internal abstract fun start(
        launcherWrapper: LauncherWrapper,
        intent: Intent,
    ): Response

    internal abstract fun startForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent,
    ): Response

    /**
     * This is an interceptor list which has this router's interceptors
     * and global interceptors, It is sorted by the priority of the interceptor
     */
    protected val actualInterceptors: List<InterceptorFactory>
            by lazy {
                (interceptors + routerManager.globalInterceptorList)
                    .sortedByDescending { it.first }
                    .map { it.second }
            }
}

abstract class AbsGeneratedRouter(private val destination: KClass<*>) : AbsRouter() {
    protected abstract fun actualStart(launcherWrapper: LauncherWrapper, intent: Intent): Response
    protected abstract fun actualStartForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent,
    ): Response

    override fun start(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        return try {
            intent.setClass(launcherWrapper.context, destination.java)
            Chain(launcherWrapper, intent, actualInterceptors, 0) { pIntent ->
                try {
                    actualStart(launcherWrapper, pIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to start this router: ", e)
                    // return null to notify app that the router start failed
                    Response(success = false, intent = pIntent, exception = e)
                }
            }.proceed(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start this router: ", e)
            Response(success = false, intent = intent, exception = e)
        }
    }

    override fun startForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent,
    ): Response {
        return try {
            intent.setClass(launcherWrapper.context, destination.java)
            Chain(launcherWrapper, intent, actualInterceptors, 0) { pIntent ->
                try {
                    actualStartForResult(launcherWrapper, requestCode, pIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to start this router for result: ", e)
                    Response(success = false, intent = pIntent, exception = e)
                }
            }.proceed(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start this router for result: ", e)
            Response(success = false, intent = intent, exception = e)
        }
    }
}

@CompileSensitive
abstract class ActivityRouter(destination: KClass<out Activity>) : AbsGeneratedRouter(destination) {
    override fun actualStart(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        launcherWrapper.startActivity(intent)
        return Response(success = true, intent = intent)
    }

    override fun actualStartForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent
    ): Response {
        launcherWrapper.startActivityForResult(intent, requestCode)
        return Response(success = true, intent = intent)
    }
}

@CompileSensitive
abstract class ActionRouter(private val block: () -> RouterAction, destination: KClass<*>) :
    AbsGeneratedRouter(destination) {
    override fun actualStart(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        return block().run(launcherWrapper, intent)
    }

    override fun actualStartForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent
    ): Response {
        return block().runForResult(launcherWrapper, requestCode, intent)
    }
}

@CompileSensitive
abstract class ServiceRouter(destination: KClass<out Service>) : AbsGeneratedRouter(destination) {
    override fun actualStart(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        launcherWrapper.startService(intent)
        return Response(success = true, intent = intent)
    }

    override fun actualStartForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent
    ) = throw IllegalStateException("Can't start Service for result")
}

@CompileSensitive
abstract class DialogFragmentRouter(
    destination: KClass<out DialogFragment>,
    private val block: () -> DialogFragment,
) : AbsGeneratedRouter(destination) {
    override fun actualStart(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        val dialogFragment = block()
        dialogFragment.arguments = Bundle().apply {
            putParcelable(dialogFragmentIntentKey, intent)
        }
        launcherWrapper.startDialog(intent, dialogFragment)
        return Response(success = true, intent = intent)
    }

    override fun actualStartForResult(
        launcherWrapper: LauncherWrapper,
        requestCode: Int,
        intent: Intent
    ) = throw IllegalStateException("Can't start DialogFragment for result")
}