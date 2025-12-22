package com.ilittle7.router

import javax.annotation.processing.Messager
import javax.tools.Diagnostic

lateinit var sMessager: Messager

fun warning(msg: String) {
    if (msg.isNotEmpty()) sMessager.printMessage(Diagnostic.Kind.WARNING, "$msg\r\n")
}

fun error(msg: String) {
    if (msg.isNotEmpty()) sMessager.printMessage(Diagnostic.Kind.ERROR, "$msg\r\n")
}

fun info(msg: String) {
    if (msg.isNotEmpty()) sMessager.printMessage(Diagnostic.Kind.NOTE, "$msg\r\n")
}