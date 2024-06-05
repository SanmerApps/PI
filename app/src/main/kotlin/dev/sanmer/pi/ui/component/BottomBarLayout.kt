package dev.sanmer.pi.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

@Composable
fun BottomBarLayout(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) = SubcomposeLayout(modifier) { constraints ->
    val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

    val bottomBarPlaceable = subcompose("BottomBar") {
        bottomBar()
    }.fastMap { it.measure(looseConstraints) }

    val bottomBarHeight = bottomBarPlaceable.fastMaxBy { it.height }?.height ?: 0

    val bodyContentPlaceable = subcompose("MainContent") {
        val innerPadding = PaddingValues(bottom = bottomBarHeight.toDp())
        content(innerPadding)
    }.fastMap { it.measure(looseConstraints) }

    val bodyContentHeight = bodyContentPlaceable.fastMaxBy { it.height }?.height ?: 0

    layout(constraints.maxWidth, bodyContentHeight) {
        bodyContentPlaceable.fastForEach {
            it.place(0, 0)
        }

        bottomBarPlaceable.fastForEach {
            it.place(0, bodyContentHeight - bottomBarHeight)
        }
    }
}