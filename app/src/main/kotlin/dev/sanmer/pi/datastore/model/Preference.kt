package dev.sanmer.pi.datastore.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Preference(
    @ProtoNumber(1)
    val originatingPackageName: String = "",
    @ProtoNumber(2)
    val installerPackageName: String = ""
)
