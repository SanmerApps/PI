package dev.sanmer.pi.datastore

import androidx.compose.runtime.Immutable
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.compat.BuildCompat

@Immutable
data class UserPreferencesExt(
    val provider: Provider,
    val dynamicColor: Boolean,
    val requester: String,
    val executor: String
) {
    companion object {
        fun default() = UserPreferencesExt(
            provider = Provider.None,
            dynamicColor = BuildCompat.atLeastS,
            requester = BuildConfig.APPLICATION_ID,
            executor = BuildConfig.APPLICATION_ID
        )
    }
}

fun UserPreferencesExt.toProto(): UserPreferences = UserPreferences.newBuilder()
    .setProvider(provider)
    .setDynamicColor(dynamicColor)
    .setRequester(requester)
    .setExecutor(executor)
    .build()

fun UserPreferences.toExt() = UserPreferencesExt(
    provider = provider,
    dynamicColor = dynamicColor,
    requester = requester,
    executor = executor
)

fun UserPreferences.new(
    block: UserPreferencesKt.Dsl.() -> Unit
) = toExt()
    .toProto()
    .copy(block)