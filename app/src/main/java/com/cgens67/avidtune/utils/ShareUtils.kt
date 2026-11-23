@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.cgens67.avidtune.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.cgens67.avidtune.constants.AppFontKey
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.DynamicThemeKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.ShareLinkFormat
import com.cgens67.avidtune.constants.ShareLinkFormatKey
import com.cgens67.avidtune.constants.UseSystemFontKey
import com.cgens67.avidtune.extensions.toEnum
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.screens.settings.ThemePalettes
import com.cgens67.avidtune.ui.theme.AvidTuneTheme
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec

object ShareUtils {
    const val AVIDTUNE_BASE_URL = "https://avidtune.vercel.app"

    fun buildAvidTuneUrl(videoId: String, positionMs: Long? = null): String {
        val seconds = positionMs?.let { it / 1000 }?.takeIf { it > 0 }
        return if (seconds != null) {
            "$AVIDTUNE_BASE_URL/$videoId/$seconds"
        } else {
            "$AVIDTUNE_BASE_URL/$videoId"
        }
    }

    fun buildYouTubeMusicUrl(videoId: String, positionMs: Long? = null): String {
        val seconds = positionMs?.let { it / 1000 }?.takeIf { it > 0 }
        return if (seconds != null) {
            "https://music.youtube.com/watch?v=$videoId&t=$seconds"
        } else {
            "https://music.youtube.com/watch?v=$videoId"
        }
    }

    fun buildSongShareUrl(videoId: String, positionMs: Long? = null): String {
        return buildAvidTuneUrl(videoId, positionMs)
    }

    fun buildSongShareUrl(context: Context, videoId: String, positionMs: Long? = null): String {
        val format = context.dataStore.get(ShareLinkFormatKey).toEnum(ShareLinkFormat.AVIDTUNE)
        return when (format) {
            ShareLinkFormat.AVIDTUNE, ShareLinkFormat.ASK -> buildAvidTuneUrl(videoId, positionMs)
            ShareLinkFormat.YOUTUBE_MUSIC -> buildYouTubeMusicUrl(videoId, positionMs)
        }
    }

    private fun launchShareIntent(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    private fun copyUrl(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Song Link", url))
        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
    }

    fun shareSong(
        context: Context,
        videoId: String,
        title: String? = null,
        positionMs: Long? = null,
    ) {
        val format = context.dataStore.get(ShareLinkFormatKey).toEnum(ShareLinkFormat.AVIDTUNE)

        when (format) {
            ShareLinkFormat.AVIDTUNE -> launchShareIntent(context, buildAvidTuneUrl(videoId, positionMs))
            ShareLinkFormat.YOUTUBE_MUSIC -> launchShareIntent(context, buildYouTubeMusicUrl(videoId, positionMs))
            ShareLinkFormat.ASK -> showShareFormatDialog(
                context = context,
                title = title,
                positionMs = positionMs,
                avidTuneUrl = buildAvidTuneUrl(videoId, positionMs),
                ytmUrl = buildYouTubeMusicUrl(videoId, positionMs),
            )
        }
    }

    private fun showShareFormatDialog(
        context: Context,
        title: String?,
        positionMs: Long?,
        avidTuneUrl: String,
        ytmUrl: String,
    ) {
        val darkThemePref = context.dataStore.get(DarkModeKey).toEnum(DarkMode.AUTO)
        val isSystemDark =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val useDarkTheme = when (darkThemePref) {
            DarkMode.ON -> true
            DarkMode.OFF -> false
            DarkMode.AUTO -> isSystemDark
        }
        val pureBlack = context.dataStore.get(PureBlackKey) ?: false
        val dynamicTheme = context.dataStore.get(DynamicThemeKey) ?: true
        val customThemeColor = context.dataStore.get(CustomThemeColorKey) ?: ThemePalettes.Default.id
        val useSystemFont = context.dataStore.get(UseSystemFontKey) ?: false
        val appFontStr = context.dataStore.get(AppFontKey) ?: ""
        val appFont = if (appFontStr.isNotEmpty()) {
            runCatching { AppFont.valueOf(appFontStr) }.getOrDefault(AppFont.SYSTEM)
        } else {
            if (useSystemFont) AppFont.SYSTEM else AppFont.SF_PRO
        }

        val seedPalette: ThemeSeedPalette?
        val themeColor: Color
        if (dynamicTheme) {
            seedPalette = null
            themeColor = DefaultThemeColor
        } else {
            val decoded = ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)
            if (decoded != null) {
                seedPalette = decoded
                themeColor = decoded.primary
            } else {
                val p = ThemePalettes.findById(customThemeColor, useDarkTheme)
                    ?: ThemePalettes.findByPrimaryColor(customThemeColor, useDarkTheme)
                    ?: ThemePalettes.Default
                seedPalette = ThemeSeedPalette(p.primary, p.secondary, p.tertiary, p.neutral)
                themeColor = p.primary
            }
        }

        val dialog = ComponentDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AvidTuneTheme(
                    darkTheme = useDarkTheme,
                    pureBlack = pureBlack,
                    themeColor = themeColor,
                    seedPalette = seedPalette,
                    appFont = appFont,
                ) {
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme,
                        typography = MaterialTheme.typography,
                        shapes = MaterialTheme.shapes,
                        motionScheme = MotionScheme.expressive(),
                    ) {
                        ShareFormatDialogContent(
                            songTitle = title,
                            positionMs = positionMs,
                            avidTuneUrl = avidTuneUrl,
                            ytmUrl = ytmUrl,
                            onShare = { url ->
                                dialog.dismiss()
                                launchShareIntent(context, url)
                            },
                            onCopy = { url ->
                                copyUrl(context, url)
                                dialog.dismiss()
                            },
                            onDismiss = { dialog.dismiss() },
                        )
                    }
                }
            }
        }

        dialog.setContentView(composeView)
        runCatching { dialog.show() }
    }
}

@Composable
private fun ShareFormatDialogContent(
    songTitle: String?,
    positionMs: Long?,
    avidTuneUrl: String,
    ytmUrl: String,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeStampLabel = remember(positionMs) {
        positionMs?.takeIf { it >= 1000L }?.let { makeTimeString(it) }
    }

    val listColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.share_song_as),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                )

                if (!songTitle.isNullOrBlank() || !timeStampLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!songTitle.isNullOrBlank()) {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AlertDialogDefaults.textContentColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (!timeStampLabel.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.schedule),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = timeStampLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    SegmentedListItem(
                        onClick = { onShare(avidTuneUrl) },
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        colors = listColors,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(MaterialShapes.Cookie9Sided.toShape())
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.avidtune),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        supportingContent = {
                            Text(
                                text = avidTuneUrl.removePrefix("https://"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            FilledTonalIconButton(
                                onClick = { onCopy(avidTuneUrl) },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy),
                                    contentDescription = stringResource(R.string.action_copy),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        content = {
                            Text(
                                text = stringResource(R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )

                    SegmentedListItem(
                        onClick = { onShare(ytmUrl) },
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                        colors = listColors,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(MaterialShapes.Circle.toShape())
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        supportingContent = {
                            Text(
                                text = ytmUrl.removePrefix("https://"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            FilledTonalIconButton(
                                onClick = { onCopy(ytmUrl) },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy),
                                    contentDescription = stringResource(R.string.action_copy),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        content = {
                            Text(
                                text = stringResource(R.string.provider_youtube_music),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}