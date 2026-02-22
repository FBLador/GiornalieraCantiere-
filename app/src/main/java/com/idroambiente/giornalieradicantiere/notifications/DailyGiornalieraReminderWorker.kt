package com.idroambiente.giornalieradicantiere.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class DailyGiornalieraReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesRepository: PreferencesRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val dailyReminderScheduler: DailyReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val forceNotification = inputData.getBoolean(KEY_FORCE_NOTIFICATION, false)
        if (forceNotification) {
            if (!ReminderNotificationHelper.hasRuntimePermission(applicationContext)) {
                return Result.success()
            }
            ReminderNotificationHelper.showReminderNotification(
                context = applicationContext,
                isTest = true
            )
            return Result.success()
        }

        val isReminderEnabled = preferencesRepository.dailyReminderEnabled.first()
        if (!isReminderEnabled) {
            dailyReminderScheduler.cancel()
            return Result.success()
        }

        // Day-of-week filter: skip notifications if today is not an enabled day
        val enabledDays = preferencesRepository.dailyReminderDays.first()
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (todayDow !in enabledDays) {
            dailyReminderScheduler.scheduleNextFromWorker()
            return Result.success()
        }

        // Mutually exclusive notifications: "da inviare" has priority
        if (ReminderNotificationHelper.hasRuntimePermission(applicationContext)) {
            val notSentCount = giornalieraRepository.countNotSent()
            if (notSentCount > 0) {
                ReminderNotificationHelper.showNotSentNotification(
                    context = applicationContext,
                    notSentCount = notSentCount
                )
                ReminderNotificationHelper.cancelReminderNotification(applicationContext)
            } else {
                val hasCompiledToday = giornalieraRepository.hasCompiledForDay(System.currentTimeMillis())
                if (!hasCompiledToday) {
                    ReminderNotificationHelper.showReminderNotification(
                        context = applicationContext,
                        isTest = false
                    )
                }
                ReminderNotificationHelper.cancelNotSentNotification(applicationContext)
            }
        }

        // Re-schedule the next daily run at the configured time.
        dailyReminderScheduler.scheduleNextFromWorker()
        return Result.success()
    }

    companion object {
        const val KEY_FORCE_NOTIFICATION = "force_notification"
    }
}

