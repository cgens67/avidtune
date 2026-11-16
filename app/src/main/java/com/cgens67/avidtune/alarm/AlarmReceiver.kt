package com.cgens67.avidtune.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmManagerHelper.EXTRA_ALARM_ID) ?: return
        val isSnooze = intent.getBooleanExtra(AlarmManagerHelper.EXTRA_IS_SNOOZE, false)

        val alarm = AlarmManagerHelper.getAlarm(context, alarmId)
        if (alarm == null) {
            Timber.w("AlarmReceiver: Alarm $alarmId no longer exists, ignoring")
            return
        }

        if (!isSnooze && !alarm.isEnabled) {
            Timber.w("AlarmReceiver: Alarm $alarmId is disabled, ignoring")
            return
        }

        // Acquire a brief WakeLock until AlarmService enters foreground and takes over.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AvidTune:AlarmReceiverWakeLock"
        ).apply {
            acquire(15_000L)
        }

        try {
            if (isSnooze) {
                AlarmManagerHelper.clearSnoozedUntil(context, alarmId)
            } else if (alarm.days.isNotEmpty()) {
                // Immediately schedule the next repeating occurrence so it works even if dismissed from notification.
                AlarmManagerHelper.rescheduleNextOccurrence(context, alarmId)
            }

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_ALARM
                putExtra(AlarmManagerHelper.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmManagerHelper.EXTRA_IS_SNOOZE, isSnooze)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start AlarmService for $alarmId")
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.cgens67.avidtune.ACTION_FIRE_ALARM"
    }
}

class AlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Timber.i("AlarmBootReceiver: Rescheduling alarms after $action")
            val alarms = AlarmManagerHelper.getAlarms(context)
            AlarmManagerHelper.updateAllAlarms(context, alarms)
        }
    }
}