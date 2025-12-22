@file:Suppress("UNUSED_PARAMETER")

package com.ilittle7.router

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ilittle7.router.action.NoPathAction
import com.ilittle7.router.action.TestToastAction
import com.ilittle7.router.activity.AdvActivity
import com.ilittle7.router.activity.TransitionActivity
import com.ilittle7.router.dialog.TestDialogFragment
import com.ilittle7.router.service.TestAService
import com.ilittle7.router.service.TestBService
import com.ilittle7.router.service.TestCService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun testA(view: View) {
        route(Uri.parse("/testa/111?userToken=someToken"))
    }

    fun testB(view: View) {
        route("/testb")
    }

    fun testC(view: View) {
        routeForResult("/testc", 0x11)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && resultCode == 0x33 && data != null) {
            Toast.makeText(this, data.getStringExtra("test").orEmpty(), Toast.LENGTH_SHORT).show()
        }
    }

    fun testRouterAction(view: View) {
        route("/testToastAction")
        route(Intent(this, TestToastAction::class.java))
    }

    fun testRouterService(view: View) {
        application.route(Intent(application, TestAService::class.java))
        application.route(Intent(application, TestBService::class.java))
        application.route(Intent(application, TestCService::class.java))
    }

    fun testModuleJump(view: View) {
        route("/testModule")
    }

    fun testNoPath(view: View) {
        route(Intent(this, NoPathAction::class.java))
        route(Intent(this, AdvActivity::class.java))
    }

    fun testPathParam(view: View) {
        route("/adv/0.375")
        route("/a/b/c")
        route("butter://web:8080/request/23")
        route("/adv/0.625/notShow/a/b/c")
        route("/adv/0.6/notShow/id/stub")
    }

    fun testStartError(view: View) {
        route(Intent(this, ExceptionActivity::class.java))
    }

    @SuppressLint("Assert")
    fun otherTest(view: View) {
        assert("/a/b/c" in RouterCollection)
        assert("/adv/0.375" in RouterCollection)
        assert("/a/b/c" in RouterCollection)
        assert("butter://web:8080/request/23" in RouterCollection)
        assert("/adv/0.625/no0tShow/a/b/c/d" !in RouterCollection)
        assert("/adv/0.6/notShow/id/stub" in RouterCollection)
    }

    fun testFallbackInterceptor(view: View) {
        route("/a/b/c/d/e/f")
    }

    fun testTransition(view: View) {
        TransitionActivity.transitionStart(this, view)
    }

    fun testDialogFragmentRouter(view: View) {
        Fragment().route("/dialog/test")
        route("/dialog/test")
        route(Intent(this, TestDialogFragment::class.java))
    }

    fun testResponseMessage(view: View) {
        val response = route("/test/error/message")
        Toast.makeText(view.context, response.message.orEmpty(), Toast.LENGTH_SHORT).show()
    }
}