package dev.sanmer.pi.core.compat

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

@JvmInline
value class LocaleListCompat(val localeList: LocaleList) : Iterable<Locale> {
    override fun iterator() = object : Iterator<Locale> {
        private var index: Int = 0
        override fun next(): Locale = localeList.get(index++)
        override fun hasNext() = index < localeList.size()
    }

    companion object Default {
        fun getSystemLocales(context: Context): LocaleListCompat {
            if (BuildCompat.atLeastT) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                if (localeManager != null) return LocaleListCompat(localeManager.systemLocales)
            }
            return LocaleListCompat(Resources.getSystem().configuration.locales)
        }
    }
}