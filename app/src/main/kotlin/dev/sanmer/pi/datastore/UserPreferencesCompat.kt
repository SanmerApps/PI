package dev.sanmer.pi.datastore

import androidx.compose.runtime.Immutable
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.compat.BuildCompat

@Immutable
data class UserPreferencesCompat(
    val provider: Provider,
    val dynamicColor: Boolean,
    val requester: String,
    val executor: String,
    val selfUpdate: Boolean
) {
    constructor(original: UserPreferences) : this(
        provider = original.provider,
        dynamicColor = original.dynamicColor,
        requester = original.requester,
        executor = original.executor,
        selfUpdate = original.selfUpdate
    )

    fun toProto(): UserPreferences = UserPreferences.newBuilder()
        .setProvider(provider)
        .setDynamicColor(dynamicColor)
        .setRequester(requester)
        .setExecutor(executor)
        .setSelfUpdate(selfUpdate)
        .build()

    companion object {
        fun default() = UserPreferencesCompat(
            provider = Provider.None,
            dynamicColor = BuildCompat.atLeastS,
            requester = BuildConfig.APPLICATION_ID,
            executor = BuildConfig.APPLICATION_ID,
            selfUpdate = true
        )

        fun UserPreferences.new(
            block: UserPreferencesKt.Dsl.() -> Unit
        ) = UserPreferencesCompat(this)
            .toProto()
            .copy(block)
    }
}