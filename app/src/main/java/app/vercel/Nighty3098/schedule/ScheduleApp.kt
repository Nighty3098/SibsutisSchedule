package app.vercel.Nighty3098.schedule

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.vercel.Nighty3098.schedule.data.calendar.CalendarSyncManager
import app.vercel.Nighty3098.schedule.data.datastore.SecureCredentialsStore
import app.vercel.Nighty3098.schedule.data.datastore.SettingsDataStore
import app.vercel.Nighty3098.schedule.data.local.ScheduleDatabase
import app.vercel.Nighty3098.schedule.data.network.AuthManager
import app.vercel.Nighty3098.schedule.data.network.GroupResolver
import app.vercel.Nighty3098.schedule.data.network.ScheduleFetcher
import app.vercel.Nighty3098.schedule.data.network.SibsutisHttpClient
import app.vercel.Nighty3098.schedule.data.repository.ScheduleRepositoryImpl
import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import app.vercel.Nighty3098.schedule.domain.usecase.ObserveDayScheduleUseCase
import app.vercel.Nighty3098.schedule.domain.usecase.RefreshScheduleUseCase
import app.vercel.Nighty3098.schedule.util.ensureScheduleChannels
import app.vercel.Nighty3098.schedule.widget.ScheduleUpdateWorker
import app.vercel.Nighty3098.schedule.widget.ScheduleWidget
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ручной DI-контейнер (без Hilt — меньше магии, проще для учебного проекта).
 * UseCases/ViewModel получают зависимости отсюда.
 */
class ScheduleApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ensureScheduleChannels()
        container = AppContainer(applicationContext)
        container.schedulePeriodicRefresh()
        container.observeThemeForWidgets()
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val okHttp by lazy { SibsutisHttpClient.create() }

    private val secureCredentials by lazy { SecureCredentialsStore(appContext) }

    val settings: SettingsDataStore by lazy {
        SettingsDataStore(appContext, secureCredentials).also {
            // Прогрев шифрованного хранилища + миграция из открытого DataStore.
            appScope.launch { secureCredentials.ensureReady() }
        }
    }

    private val database: ScheduleDatabase by lazy {
        Room.databaseBuilder(appContext, ScheduleDatabase::class.java, "schedule.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val authManager by lazy { AuthManager(okHttp) }
    private val groupResolver by lazy { GroupResolver(okHttp) }
    private val fetcher by lazy { ScheduleFetcher(okHttp) }

    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepositoryImpl(
            dao = database.scheduleDao(),
            fetcher = fetcher,
            resolver = groupResolver,
            auth = authManager,
            settings = settings,
        )
    }

    val observeDaySchedule by lazy {
        ObserveDayScheduleUseCase(scheduleRepository, settings)
    }
    val refreshSchedule by lazy {
        RefreshScheduleUseCase(scheduleRepository, settings)
    }

    val calendarSync by lazy {
        CalendarSyncManager(appContext, scheduleRepository, settings)
    }

    /** Фон: обновление кэша + виджета каждые 3 часа при наличии сети. */
    fun schedulePeriodicRefresh() {        val request = PeriodicWorkRequestBuilder<ScheduleUpdateWorker>(3, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            ScheduleUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Glance сам за DataStore не следит: каждая смена темы (откуда бы она
     * ни пришла) вручную дёргает перерисовку всех инстансов виджета.
     * Работает независимо от Activity — даже если настройки поменяли фоном.
     */
    fun observeThemeForWidgets() {
        appScope.launch {
            settings.themeMode.collect { mode ->
                try {
                    Log.d("ScheduleWidget", "theme -> $mode, updateAll widgets")
                    ScheduleWidget().updateAll(appContext)
                } catch (e: Exception) {
                    Log.w("ScheduleWidget", "updateAll failed: ${e.message}")
                }
            }
        }
    }
}

/** Доступ к контейнеру из Activity / Worker / Glance. */
fun Context.appContainer(): AppContainer =
    (applicationContext as ScheduleApp).container
