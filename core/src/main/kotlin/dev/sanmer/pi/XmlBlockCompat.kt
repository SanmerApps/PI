package dev.sanmer.pi

import android.content.res.XmlBlock
import android.content.res.XmlResourceParser

object XmlBlockCompat {
    fun newParser(data: ByteArray): XmlResourceParser {
        return if (BuildCompat.atLeastS) {
            XmlBlock(data).use { it.newParser() }
        } else {
            val cls = Class.forName("android.content.res.XmlBlock")
            cls.getConstructor(ByteArray::class.java).run {
                isAccessible = true
                newInstance(data) as AutoCloseable
            }.use {
                @Suppress("DiscouragedPrivateApi")
                cls.getDeclaredMethod("newParser").invoke(it) as XmlResourceParser
            }
        }
    }
}