package app.vercel.Nighty3098.schedule.util

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
import app.vercel.Nighty3098.schedule.MainActivity
import app.vercel.Nighty3098.schedule.R
import app.vercel.Nighty3098.schedule.domain.model.ScheduleChange
import app.vercel.Nighty3098.schedule.domain.model.describe

private const val CHANNEL_CHANGES = "schedule_changes"
private const val NOTIFICATION_CHANGES_ID = 1001

/** Максимум строк изменений в уведомлении, остальное — счётчиком. */
private const val MAX_NOTIFICATION_LINES = 6

/** Канал «Изменения расписания». Вызывать в Application.onCreate. */
fun Context.ensureScheduleChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_CHANGES) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHANGES,
                "Изменения расписания",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Уведомления, когда в расписании появляются изменения"
            },
        )
    }
}

private fun Context.canPostNotifications(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

/**
 * Уведомление о найденных изменениях (из фонового воркера).
 * Без разрешения молча выходим — воркер не умеет его запрашивать.
 */
fun Context.notifyScheduleChanges(changes: List<ScheduleChange>) {
    if (changes.isEmpty() || !canPostNotifications()) return
    val lines = changes.take(MAX_NOTIFICATION_LINES).map { it.describe() }
    val overflow = changes.size - lines.size
    val bigText = buildString {
        append(lines.joinToString("\n"))
        if (overflow > 0) append("\n…и ещё $overflow")
    }
    val openApp = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(this, CHANNEL_CHANGES)
        .setSmallIcon(R.drawable.ic_stat_schedule)
        .setContentTitle(
            if (changes.size == 1) "Расписание изменилось" else "Расписание изменилось: ${changes.size}",
        )
        .setContentText(lines.first())
        .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        .setContentIntent(openApp)
        .setAutoCancel(true)
        .build()
    runCatching {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_CHANGES_ID, notification)
    }
}
