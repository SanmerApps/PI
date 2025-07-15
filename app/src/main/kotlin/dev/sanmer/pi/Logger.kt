package dev.sanmer.pi

import android.util.Log

interface Logger {
    fun v(msg: String)
    fun v(tr: Throwable)
    fun d(msg: String)
    fun d(tr: Throwable)
    fun i(msg: String)
    fun i(tr: Throwable)
    fun w(msg: String)
    fun w(tr: Throwable)
    fun e(msg: String)
    fun e(tr: Throwable)

    class Android(private val tag: String) : Logger {
        override fun v(msg: String) {
            Log.v(tag, msg)
        }

        override fun v(tr: Throwable) {
            Log.v(tag, tr.stackTraceToString())
        }

        override fun d(msg: String) {
            Log.d(tag, msg)
        }

        override fun d(tr: Throwable) {
            Log.d(tag, tr.stackTraceToString())
        }

        override fun i(msg: String) {
            Log.i(tag, msg)
        }

        override fun i(tr: Throwable) {
            Log.i(tag, tr.stackTraceToString())
        }

        override fun w(msg: String) {
            Log.w(tag, msg)
        }

        override fun w(tr: Throwable) {
            Log.w(tag, tr.stackTraceToString())
        }

        override fun e(msg: String) {
            Log.e(tag, msg)
        }

        override fun e(tr: Throwable) {
            Log.e(tag, tr.stackTraceToString())
        }
    }
}