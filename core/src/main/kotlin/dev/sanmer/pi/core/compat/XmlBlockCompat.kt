package dev.sanmer.pi.core.compat

import android.content.res.XmlBlock
import android.content.res.XmlResourceParser

object XmlBlockCompat {
    fun <R> newParser(data: ByteArray, block: (XmlResourceParser) -> R) = when {
        BuildCompat.atLeastS -> {
            XmlBlock(data).use { xmlBlock ->
                xmlBlock.newParser().use { parser ->
                    block(parser)
                }
            }
        }

        else -> {
            val cls = Class.forName("android.content.res.XmlBlock")
            cls.getConstructor(ByteArray::class.java).run {
                isAccessible = true
                newInstance(data) as AutoCloseable
            }.use { xmlBlock ->
                @Suppress("DiscouragedPrivateApi")
                (cls.getDeclaredMethod("newParser").invoke(xmlBlock) as XmlResourceParser)
                    .use { parser ->
                        block(parser)
                    }
            }
        }
    }
}