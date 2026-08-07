package com.cgens67.avidtune.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.cgens67.avidtune.R
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.playback.MusicService
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.theme.AvidTuneTheme
import com.cgens67.avidtune.utils.isInternetAvailable
import com.cgens67.innertube.models.WatchEndpoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var database: MusicDatabase

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var songId: String? = null
    private var alarmId: String? = null
    
    private var fallbackPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(this@AlarmActivity, service, database, lifecycleScope)
                
                songId?.let { id ->
                    lifecycleScope.launch {
                        try {
                            val dbSong = database.song(id).firstOrNull()
                            val hasInternet = isInternetAvailable(this@AlarmActivity)

                            if (!hasInternet && dbSong == null) {
                                playFallbackRingtone()
                                return@launch
                            }

                            // Fade in volume over 20 seconds
                            playerConnection?.player?.volume = 0f
                            launch {
                                for (i in 1..20) {
                                    playerConnection?.player?.volume = i / 20f
                                    delay(1000)
                                }
                            }

                            if (dbSong != null) {
                                playerConnection?.playQueue(ListQueue("Alarm", listOf(dbSong.toMediaItem())))
                            } else {
                                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = id)))
                            }
                        } catch (e: Exception) {
                            playFallbackRingtone()
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songId = intent.getStringExtra("songId")
        alarmId = intent.getStringExtra("alarmId")

        // Start Vibration Pattern: Pause 0, vibrate 500, pause 500 (repeat 0 means infinite loop)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
        }

        // Turn on the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        startService(Intent(this, MusicService::class.java))
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            AvidTuneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.schedule),
                            contentDescription = "Alarm",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Wake Up!",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (alarmId != null && songId != null) {
                                        AlarmManagerHelper.snoozeAlarm(this@AlarmActivity, alarmId!!, songId!!)
                                    }
                                    stopAlarm()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                            ) {
                                Text("Snooze (10m)", style = MaterialTheme.typography.titleMedium)
                            }
                            
                            Button(
                                onClick = {
                                    handleTurnOffLogic()
                                    stopAlarm()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                            ) {
                                Text("Stop", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun playFallbackRingtone() {
        if (fallbackPlayer?.isPlaying == true) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            fallbackPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTurnOffLogic() {
        if (alarmId == null) return
        val alarms = AlarmManagerHelper.getAlarms(this).toMutableList()
        val index = alarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val alarm = alarms[index]
            // Disable one-time alarms automatically
            if (alarm.days.isEmpty()) {
                alarms[index] = alarm.copy(isEnabled = false)
                AlarmManagerHelper.saveAlarms(this, alarms)
            } else {
                // Repeating alarm naturally rolls over, just re-save to ensure it's re-scheduled properly
                AlarmManagerHelper.saveAlarms(this, alarms)
            }
        }
    }

    private fun stopAlarm() {
        playerConnection?.player?.pause()
        fallbackPlayer?.stop()
        fallbackPlayer?.release()
        vibrator?.cancel()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alarmId?.let { notificationManager.cancel(it.hashCode()) }
        
        finish()
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        fallbackPlayer?.release()
        vibrator?.cancel()
        super.onDestroy()
    }
}
