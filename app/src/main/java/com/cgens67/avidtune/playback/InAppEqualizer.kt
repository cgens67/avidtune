package com.cgens67.avidtune.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.AuxEffectInfo
import androidx.media3.exoplayer.ExoPlayer
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import timber.log.Timber

object AudioEffectManager {
    var equalizer: Equalizer? = null
    var bassBoost: BassBoost? = null
    var virtualizer: Virtualizer? = null
    var reverb: PresetReverb? = null

    var sessionId: Int = 0

    var isEnabled = false
    var bassStrength: Short = 0
    var surroundStrength: Short = 0
    var reverbPreset: Short = PresetReverb.PRESET_NONE
    var bandLevels = mutableMapOf<Short, Short>()

    fun init(newSessionId: Int, context: Context) {
        if (newSessionId <= 0) return
        try {
            // If it's a new session, release old effects
            if (sessionId != newSessionId) {
                release()
            }
            sessionId = newSessionId

            val prefs = context.getSharedPreferences("in_app_audio_effects", Context.MODE_PRIVATE)
            isEnabled = prefs.getBoolean("enabled", false)
            bassStrength = prefs.getInt("bass", 0).toShort()
            surroundStrength = prefs.getInt("surround", 0).toShort()
            reverbPreset = prefs.getInt("reverb", PresetReverb.PRESET_NONE.toInt()).toShort()

            if (equalizer == null) {
                equalizer = Equalizer(0, sessionId).apply { enabled = isEnabled }
            }
            if (bassBoost == null) {
                bassBoost = BassBoost(0, sessionId).apply {
                    enabled = isEnabled
                    if (strengthSupported) setStrength(bassStrength)
                }
            }
            if (virtualizer == null) {
                virtualizer = Virtualizer(0, sessionId).apply {
                    enabled = isEnabled
                    if (strengthSupported) setStrength(surroundStrength)
                }
            }
            if (reverb == null) {
                reverb = PresetReverb(0, sessionId).apply {
                    enabled = isEnabled
                    preset = reverbPreset
                }
            }

            equalizer?.let { eq ->
                val numBands = eq.numberOfBands
                for (i in 0 until numBands) {
                    val savedLevel = prefs.getInt("band_$i", eq.getBandLevel(i.toShort()).toInt()).toShort()
                    bandLevels[i.toShort()] = savedLevel
                    eq.setBandLevel(i.toShort(), savedLevel)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AudioEffects")
        }
    }

    fun save(context: Context) {
        context.getSharedPreferences("in_app_audio_effects", Context.MODE_PRIVATE).edit().apply {
            putBoolean("enabled", isEnabled)
            putInt("bass", bassStrength.toInt())
            putInt("surround", surroundStrength.toInt())
            putInt("reverb", reverbPreset.toInt())
            bandLevels.forEach { (band, level) ->
                putInt("band_$band", level.toInt())
            }
            apply()
        }
    }

    fun release() {
        try {
            equalizer?.release(); equalizer = null
            bassBoost?.release(); bassBoost = null
            virtualizer?.release(); virtualizer = null
            reverb?.release(); reverb = null
        } catch (e: Exception) {
            Timber.e(e, "Error releasing AudioEffects")
        }
    }

    fun applyClearVoice(context: Context) {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands
            val maxLevel = eq.bandLevelRange[1]
            for (i in 0 until numBands) {
                val freq = eq.getCenterFreq(i.toShort())
                // Boost mid-high frequencies (1kHz - 8kHz) for Clear Voice effect
                val level = if (freq in 1000000..8000000) (maxLevel * 0.6).toInt().toShort() else 0.toShort()
                eq.setBandLevel(i.toShort(), level)
                bandLevels[i.toShort()] = level
            }
        }
        save(context)
    }

    fun resetAll(context: Context, exoPlayer: ExoPlayer?) {
        isEnabled = false
        bassStrength = 0
        surroundStrength = 0
        reverbPreset = PresetReverb.PRESET_NONE
        
        equalizer?.enabled = false
        bassBoost?.enabled = false
        virtualizer?.enabled = false
        reverb?.enabled = false

        bassBoost?.setStrength(0)
        virtualizer?.setStrength(0)
        reverb?.preset = PresetReverb.PRESET_NONE
        exoPlayer?.clearAuxEffectInfo()

        equalizer?.let { eq ->
            val numBands = eq.numberOfBands
            for (i in 0 until numBands) {
                eq.setBandLevel(i.toShort(), 0.toShort())
                bandLevels[i.toShort()] = 0.toShort()
            }
        }
        save(context)
    }
    
    // Properly attach Reverb to ExoPlayer
    fun syncReverb(exoPlayer: ExoPlayer?) {
        if (isEnabled && reverbPreset != PresetReverb.PRESET_NONE) {
            reverb?.preset = reverbPreset
            reverb?.enabled = true
            reverb?.id?.let { id ->
                exoPlayer?.setAuxEffectInfo(AuxEffectInfo(id, 1f))
            }
        } else {
            reverb?.enabled = false
            exoPlayer?.clearAuxEffectInfo()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppEqualizerBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playerConnection = LocalPlayerConnection.current
    val exoPlayer = playerConnection?.player as? ExoPlayer

    // Make sure reverb is synced when the bottom sheet is opened
    LaunchedEffect(Unit) {
        AudioEffectManager.syncReverb(exoPlayer)
    }

    var enabled by remember { mutableStateOf(AudioEffectManager.isEnabled) }
    var bass by remember { mutableStateOf(AudioEffectManager.bassStrength.toFloat()) }
    var surround by remember { mutableStateOf(AudioEffectManager.surroundStrength.toFloat()) }
    var reverb by remember { mutableStateOf(AudioEffectManager.reverbPreset) }
    var updateTrigger by remember { mutableIntStateOf(0) }

    val reverbOptions = listOf(
        PresetReverb.PRESET_NONE to "None",
        PresetReverb.PRESET_SMALLROOM to "Bathroom",
        PresetReverb.PRESET_MEDIUMROOM to "Living Room",
        PresetReverb.PRESET_LARGEHALL to "Concert Hall",
        PresetReverb.PRESET_PLATE to "Cave/Plate"
    )

    // Consumes ALL vertical overscroll to prevent the sheet from dragging and snapping back
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(0f, available.y)
            }
        }
    }
    
    // Fix for horizontal elements making the sheet "snap back" or jitter
    val horizontalNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Consume all horizontal unconsumed scroll to avoid sending it to the bottom sheet pager
                return Offset(available.x, 0f)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with System Equalizer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equalizer & Effects",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(
                    onClick = {
                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffectManager.sessionId)
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "No system equalizer found", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "System Equalizer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Enable Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable Audio Effects", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Turn on built-in equalizer, bass boost and reverb", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            AudioEffectManager.isEnabled = it
                            AudioEffectManager.equalizer?.enabled = it
                            AudioEffectManager.bassBoost?.enabled = it
                            AudioEffectManager.virtualizer?.enabled = it
                            AudioEffectManager.syncReverb(exoPlayer)
                            AudioEffectManager.save(context)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Bass & Surround Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Enhancements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(16.dp))
                            
                            Text("Mega Bass", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = bass,
                                onValueChange = {
                                    bass = it
                                    AudioEffectManager.bassStrength = it.toInt().toShort()
                                    AudioEffectManager.bassBoost?.setStrength(AudioEffectManager.bassStrength)
                                },
                                onValueChangeFinished = { AudioEffectManager.save(context) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(Modifier.height(8.dp))

                            Text("Panoramic Surround", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = surround,
                                onValueChange = {
                                    surround = it
                                    AudioEffectManager.surroundStrength = it.toInt().toShort()
                                    AudioEffectManager.virtualizer?.setStrength(AudioEffectManager.surroundStrength)
                                },
                                onValueChangeFinished = { AudioEffectManager.save(context) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }

                    // Reverb Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Environment Reverb", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .nestedScroll(horizontalNestedScrollConnection) // Consumes horizontal bounds to prevent snapping
                                    .padding(top = 8.dp)
                            ) {
                                items(reverbOptions) { opt ->
                                    FilterChip(
                                        selected = reverb == opt.first,
                                        onClick = {
                                            reverb = opt.first
                                            AudioEffectManager.reverbPreset = opt.first
                                            AudioEffectManager.syncReverb(exoPlayer)
                                            AudioEffectManager.save(context)
                                        },
                                        label = { Text(opt.second) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Equalizer Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Equalizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                FilledTonalButton(
                                    onClick = {
                                        AudioEffectManager.applyClearVoice(context)
                                        updateTrigger++
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Clear Voice", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            AudioEffectManager.equalizer?.let { eq ->
                                val numBands = eq.numberOfBands
                                if (numBands > 0) {
                                    val minRange = eq.bandLevelRange[0].toFloat()
                                    val maxRange = eq.bandLevelRange[1].toFloat()

                                    for (i in 0 until numBands) {
                                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                                        val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"

                                        var bandLevel by remember(i, updateTrigger) {
                                            mutableFloatStateOf(AudioEffectManager.bandLevels[i.toShort()]?.toFloat() ?: 0f)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = String.format("%5s", "$freqLabel Hz"),
                                                modifier = Modifier.width(60.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Slider(
                                                value = bandLevel,
                                                valueRange = minRange..maxRange,
                                                onValueChange = {
                                                    bandLevel = it
                                                    val levelShort = it.toInt().toShort()
                                                    eq.setBandLevel(i.toShort(), levelShort)
                                                    AudioEffectManager.bandLevels[i.toShort()] = levelShort
                                                },
                                                onValueChangeFinished = { AudioEffectManager.save(context) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${(bandLevel / 100).toInt()} dB",
                                                modifier = Modifier.width(48.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                textAlign = TextAlign.End,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    Text("No equalizer bands available.", color = MaterialTheme.colorScheme.error)
                                }
                            } ?: Text("Equalizer not supported on this device", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    // Reset All Button
                    OutlinedButton(
                        onClick = {
                            AudioEffectManager.resetAll(context, exoPlayer)
                            enabled = false
                            bass = 0f
                            surround = 0f
                            reverb = PresetReverb.PRESET_NONE
                            updateTrigger++
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset All Settings")
                    }
                }
            }
        }
    }
}
