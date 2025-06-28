@file:Suppress("NOTHING_TO_INLINE")

package dev.sanmer.pi.ui.ktx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

inline operator fun PaddingValues.plus(other: PaddingValues): PaddingValues =
    OperatorPaddingValues(this, other, Dp::plus)

inline operator fun PaddingValues.minus(other: PaddingValues): PaddingValues =
    OperatorPaddingValues(this, other, Dp::minus)

@Immutable
class OperatorPaddingValues(
    private val that: PaddingValues,
    private val other: PaddingValues,
    private val operator: Dp.(Dp) -> Dp,
) : PaddingValues {
    override fun calculateBottomPadding(): Dp =
        operator(
            that.calculateBottomPadding(),
            other.calculateBottomPadding()
        )

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        operator(
            that.calculateLeftPadding(layoutDirection),
            other.calculateLeftPadding(layoutDirection)
        )

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        operator(
            that.calculateRightPadding(layoutDirection),
            other.calculateRightPadding(layoutDirection)
        )

    override fun calculateTopPadding(): Dp =
        operator(
            that.calculateTopPadding(),
            other.calculateTopPadding()
        )
}
