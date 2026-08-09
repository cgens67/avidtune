package com.cgens67.avidtune.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
                                return@launch
                            }

                            AlarmAudioFallback.stop()
                            playerConnection?.player?.volume = 1f

                            if (dbSong != null) {
                                playerConnection?.playQueue(ListQueue("Alarm", listOf(dbSong.toMediaItem())))
                            } else {
                                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = id)))
                            }
                        } catch (e: Exception) {
                            // Fallback remains active if exception occurs
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
                            contentDescription = stringResource(R.string.alarm),
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(R.string.alarm_wake_up),
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
                                Text(stringResource(R.string.alarm_snooze), style = MaterialTheme.typography.titleMedium)
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
                                Text(stringResource(R.string.alarm_stop), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleTurnOffLogic() {
        if (alarmId == null) return
        val alarms = AlarmManagerHelper.getAlarms(this).toMutableList()
        val index = alarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val alarm = alarms[index]
            if (alarm.days.isEmpty()) {
                alarms[index] = alarm.copy(isEnabled = false)
                AlarmManagerHelper.saveAlarms(this, alarms)
            } else {
                AlarmManagerHelper.saveAlarms(this, alarms)
            }
        }
    }

    private fun stopAlarm() {
        playerConnection?.player?.pause()
        AlarmAudioFallback.stop()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alarmId?.let { notificationManager.cancel(it.hashCode()) }
        
        finish()
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        AlarmAudioFallback.stop()
        super.onDestroy()
    }
}
