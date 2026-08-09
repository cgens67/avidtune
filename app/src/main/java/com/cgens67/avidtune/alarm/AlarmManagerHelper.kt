package com.cgens67.avidtune.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.cgens67.avidtune.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

@Serializable
data class AlarmState(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 8,
    val minute: Int = 0,
    val days: Set<Int> = emptySet(),
    val isEnabled: Boolean = false,
    val songId: String = "",
    val songTitle: String = "No Alarm Sound",
    val songArtist: String? = null,
    val songThumbnail: String? = null
)

object AlarmManagerHelper {
    private const val PREFS_NAME = "avidtune_alarms_prefs"
    private const val ALARMS_KEY = "alarms_list_v1"

    fun getAlarms(context: Context): List<AlarmState> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(ALARMS_KEY, null) ?: return emptyList()
        return try {
            Json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAlarms(context: Context, alarms: List<AlarmState>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ALARMS_KEY, Json.encodeToString(alarms)).apply()
        updateAllAlarms(context, alarms)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun updateAllAlarms(context: Context, alarms: List<AlarmState>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarms.forEach { alarm ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarmId", alarm.id)
                putExtra("songId", alarm.songId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, alarm.id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            if (alarm.isEnabled && alarm.songId.isNotBlank()) {
                val triggerTime = getNextAlarmTime(alarm.hour, alarm.minute, alarm.days)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        }
    }

    fun getNextAlarmTime(hour: Int, minute: Int, days: Set<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (days.isEmpty()) {
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis
        }

        for (i in 0..7) {
            val candidate = target.clone() as Calendar
            candidate.add(Calendar.DAY_OF_MONTH, i)
            if (days.contains(candidate.get(Calendar.DAY_OF_WEEK))) {
                if (candidate.after(now)) {
                    return candidate.timeInMillis
                }
            }
        }
        return target.timeInMillis
    }

    fun snoozeAlarm(context: Context, alarmId: String, songId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("songId", songId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
            Toast.makeText(context, context.getString(R.string.alarm_snoozed_toast), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {}
    }
}
