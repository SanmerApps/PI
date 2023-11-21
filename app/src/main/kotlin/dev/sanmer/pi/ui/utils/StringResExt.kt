package dev.sanmer.pi.ui.utils

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
@ReadOnlyComposable
private fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}

@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, @StringRes vararg formatArgs: Int): String {
    val resources = resources()
    val strings = formatArgs.map {
        resources.getString(it)
    }.toTypedArray()

    return resources.getString(id, *strings)
}

@Composable
fun formatStringResource(
    style: (SpanStyle) -> SpanStyle = { it },
    @StringRes id: Int,
    vararg args: Any
): AnnotatedString {
    val strRes = androidx.compose.ui.res.stringResource(id = id)

    val strList by remember {
        derivedStateOf { strRes.split("%[1-${args.size}]\\\$s".toRegex()) }
    }

    val argsMap by remember {
        derivedStateOf {
            buildMap(args.size) {
                var argsLength = 0
                repeat(args.size) {
                    val char = "%${it + 1}\$s"
                    val index = strRes.indexOf(char) + argsLength
                    val value = args[it].toString().apply {
                        if (isNotEmpty()) argsLength += length - char.length
                    }
                    put(index, value)
                }
            }
        }
    }

    val currentStyle = LocalTextStyle.current.toSpanStyle()
    val italicStyle by remember { derivedStateOf { style(currentStyle) } }

    return buildAnnotatedString {
        strList.forEach {
            if (argsMap.containsKey(length)) {
                withStyle(italicStyle) { append(argsMap.getValue(length)) }
            }

            withStyle(currentStyle) { append(it) }
        }
    }
}