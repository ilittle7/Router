package com.ilittle7.router

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import timber.log.Timber

class AppLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.data?.let {
            Timber.e(it.toString())
            val intent = Intent()
            intent.data = it
            intent.putExtra("export", "sdlfj")
            route(intent)
        }
        finish()
    }
}