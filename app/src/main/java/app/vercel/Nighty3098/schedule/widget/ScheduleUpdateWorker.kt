package app.vercel.Nighty3098.schedule.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.vercel.Nighty3098.schedule.appContainer
import kotlinx.coroutines.flow.first

/**
 * Фоновое обновление: тянет расписание группы с сайта в Room,
 * затем перерисовывает все виджеты.
 *
 * Периодический запуск (каждые 3 часа, только с сетью) планируется
 * в [app.vercel.Nighty3098.schedule.AppContainer.schedulePeriodicRefresh].
 */
class ScheduleUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = applicationContext.appContainer()
            val group = container.settings.groupQuery.first().trim()
            if (group.isNotEmpty()) {
                // Ошибки сети не роняем: кэш и так показывается.
                container.scheduleRepository.refresh(group)
            }
            // Календарь пересинхронизируется, только если включён в настройках.
            runCatching { container.calendarSync.syncIfEnabled() }
            ScheduleWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "schedule-periodic-refresh"
    }
}
