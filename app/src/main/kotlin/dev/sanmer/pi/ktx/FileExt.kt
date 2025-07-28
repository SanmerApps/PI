package dev.sanmer.pi.ktx

import java.io.File

fun File?.temp() = File(this, System.currentTimeMillis().toString())