package dev.sanmer.pi.datastore.model

import dev.sanmer.pi.Const
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Preference(
    @ProtoNumber(1)
    val provider: Provider = Provider.None,
    @ProtoNumber(2)
    val automatic: Boolean = true,
    @ProtoNumber(3)
    val requester: String = "",
    @ProtoNumber(4)
    val executor: String = Const.SHELL,
    @ProtoNumber(5)
    val darkMode: DarkMode = DarkMode.Default
)