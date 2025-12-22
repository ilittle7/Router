package com.ilittle7.router

import android.content.Intent
import android.net.Uri

internal interface IRouterCollection {
    operator fun contains(path: String): Boolean
    operator fun contains(uri: Uri): Boolean
    operator fun contains(intent: Intent): Boolean
}

object RouterCollection : IRouterCollection by routerManager