package com.cgens67.avidtune.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

object AlarmManagerHelper {
    private const val ALARM_REQUEST_CODE = 9999

    @SuppressLint("ScheduleExactAlarm")
    fun setAlarm(context: Context, timeInMillis: Long, songId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("songId", songId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var triggerTime = timeInMillis
        // If the selected time is in the past, schedule it for the next day
        if (triggerTime <= System.currentTimeMillis()) {
            triggerTime += 24 * 60 * 60 * 1000
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        // Save State
        context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit().apply {
            putBoolean("alarm_enabled", true)
            putLong("alarm_time", triggerTime)
            putString("alarm_song_id", songId)
            apply()
        }
        
        Toast.makeText(context, "Alarm set successfully!", Toast.LENGTH_SHORT).show()
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("alarm_enabled", false)
            .apply()
            
        Toast.makeText(context, "Alarm cancelled", Toast.LENGTH_SHORT).show()
    }

    fun snoozeAlarm(context: Context, songId: String) {
        // Snooze for 10 minutes
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000)
        setAlarm(context, snoozeTime, songId)
        Toast.makeText(context, "Alarm snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
    }
}
