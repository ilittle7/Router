package com.ilittle7.router.activity

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ilittle7.router.Chain
import com.ilittle7.router.Interceptor
import com.ilittle7.router.Response
import com.ilittle7.router.Router

@Router(["/login"])
class LoginActivity :AppCompatActivity(){
    companion object: Interceptor{
        override fun onIntercept(chain: Chain): Response {

            Log.i("LoginActivity", "run: ${chain.requestIntent.data}")

            return chain.proceed(chain.requestIntent)
        }
    }
}