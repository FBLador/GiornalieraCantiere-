package com.idroambiente.giornalieradicantiere.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DailyReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyReminderScheduler.ACTION_DAILY_REMINDER_ALARM) {
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DailyReminderSchedulerEntryPoint::class.java
        )
        val dailyReminderScheduler = entryPoint.dailyReminderScheduler()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // Schedule next alarm immediately, then run today's check now.
                dailyReminderScheduler.syncScheduleFromPreferences()
                dailyReminderScheduler.enqueueReminderCheckNow()
            } finally {
                pendingResult.finish()
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyReminderSchedulerEntryPoint {
        fun dailyReminderScheduler(): DailyReminderScheduler
    }
}
