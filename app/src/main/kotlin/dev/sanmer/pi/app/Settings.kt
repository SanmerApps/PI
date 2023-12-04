package dev.sanmer.pi.app

object Settings {
    const val MODE = "WORKING_MODE"
    enum class Provider {
        None,
        Shizuku,
        SuperUser
    }

    const val REQUESTER = "REQUESTER_PACKAGE_NAME"
    const val EXECUTOR = "EXECUTOR_PACKAGE_NAME"
}