package com.ilittle7.router

import android.content.Intent

@CompileSensitive
interface RouterAction {
    fun run(launcherWrapper: LauncherWrapper, intent: Intent): Response =
        throw NotImplementedError()

    fun runForResult(launcherWrapper: LauncherWrapper, requestCode: Int, intent: Intent): Response =
        throw NotImplementedError()
}