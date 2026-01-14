package com.ilittle7.testmodule

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ilittle7.testmodule.R
import com.ilittle7.router.Router
import com.ilittle7.router.route

@Router(["/testModule"])
class TestModuleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_module)

        route("/aaaaaa")
    }
}