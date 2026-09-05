package app.vercel.Nighty3098.schedule.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/** Кнопка «Обновить» на виджете: разовый Worker + перерисовка. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Log.d("ScheduleWidget", "RefreshAction: tap -> enqueue worker + redraw from cache")
        val request = OneTimeWorkRequestBuilder<ScheduleUpdateWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "schedule-widget-refresh-once",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        // Мгновенно перерисовать из кэша, не дожидаясь сети.
        updateAppWidgetState(context, glanceId) {}
        ScheduleWidget().update(context, glanceId)
    }
}
