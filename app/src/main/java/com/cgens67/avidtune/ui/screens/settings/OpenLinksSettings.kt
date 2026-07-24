package com.cgens67.avidtune.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenLinksSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isVerified by remember { mutableStateOf(checkVerificationStatus(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isVerified = checkVerificationStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsPage(
        title = stringResource(R.string.open_supported_links),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = "Status",
            items = listOf(
                {
                    PreferenceEntry(
                        title = { Text(if (isVerified) "Links are opening in AvidTune" else "Links are not opening in AvidTune") },
                        description = if (isVerified) "AvidTune is set as the default app for supported links." else "Tap to open system settings and allow AvidTune to open supported links. Be sure to add all supported links.",
                        icon = {
                            Icon(
                                painter = painterResource(if (isVerified) R.drawable.check_circle else R.drawable.info),
                                contentDescription = null,
                                tint = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = "Supported Links",
            items = listOf(
                "music.youtube.com",
                "youtube.com",
                "youtu.be"
            ).map { domain ->
                {
                    PreferenceEntry(
                        title = { Text(domain) },
                        icon = { Icon(painterResource(R.drawable.link), null) },
                        isEnabled = false
                    )
                }
            }
        )
    }
}

private fun checkVerificationStatus(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(DomainVerificationManager::class.java)
        val userState = manager?.getDomainVerificationUserState(context.packageName)
        val domains = userState?.hostToStateMap
        domains?.any { it.value == DomainVerificationUserState.DOMAIN_STATE_VERIFIED } == true
    } else {
        true // Pre-Android 12 automatically handles links if intent filters match
    }
}
