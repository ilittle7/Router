package com.ilittle7.router.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ilittle7.router.R
import com.ilittle7.router.Router
import com.ilittle7.router.interceptor.ToastInterceptor

// These interceptors will be shrank to an array which only has one ToastInterceptor
@Router(["/testb"], [ToastInterceptor::class,ToastInterceptor::class,ToastInterceptor::class, ToastInterceptor::class])
class TestBActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_b)
    }
}