package com.idroambiente.giornalieradicantiere.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.idroambiente.giornalieradicantiere.MainActivity

object ReminderNotificationHelper {

    fun hasRuntimePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areSystemNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isReminderChannelBlocked(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(CHANNEL_ID) ?: return false
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }

    fun showReminderNotification(context: Context, isTest: Boolean) {
        if (!hasRuntimePermission(context)) return
        createNotificationChannelIfNeeded(context)
        if (!areSystemNotificationsEnabled(context)) return
        if (isReminderChannelBlocked(context)) return

        val title = if (isTest) "Test notifica giornaliera" else "Promemoria giornaliera"
        val body = if (isTest) {
            "Questa e una notifica di test."
        } else {
            "Non risulta una giornaliera compilata oggi. Apri l'app per completarla."
        }

        val notificationId = if (isTest) TEST_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID
        showNotification(
            context = context,
            title = title,
            body = body,
            notificationId = notificationId
        )
    }

    fun cancelReminderNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(REMINDER_NOTIFICATION_ID)
    }

    fun cancelNotSentNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOT_SENT_NOTIFICATION_ID)
    }

    fun showNotSentNotification(context: Context, notSentCount: Int) {
        if (notSentCount <= 0) return
        if (!hasRuntimePermission(context)) return
        createNotificationChannelIfNeeded(context)
        if (!areSystemNotificationsEnabled(context)) return
        if (isReminderChannelBlocked(context)) return

        val title = "Giornaliere da inviare"
        val body = if (notSentCount == 1) {
            "Hai 1 giornaliera in bozza o completata ma non inviata."
        } else {
            "Hai $notSentCount giornaliere in bozza o completate ma non inviate."
        }

        showNotification(
            context = context,
            title = title,
            body = body,
            notificationId = NOT_SENT_NOTIFICATION_ID
        )
    }

    private fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        // Da Android 13 l'utente può negare il permesso delle notifiche: in quel caso non mostriamo nulla
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun createNotificationChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    const val CHANNEL_ID = "giornaliera_reminder_channel"
    private const val CHANNEL_NAME = "Promemoria giornaliera"
    private const val CHANNEL_DESCRIPTION = "Notifiche giornaliere per compilare la giornaliera"
    private const val REMINDER_NOTIFICATION_ID = 1001
    private const val TEST_NOTIFICATION_ID = 1002
    private const val NOT_SENT_NOTIFICATION_ID = 1003
}
