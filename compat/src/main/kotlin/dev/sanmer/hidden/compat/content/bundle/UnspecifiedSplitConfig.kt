package dev.sanmer.hidden.compat.content.bundle

class UnspecifiedSplitConfig(
    filename: String,
    size: Long,
) : SplitConfig(
    filename,
    size
) {
    override val name = filename

    override fun isRequired(): Boolean {
        return false
    }

    override fun isDisabled(): Boolean {
        return false
    }

    override fun isRecommended(): Boolean {
        return true
    }
}