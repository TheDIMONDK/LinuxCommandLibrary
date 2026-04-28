package com.linuxcommandlibrary.app.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linuxcommandlibrary.app.NavEvent
import com.linuxcommandlibrary.app.platform.shareButtonDescription
import com.linuxcommandlibrary.app.platform.shareButtonIcon
import com.linuxcommandlibrary.shared.CommandElement
import com.linuxcommandlibrary.shared.platform.ShareHandler
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject

@Composable
fun CommandView(
    command: String,
    elements: List<CommandElement>,
    onNavigate: (NavEvent) -> Unit = {},
    verticalPadding: Dp = 6.dp,
) {
    val codeColor = MaterialTheme.colorScheme.primary
    val baseAnnotatedString = remember(elements, codeColor) {
        buildAnnotatedString {
            elements.forEach { element ->
                when (element) {
                    is CommandElement.Text -> {
                        append(element.text)
                    }

                    is CommandElement.Man -> {
                        val start = this.length
                        withStyle(style = SpanStyle(color = codeColor)) {
                            append(element.man)
                        }
                        val end = this.length
                        addLink(
                            LinkAnnotation.Clickable(
                                tag = "man:${element.man}",
                                linkInteractionListener = {
                                    onNavigate(NavEvent.ToCommand(element.man))
                                },
                            ),
                            start,
                            end,
                        )
                    }

                    is CommandElement.Url -> {
                        val start = this.length
                        withStyle(style = SpanStyle(color = codeColor)) {
                            append(element.command)
                        }
                        val end = this.length
                        addLink(
                            LinkAnnotation.Url(element.url),
                            start,
                            end,
                        )
                    }
                }
            }
        }
    }

    val finalAnnotatedString = remember(baseAnnotatedString) {
        if (baseAnnotatedString.text.isEmpty()) {
            baseAnnotatedString
        } else {
            buildAnnotatedString {
                append(baseAnnotatedString)
                addStyle(
                    style = ParagraphStyle(textDirection = TextDirection.Ltr),
                    start = 0,
                    end = baseAnnotatedString.text.length,
                )
            }
        }
    }

    Row(modifier = Modifier.padding(start = 12.dp, end = 4.dp).padding(vertical = verticalPadding)) {
        Text(
            text = finalAnnotatedString,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            style = MaterialTheme.typography.titleSmall,
        )

        if ('\n' !in command) {
            val shareHandler: ShareHandler = koinInject()
            val shareAction = remember(shareHandler, command) {
                {
                    val cleanedCommand = command.dropWhile { it == '$' || it.isWhitespace() }.replace("\\n", "")
                    shareHandler.shareText(cleanedCommand)
                }
            }
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .pointerHoverIcon(PointerIcon.Hand),
                onClick = shareAction,
            ) {
                Icon(
                    imageVector = shareButtonIcon,
                    contentDescription = shareButtonDescription,
                )
            }
        }
    }
}

