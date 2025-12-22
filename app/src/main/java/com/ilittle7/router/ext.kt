package com.ilittle7.router

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T : Any> lateInit(): ReadWriteProperty<Any?, T> {
    return object : ReadWriteProperty<Any?, T> {
        @Volatile
        lateinit var value: T

        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            check(this::value.isInitialized) { "The value of lateInit is not assigned" }
            return value
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
            synchronized(this) {
                check(this::value.isInitialized.not()) { "The value of lateInit is already assigned" }
                this.value = value
            }
    }
}