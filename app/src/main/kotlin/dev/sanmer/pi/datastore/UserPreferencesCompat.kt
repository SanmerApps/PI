package dev.sanmer.pi.datastore

import androidx.compose.runtime.Immutable
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.compat.BuildCompat
import java.io.InputStream
import java.io.OutputStream

@Immutable
data class UserPreferencesCompat(
    val provider: Provider,
    val dynamicColor: Boolean,
    val requester: String,
    val executor: String
) {
    constructor(original: UserPreferences) : this(
        provider = original.provider,
        dynamicColor = original.dynamicColor,
        requester = original.requester,
        executor = original.executor
    )

    fun writeTo(out: OutputStream) = UserPreferences.newBuilder()
        .setProvider(provider)
        .setDynamicColor(dynamicColor)
        .setRequester(requester)
        .setExecutor(executor)
        .build()
        .writeTo(out)

    companion object {
        fun from(input: InputStream) = UserPreferencesCompat(
            UserPreferences.parseFrom(input)
        )

        fun default() = UserPreferencesCompat(
            provider = Provider.None,
            dynamicColor = BuildCompat.atLeastS,
            requester = BuildConfig.APPLICATION_ID,
            executor = BuildConfig.APPLICATION_ID
        )
    }
}