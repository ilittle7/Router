package com.ilittle7.router.action

import android.content.Intent
import android.util.Log
import com.ilittle7.router.LauncherWrapper
import com.ilittle7.router.Response
import com.ilittle7.router.Router
import com.ilittle7.router.RouterAction
import com.ilittle7.router.route

@Router(path = ["/tta1"])
class TTa1Action: RouterAction {
    override fun run(launcherWrapper: LauncherWrapper, intent: Intent): Response {
        Log.i("TTa1Action", "run: $intent")
        return Response(true)
    }
}