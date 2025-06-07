package dev.sanmer.pi.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import dev.sanmer.pi.R

@Composable
fun PageIndicator(
    icon: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable ColumnScope.() -> Unit = {},
    height: Dp = Dp.Unspecified
) = Column(
    modifier = modifier then (if (height.isSpecified) {
        Modifier
            .height(height = height)
            .fillMaxWidth()
    } else {
        Modifier.fillMaxSize()
    }),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    icon()
    Spacer(modifier = Modifier.height(20.dp))
    text()
}

@Composable
fun PageIndicator(
    icon: Int,
    text: Int,
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified
) = PageIndicator(
    icon = {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = PageIndicatorDefaults.ContentColor,
            modifier = Modifier.size(PageIndicatorDefaults.IconSize)
        )
    },
    text = {
        Text(
            text = stringResource(text),
            style = PageIndicatorDefaults.TextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    modifier = modifier,
    height = height
)

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified
) = PageIndicator(
    icon = {
        CircularProgressIndicator(
            modifier = Modifier.size(PageIndicatorDefaults.IconSize),
            strokeWidth = 5.dp
        )
    },
    modifier = modifier,
    height = height
)

@Composable
fun Failed(
    message: String,
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified
) = PageIndicator(
    icon = {
        Icon(
            painter = painterResource(R.drawable.bug),
            contentDescription = null,
            tint = PageIndicatorDefaults.ContentColor,
            modifier = Modifier.size(PageIndicatorDefaults.IconSize)
        )
    },
    text = {
        Text(
            text = message,
            style = PageIndicatorDefaults.TextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    modifier = modifier,
    height = height
)

@Composable
fun Failed(
    error: Throwable,
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified
) = Failed(
    message = error.message ?: error.javaClass.name,
    modifier = modifier,
    height = height
)

object PageIndicatorDefaults {
    val IconSize = 48.dp
    val ContentColor @Composable get() = MaterialTheme.colorScheme.outline.copy(0.5f)

    val TextStyle @Composable get() = TextStyle(
        color = ContentColor,
        fontSize = 20.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
}