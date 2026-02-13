package com.idroambiente.giornalieradicantiere.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val preferencesRepository: PreferencesRepository
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun syncScheduleFromPreferences() {
        cancelLegacyWorkRequests()

        val enabled = preferencesRepository.dailyReminderEnabled.first()
        if (!enabled) {
            cancelAlarm()
            return
        }

        val hour = preferencesRepository.dailyReminderHour.first()
        val minute = preferencesRepository.dailyReminderMinute.first()
        scheduleAlarm(hour = hour, minute = minute)
    }

    suspend fun scheduleNextFromWorker() {
        syncScheduleFromPreferences()
    }

    fun enqueueReminderCheckNow() {
        val request = OneTimeWorkRequestBuilder<DailyGiornalieraReminderWorker>()
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            REMINDER_CHECK_NOW_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel() {
        cancelAlarm()
        cancelLegacyWorkRequests()
    }

    fun scheduleTestReminder(delaySeconds: Long = DEFAULT_TEST_DELAY_SECONDS) {
        val request = OneTimeWorkRequestBuilder<DailyGiornalieraReminderWorker>()
            .setInitialDelay(delaySeconds.coerceAtLeast(1), TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    DailyGiornalieraReminderWorker.KEY_FORCE_NOTIFICATION to true
                )
            )
            .addTag(TEST_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            TEST_UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleAlarm(hour: Int, minute: Int) {
        val triggerAtMillis = computeNextTriggerAtMillis(hour, minute)
        val pendingIntent = reminderAlarmPendingIntent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            return
        }

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun reminderAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyReminderAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER_ALARM
        }

        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAlarm() {
        alarmManager.cancel(reminderAlarmPendingIntent())
    }

    private fun cancelLegacyWorkRequests() {
        workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(LEGACY_ONE_TIME_WORK_NAME)
    }

    private fun computeNextTriggerAtMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return nextRun.timeInMillis
    }

    companion object {
        const val ACTION_DAILY_REMINDER_ALARM =
            "com.idroambiente.giornalieradicantiere.action.DAILY_REMINDER_ALARM"

        private const val ALARM_REQUEST_CODE = 14001
        private const val LEGACY_PERIODIC_WORK_NAME = "daily_giornaliera_reminder_work"
        private const val LEGACY_ONE_TIME_WORK_NAME = "daily_giornaliera_reminder_next_work"
        private const val REMINDER_CHECK_NOW_WORK_NAME = "daily_giornaliera_reminder_now_work"

        const val WORK_TAG = "daily_giornaliera_reminder"
        const val TEST_UNIQUE_WORK_NAME = "daily_giornaliera_reminder_test_work"
        const val TEST_WORK_TAG = "daily_giornaliera_reminder_test"
        const val DEFAULT_TEST_DELAY_SECONDS = 20L
    }
}
