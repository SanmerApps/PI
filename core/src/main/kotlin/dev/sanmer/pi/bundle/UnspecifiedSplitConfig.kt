package dev.sanmer.pi.bundle

class UnspecifiedSplitConfig(
    filename: String,
    size: Long,
) : SplitConfig(
    null,
    filename,
    size
) {
    override val name = filename

    override val isRequired = false

    override val isDisabled = false

    override val isRecommended = true
}