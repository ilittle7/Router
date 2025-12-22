package com.ilittle7.router

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ilittle7.router.interceptor.ExceptionInterceptor

@Router(interceptorArr = [ExceptionInterceptor::class])
class ExceptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}