@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.player

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.cgens67.avidtune.R

internal enum class PlaybackErrorKind {
    NoInternet,
    Timeout,
    NoStream,
    Decoder,
    Http,
    Unknown,
}

internal data class PlaybackErrorInfo(
    val kind: PlaybackErrorKind,
    val httpCode: Int?,
)

internal fun PlaybackException.toPlaybackErrorInfo(): PlaybackErrorInfo {
    val httpCode = httpStatusCodeOrNull()
    val kind =
        when {
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> PlaybackErrorKind.NoInternet
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackErrorKind.Timeout
            httpCode in setOf(403, 404, 410, 416) -> PlaybackErrorKind.NoStream
            errorCode in setOf(
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            ) -> PlaybackErrorKind.Decoder
            httpCode != null -> PlaybackErrorKind.Http
            else -> PlaybackErrorKind.Unknown
        }

    return PlaybackErrorInfo(
        kind = kind,
        httpCode = httpCode,
    )
}

internal fun PlaybackException.httpStatusCodeOrNull(): Int? {
    var throwable: Throwable? = cause
    while (throwable != null) {
        if (throwable is HttpDataSource.InvalidResponseCodeException) return throwable.responseCode
        throwable = throwable.cause
    }
    return null
}

@Composable
fun PlaybackError(
    error: PlaybackException,
    mediaId: String? = null,
    retry: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val fallbackUnknown = stringResource(R.string.error_unknown)
    val fallbackNoInternet = stringResource(R.string.error_no_internet)
    val fallbackTimeout = stringResource(R.string.error_timeout)
    val fallbackNoStream = "No stream"
    val retryText = stringResource(R.string.action_retry)
    val copyText = "Copy"
    val copiedText = stringResource(R.string.copied)
    
    val errorInfo = remember(error, mediaId) { error.toPlaybackErrorInfo() }
    val httpCode = errorInfo.httpCode
    val title = fallbackUnknown
    val reason =
        when (errorInfo.kind) {
            PlaybackErrorKind.NoInternet -> fallbackNoInternet
            PlaybackErrorKind.Timeout -> fallbackTimeout
            PlaybackErrorKind.NoStream -> fallbackNoStream
            PlaybackErrorKind.Decoder -> "$fallbackUnknown (code ${error.errorCode})"
            PlaybackErrorKind.Http -> "$fallbackUnknown (HTTP $httpCode)"
            PlaybackErrorKind.Unknown -> error.cause?.message?.takeIf { it.isNotBlank() }
                ?: error.message?.takeIf { it.isNotBlank() }
                ?: fallbackUnknown
        }

    val details =
        remember(error, reason, httpCode) {
            buildString {
                appendLine(reason)
                appendLine("Code: ${error.errorCode}")
                if (httpCode != null) appendLine("HTTP: $httpCode")

                val rootMessage = error.message?.trim().orEmpty()
                if (rootMessage.isNotBlank() && rootMessage != reason) {
                    appendLine()
                    appendLine("Message: $rootMessage")
                }

                var t: Throwable? = error.cause
                var depth = 0
                while (t != null && depth < 6) {
                    val name = t.javaClass.simpleName.ifBlank { t.javaClass.name }
                    val msg = t.message?.trim().orEmpty()
                    appendLine()
                    appendLine("Cause: $name${if (msg.isNotBlank()) ": $msg" else ""}")
                    t = t.cause
                    depth++
                }
            }.trim()
        }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.86f),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.92f),
                    modifier = Modifier.padding(12.dp),
                    maxLines = 12,
                    overflow = TextOverflow.Clip,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = retry,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = retryText)
                }

                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(details))
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.select_all),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = copyText)
                }
            }
        }
    }
}
