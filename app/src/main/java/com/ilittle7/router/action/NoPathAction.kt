package com.ilittle7.router.action

import android.content.Intent
import android.widget.Toast
import com.ilittle7.router.LauncherWrapper
import com.ilittle7.router.Response
import com.ilittle7.router.Router
import com.ilittle7.router.RouterAction

@Router
class NoPathAction : RouterAction {
    override fun run(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        Toast.makeText(launcherWrapper.context, "I'm an Action with no path!", Toast.LENGTH_SHORT).show()
        return Response(success = true, intent = intent)
    }
}