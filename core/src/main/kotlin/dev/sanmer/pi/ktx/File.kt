package dev.sanmer.pi.ktx

import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun File?.temp() = File(this, Uuid.random().toString())