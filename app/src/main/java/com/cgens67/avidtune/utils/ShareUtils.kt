@file:OptIn(ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.cgens67.avidtune.constants.AppFontKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.ShareLinkFormat
import com.cgens67.avidtune.constants.ShareLinkFormatKey
import com.cgens67.avidtune.constants.UseSystemFontKey
import com.cgens67.avidtune.extensions.toEnum
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.theme.AvidTuneTheme
import com.cgens67.avidtune.ui.theme.DefaultThemeColor

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

    fun shareSong(
        context: Context,
        videoId: String,
        title: String? = null,
        positionMs: Long? = null
    ) {
        val format = context.dataStore.get(ShareLinkFormatKey).toEnum(ShareLinkFormat.AVIDTUNE)

        when (format) {
            ShareLinkFormat.AVIDTUNE -> {
                val url = buildAvidTuneUrl(videoId, positionMs)
                launchShareIntent(context, url)
            }
            ShareLinkFormat.YOUTUBE_MUSIC -> {
                val url = buildYouTubeMusicUrl(videoId, positionMs)
                launchShareIntent(context, url)
            }
            ShareLinkFormat.ASK -> {
                val avidTuneUrl = buildAvidTuneUrl(videoId, positionMs)
                val ytmUrl = buildYouTubeMusicUrl(videoId, positionMs)
                showShareFormatDialog(context, avidTuneUrl, ytmUrl)
            }
        }
    }

    private fun showShareFormatDialog(context: Context, avidTuneUrl: String, ytmUrl: String) {
        val darkThemePref = context.dataStore.get(DarkModeKey).toEnum(DarkMode.AUTO)
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val useDarkTheme = when (darkThemePref) {
            DarkMode.ON -> true
            DarkMode.OFF -> false
            DarkMode.AUTO -> isSystemDark
        }
        val pureBlack = context.dataStore.get(PureBlackKey) ?: false
        val useSystemFont = context.dataStore.get(UseSystemFontKey) ?: false
        val appFontStr = context.dataStore.get(AppFontKey) ?: ""
        val appFont = if (appFontStr.isNotEmpty()) {
            try { AppFont.valueOf(appFontStr) } catch (_: Exception) { AppFont.SYSTEM }
        } else {
            if (useSystemFont) AppFont.SYSTEM else AppFont.SF_PRO
        }

        val dialog = ComponentDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.55f)
        }

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AvidTuneTheme(
                    darkTheme = useDarkTheme,
                    pureBlack = pureBlack,
                    themeColor = DefaultThemeColor,
                    appFont = appFont
                ) {
                    ShareFormatDialogContent(
                        onSelectAvidTune = {
                            dialog.dismiss()
                            launchShareIntent(context, avidTuneUrl)
                        },
                        onSelectYouTubeMusic = {
                            dialog.dismiss()
                            launchShareIntent(context, ytmUrl)
                        },
                        onDismiss = { dialog.dismiss() }
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun ShareFormatDialogContent(
    onSelectAvidTune: () -> Unit,
    onSelectYouTubeMusic: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = stringResource(R.string.share_song_as),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Description
            Text(
                text = stringResource(R.string.share_dialog_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: AvidTune
            ShareOptionCard(
                title = stringResource(R.string.app_name),
                subtitle = "avidtune.vercel.app • " + stringResource(R.string.share_avidtune_desc),
                icon = {
                    Image(
                        painter = painterResource(R.drawable.avidtune),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                },
                onClick = onSelectAvidTune
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: YouTube Music
            ShareOptionCard(
                title = stringResource(R.string.provider_youtube_music),
                subtitle = "music.youtube.com • " + stringResource(R.string.share_ytm_desc),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF0000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = onSelectYouTubeMusic
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Cancel Button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ShareOptionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tileScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}