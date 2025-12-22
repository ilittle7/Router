package com.ilittle7.router.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ilittle7.router.Chain
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Priority
import com.ilittle7.router.Response
import com.ilittle7.router.Router
import com.ilittle7.router.*
import com.ilittle7.router.custom_annotation.Export
import com.ilittle7.router.factory.TestObjFactory
import timber.log.Timber

@Export
@Router(["/testc"], [TestObjFactory::class])
class TestCActivity : AppCompatActivity() {
    @Priority(22)
    companion object : Interceptor {
        override fun onIntercept(chain: Chain): Response {
            Timber.e("Oh, its a fatal error!!")
            return chain.proceed(chain.requestIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_c)
    }

    override fun finish() {
        setResult(0x33, Intent().putExtra("test", "test result"))
        super.finish()
    }
}
