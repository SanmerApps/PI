package dev.sanmer.pi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

@Composable
fun BottomSheetLayout(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    contentWindowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    bottomBar: @Composable (PaddingValues) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) = Column(
    modifier = modifier
        .background(scrimColor)
        .fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom
) {
    var edgeToTop by remember { mutableStateOf(false) }

    SubcomposeLayout(
        modifier = Modifier
            .widthIn(max = sheetMaxWidth)
            .fillMaxWidth()
            .background(
                color = containerColor,
                shape = shape
            )
            .clip(shape = shape)
    ) { constraints ->
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight

        val insets = contentWindowInsets.asPaddingValues(this)
        val insetsTop = if (edgeToTop) insets.calculateTopPadding() else 0.dp
        val insetsBottom = insets.calculateBottomPadding()
        val insetsStart = insets.calculateStartPadding(layoutDirection)
        val insetsEnd = insets.calculateStartPadding(layoutDirection)

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val bottomBarPlaceable = subcompose("BottomBar") {
            val bottomBarPadding = PaddingValues(
                bottom = insetsBottom,
                start = insetsStart,
                end = insetsEnd
            )

            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                bottomBar(bottomBarPadding)
            }
        }.fastMap { it.measure(looseConstraints) }

        val bottomBarHeight = bottomBarPlaceable.fastMaxBy { it.height }?.height ?: 0

        val bodyContentPlaceable = subcompose("MainContent") {
            val contentPadding = PaddingValues(
                bottom = insetsTop + bottomBarHeight.toDp(),
                start = insetsStart,
                end = insetsEnd
            )

            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                content(contentPadding)
            }
        }.fastMap { it.measure(looseConstraints) }

        val bodyContentHeight = bodyContentPlaceable.fastMaxBy { it.height }?.height ?: 0
        edgeToTop = bodyContentHeight >= maxHeight

        layout(maxWidth, bodyContentHeight) {
            bodyContentPlaceable.fastForEach {
                it.place(
                    x = 0,
                    y = insetsTop.roundToPx()
                )
            }

            bottomBarPlaceable.fastForEach {
                it.place(
                    x = 0,
                    y = bodyContentHeight - bottomBarHeight
                )
            }
        }
    }
}