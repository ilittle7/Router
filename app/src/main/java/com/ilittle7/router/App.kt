package com.ilittle7.router

import android.app.Application

var globalContext by lateInit<App>()

@RouterBaseUri("butter:///")
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        globalContext = this
//        Timber.plant(AndroidStudioTree())
    }
}