package dev.sanmer.pi.ktx

import androidx.core.os.LocaleListCompat
import java.util.Locale

fun LocaleListCompat.toList(): List<Locale> = List(size()) { this[it]!! }