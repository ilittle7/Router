package com.ilittle7.router.action

import android.content.Intent
import com.ilittle7.router.LauncherWrapper
import com.ilittle7.router.Response
import com.ilittle7.router.Router
import com.ilittle7.router.RouterAction

@Router(["/test/error/message"])
class SomeErrorAction : RouterAction {
    override fun run(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        return Response(
            false,
            intent,
            message = "Fatal!! The phone is about to explode!!"
        )
    }
}