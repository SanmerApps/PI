package dev.sanmer.pi.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

@Composable
fun BottomBarLayout(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    bottomBar: @Composable (PaddingValues) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) = Surface(
    modifier = modifier,
    contentColor = contentColor,
    color = containerColor,
    tonalElevation = tonalElevation
) {
    SubcomposeLayout { constraints ->
        val insets = contentWindowInsets.asPaddingValues(this)
        val insetsTop = insets.calculateTopPadding()
        val insetsBottom = insets.calculateBottomPadding()
        val insetsStart = insets.calculateStartPadding(layoutDirection)
        val insetsEnd = insets.calculateStartPadding(layoutDirection)

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val bottomBarPlaceable = subcompose("BottomBar") {
            val innerPadding = PaddingValues(
                bottom = insetsBottom,
                start = insetsStart,
                end = insetsEnd
            )

            bottomBar(innerPadding)
        }.fastMap { it.measure(looseConstraints) }

        val bottomBarHeight = bottomBarPlaceable.fastMaxBy { it.height }?.height ?: 0
        val bottomHeight = bottomBarHeight + insetsBottom.roundToPx()

        val bodyContentPlaceable = subcompose("MainContent") {
            val innerPadding = PaddingValues(
                bottom = insetsTop + bottomHeight.toDp(),
                start = insetsStart,
                end = insetsEnd
            )

            content(innerPadding)
        }.fastMap { it.measure(looseConstraints) }

        val bodyContentHeight = bodyContentPlaceable.fastMaxBy { it.height }?.height ?: 0

        layout(constraints.maxWidth, bodyContentHeight) {
            bodyContentPlaceable.fastForEach {
                it.place(
                    x = 0,
                    y = insetsTop.roundToPx()
                )
            }

            bottomBarPlaceable.fastForEach {
                it.place(
                    x = 0,
                    y = bodyContentHeight - bottomHeight
                )
            }
        }
    }
}