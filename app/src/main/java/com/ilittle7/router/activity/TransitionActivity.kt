package com.ilittle7.router.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import com.ilittle7.router.R
import com.ilittle7.router.activityOptions
import com.ilittle7.router.route

class TransitionActivity : AppCompatActivity() {
    companion object {
        const val TRANSITION_BUTTON = "transition_button"

        fun transitionStart(activity: Activity, view: View) {
            val ao: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                view,
                TRANSITION_BUTTON
            )
            activity.route(Intent(activity, TransitionActivity::class.java).apply {
                activityOptions = ao.toBundle()
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transition)
        val button: ImageView = findViewById(R.id.button)
        ViewCompat.setTransitionName(button, TRANSITION_BUTTON)

        button.setOnClickListener { onBackPressed() }
    }
}