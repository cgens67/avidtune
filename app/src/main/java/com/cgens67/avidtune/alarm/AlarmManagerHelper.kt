package com.cgens67.avidtune.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.cgens67.avidtune.MainActivity
import com.cgens67.avidtune.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class AlarmState(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 8,
    val minute: Int = 0,
    val days: Set<Int> = emptySet(),
    val isEnabled: Boolean = true,
    val songId: String = "",
    val songTitle: String = "",
    val songArtist: String? = null,
    val songThumbnail: String? = null,
    val label: String = "",
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val fadeInSeconds: Int = 15,
    val volumePercent: Int = 100,
)

object AlarmManagerHelper {
    private const val PREFS_NAME = "avidtune_alarms_prefs"
    private const val ALARMS_KEY = "alarms_list_v1"
    private const val SNOOZE_PREFS_NAME = "avidtune_alarm_snooze_prefs"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Synchronized
    fun getAlarms(context: Context): List<AlarmState> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(ALARMS_KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<AlarmState>>(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode alarms")
            emptyList()
        }
    }

    fun getAlarm(context: Context, alarmId: String): AlarmState? =
        getAlarms(context).firstOrNull { it.id == alarmId }

    @Synchronized
    fun saveAlarms(context: Context, alarms: List<AlarmState>) {
        val previousAlarms = getAlarms(context)
        val newAlarmIds = alarms.mapTo(HashSet()) { it.id }

        // Cancel alarms that were deleted before updating the remaining ones.
        previousAlarms.forEach { old ->
            if (old.id !in newAlarmIds) {
                cancelAlarm(context, old.id)
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ALARMS_KEY, json.encodeToString(alarms)).apply()
        updateAllAlarms(context, alarms)
    }

    fun deleteAlarm(context: Context, alarmId: String) {
        val updated = getAlarms(context).filterNot { it.id == alarmId }
        saveAlarms(context, updated)
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(createAlarmPendingIntent(context, alarmId, isSnooze = false))
        alarmManager.cancel(createAlarmPendingIntent(context, alarmId, isSnooze = true))
        clearSnoozedUntil(context, alarmId)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun updateAllAlarms(context: Context, alarms: List<AlarmState>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarms.forEach { alarm ->
            val mainIntent = createAlarmPendingIntent(context, alarm.id, isSnooze = false)
            alarmManager.cancel(mainIntent)

            if (alarm.isEnabled) {
                val triggerTime = getNextAlarmTime(alarm.hour, alarm.minute, alarm.days)
                scheduleExactOrFallback(context, alarmManager, triggerTime, mainIntent)
            } else {
                alarmManager.cancel(createAlarmPendingIntent(context, alarm.id, isSnooze = true))
                clearSnoozedUntil(context, alarm.id)
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun rescheduleNextOccurrence(context: Context, alarmId: String) {
        val alarm = getAlarm(context, alarmId) ?: return
        if (!alarm.isEnabled || alarm.days.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val mainIntent = createAlarmPendingIntent(context, alarm.id, isSnooze = false)
        // Advance baseline by 65 seconds so an early-firing alarm cannot re-trigger within the same minute.
        val triggerTime = getNextAlarmTime(
            hour = alarm.hour,
            minute = alarm.minute,
            days = alarm.days,
            fromTimeMs = System.currentTimeMillis() + 65_000L,
        )
        scheduleExactOrFallback(context, alarmManager, triggerTime, mainIntent)
    }

    fun markOneTimeAlarmDisabled(context: Context, alarmId: String) {
        val alarms = getAlarms(context).toMutableList()
        val index = alarms.indexOfFirst { it.id == alarmId }
        if (index != -1 && alarms[index].days.isEmpty()) {
            alarms[index] = alarms[index].copy(isEnabled = false)
            saveAlarms(context, alarms)
        } else {
            clearSnoozedUntil(context, alarmId)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun snoozeAlarm(context: Context, alarm: AlarmState) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = createAlarmPendingIntent(context, alarm.id, isSnooze = true)
        val snoozeDurationMs = alarm.snoozeMinutes.coerceIn(1, 60) * 60_000L
        val snoozeTime = System.currentTimeMillis() + snoozeDurationMs

        scheduleExactOrFallback(context, alarmManager, snoozeTime, snoozeIntent)
        setSnoozedUntil(context, alarm.id, snoozeTime)

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(snoozeTime))
        Toast.makeText(
            context,
            context.getString(R.string.alarm_snoozed_toast) + " ($timeText)",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun getSnoozedUntil(context: Context, alarmId: String): Long? {
        val prefs = context.getSharedPreferences(SNOOZE_PREFS_NAME, Context.MODE_PRIVATE)
        val ts = prefs.getLong(alarmId, -1L)
        return if (ts > System.currentTimeMillis()) ts else {
            if (ts != -1L) prefs.edit().remove(alarmId).apply()
            null
        }
    }

    fun setSnoozedUntil(context: Context, alarmId: String, triggerTimeMs: Long) {
        context.getSharedPreferences(SNOOZE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(alarmId, triggerTimeMs)
            .apply()
    }

    fun clearSnoozedUntil(context: Context, alarmId: String) {
        context.getSharedPreferences(SNOOZE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(alarmId)
            .apply()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExactOrFallback(
        context: Context,
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent,
    ) {
        try {
            if (canScheduleExactAlarms(context)) {
                val showIntent = createShowAlarmSettingsIntent(context)
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm permission denied, falling back to inexact")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun createAlarmPendingIntent(
        context: Context,
        alarmId: String,
        isSnooze: Boolean,
    ): PendingIntent {
        val schemePath = if (isSnooze) "snooze" else "main"
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            data = "avidtune://alarm/$schemePath/$alarmId".toUri()
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val requestCode = "$schemePath:$alarmId".hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createShowAlarmSettingsIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0x414C524D,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getNextAlarmTime(
        hour: Int,
        minute: Int,
        days: Set<Int>,
        fromTimeMs: Long = System.currentTimeMillis(),
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromTimeMs }
        val target = Calendar.getInstance().apply {
            timeInMillis = fromTimeMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val validDays = days.filter { it in Calendar.SUNDAY..Calendar.SATURDAY }.toSet()
        if (validDays.isEmpty()) {
            if (!target.after(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis
        }

        for (i in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, i)
            }
            if (validDays.contains(candidate.get(Calendar.DAY_OF_WEEK)) && candidate.after(now)) {
                return candidate.timeInMillis
            }
        }

        target.add(Calendar.DAY_OF_MONTH, 1)
        return target.timeInMillis
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.canUseFullScreenIntent()
        } else {
            true
        }
    }

    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    const val EXTRA_ALARM_ID = "alarmId"
    const val EXTRA_IS_SNOOZE = "isSnooze"
}