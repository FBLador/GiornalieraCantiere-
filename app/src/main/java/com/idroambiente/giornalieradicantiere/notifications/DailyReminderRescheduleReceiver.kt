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

class DailyReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in RESCHEDULE_ACTIONS) {
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
                dailyReminderScheduler.syncScheduleFromPreferences()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyReminderSchedulerEntryPoint {
        fun dailyReminderScheduler(): DailyReminderScheduler
    }
}
