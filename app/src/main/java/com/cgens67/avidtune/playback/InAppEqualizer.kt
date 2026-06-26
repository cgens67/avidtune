package com.cgens67.avidtune.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import timber.log.Timber

object AudioEffectManager {
    var equalizer: Equalizer? = null
    var bassBoost: BassBoost? = null
    var virtualizer: Virtualizer? = null
    var reverb: PresetReverb? = null

    var isEnabled = false
    var bassStrength: Short = 0
    var surroundStrength: Short = 0
    var reverbPreset: Short = PresetReverb.PRESET_NONE
    var bandLevels = mutableMapOf<Short, Short>()

    fun init(sessionId: Int, context: Context) {
        release()
        if (sessionId <= 0) return
        try {
            val prefs = context.getSharedPreferences("in_app_audio_effects", Context.MODE_PRIVATE)
            isEnabled = prefs.getBoolean("enabled", false)
            bassStrength = prefs.getInt("bass", 0).toShort()
            surroundStrength = prefs.getInt("surround", 0).toShort()
            reverbPreset = prefs.getInt("reverb", PresetReverb.PRESET_NONE.toInt()).toShort()

            equalizer = Equalizer(0, sessionId).apply { enabled = isEnabled }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) setStrength(bassStrength)
            }
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) setStrength(surroundStrength)
            }
            reverb = PresetReverb(0, sessionId).apply {
                enabled = isEnabled
                preset = reverbPreset
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppEqualizerBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Audio Effects & Equalizer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Enable Effects", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        AudioEffectManager.isEnabled = it
                        AudioEffectManager.equalizer?.enabled = it
                        AudioEffectManager.bassBoost?.enabled = it
                        AudioEffectManager.virtualizer?.enabled = it
                        AudioEffectManager.reverb?.enabled = it
                        AudioEffectManager.save(context)
                    }
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))

                    Text("Mega Bass", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = bass,
                        onValueChange = {
                            bass = it
                            AudioEffectManager.bassStrength = it.toInt().toShort()
                            AudioEffectManager.bassBoost?.setStrength(AudioEffectManager.bassStrength)
                        },
                        onValueChangeFinished = { AudioEffectManager.save(context) },
                        valueRange = 0f..1000f
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Panoramic Surround", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = surround,
                        onValueChange = {
                            surround = it
                            AudioEffectManager.surroundStrength = it.toInt().toShort()
                            AudioEffectManager.virtualizer?.setStrength(AudioEffectManager.surroundStrength)
                        },
                        onValueChangeFinished = { AudioEffectManager.save(context) },
                        valueRange = 0f..1000f
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Environment Reverb", style = MaterialTheme.typography.titleMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(reverbOptions) { opt ->
                            FilterChip(
                                selected = reverb == opt.first,
                                onClick = {
                                    reverb = opt.first
                                    AudioEffectManager.reverbPreset = opt.first
                                    AudioEffectManager.reverb?.preset = opt.first
                                    AudioEffectManager.save(context)
                                },
                                label = { Text(opt.second) }
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Equalizer", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = {
                            AudioEffectManager.applyClearVoice(context)
                            updateTrigger++
                        }) {
                            Text("Clear Voice")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    AudioEffectManager.equalizer?.let { eq ->
                        val numBands = eq.numberOfBands
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
                                    text = "$freqLabel Hz",
                                    modifier = Modifier.width(60.dp),
                                    style = MaterialTheme.typography.bodyMedium
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
                            }
                        }
                    } ?: Text("Equalizer not supported on this device", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
