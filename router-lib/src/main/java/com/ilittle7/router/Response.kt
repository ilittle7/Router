package com.ilittle7.router

import android.content.Intent

open class Response(
    var success: Boolean,

    /**
     * This field is usually the request intent，but [Interceptor] and [RouterAction] can change it.
     */
    var intent: Intent? = null,

    var exception: Exception? = null,

    /**
     * This field is usually set in [Interceptor] and [RouterAction] for UI.
     */
    var message: String? = null,
)