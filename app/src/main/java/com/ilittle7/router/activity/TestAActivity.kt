package com.ilittle7.router.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ilittle7.router.R
import com.ilittle7.router.Router

@Router(["/testa/**"])
class TestAActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_a)
        findViewById<TextView>(R.id.tvOutput).text = intent.data!!.getQueryParameter("userToken")
    }
}