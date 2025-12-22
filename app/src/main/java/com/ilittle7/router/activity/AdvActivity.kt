package com.ilittle7.router.activity

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.ilittle7.router.R
import com.ilittle7.router.Router
import com.ilittle7.router.custom_annotation.Export
import com.ilittle7.router.postSegments
import com.ilittle7.router.get

@Export
@Router(path = ["butter://web:8080/request/{requestCount}", "/adv/{probability}", "/{p1}/{p2}/{p3}", "/adv/{probability}/notShow/id/stub", "/adv/{probability}/notShow/**"])
class AdvActivity : Activity() {
    private val tvAdv: TextView by lazy { findViewById(R.id.tv_adv) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adv)

        val count: Int = intent["requestCount"] ?: -1
        tvAdv.append(count.toString())
        val probability: Float = intent["probability"] ?: 0f
        tvAdv.append(" $probability")

        if (intent.data?.pathSegments?.contains("notShow") == true) {
            tvAdv.text = "probability: $probability, not show"
        }

        val p1: String = intent["p1"] ?: ""
        val p2: String = intent["p2"] ?: ""
        val p3: String = intent["p3"] ?: ""
        tvAdv.append("p1: $p1, p2: $p2, p3: $p3")

        val postSegments = intent.postSegments
        tvAdv.append("post segments: $postSegments")
    }
}

//fun Intent.getStringBinding(key:String):String?{
//    return (getSerializableExtra() as Map<String,Any>)[key] as? String
//}
//
////adv/pay/3.5