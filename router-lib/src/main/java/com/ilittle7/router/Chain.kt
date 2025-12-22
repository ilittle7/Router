package com.ilittle7.router

import android.content.Intent

class Chain internal constructor(
    val launcherWrapper: LauncherWrapper,
    val requestIntent: Intent,
    private val interceptorFactoryList: List<InterceptorFactory>,
    private val index: Int,
    private val launchAction: (Intent) -> Response
) {
    fun proceed(intent: Intent): Response {
        if (index >= interceptorFactoryList.size) {
            return launchAction(intent)
        }
        return interceptorFactoryList[index].get().onIntercept(
            Chain(
                launcherWrapper,
                intent,
                interceptorFactoryList,
                index + 1,
                launchAction
            )
        )
    }
}