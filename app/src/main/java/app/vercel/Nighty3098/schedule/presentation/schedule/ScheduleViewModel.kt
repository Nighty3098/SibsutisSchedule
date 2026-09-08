package app.vercel.Nighty3098.schedule.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.model.ScheduleChange
import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import app.vercel.Nighty3098.schedule.domain.repository.SettingsRepository
import java.time.LocalDate
import java.util.LinkedHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val groupQuery: String = "",
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val viewMode: ScheduleViewMode = ScheduleViewMode.DAY,
    /** Изменения последнего ручного обновления — показывает диалог. */
    val lastChanges: List<ScheduleChange>? = null,
)

/** Режим отображения расписания: по дням или неделей (Пн–Сб). */
enum class ScheduleViewMode {
    DAY,
    WEEK,
}

/**
 * Понедельник учебной недели для даты (Пн–Сб).
 * Воскресенье — выходной: для него возвращаем следующий понедельник.
 * Чистая функция — переиспользуется экраном для подзаголовка.
 */
fun weekMondayOf(date: LocalDate): LocalDate {
    val effective = if (date.dayOfWeek.value == 7) date.plusDays(1) else date
    return effective.minusDays((effective.dayOfWeek.value - 1).toLong())
}

class ScheduleViewModel(
    private val schedule: ScheduleRepository,
    private val settings: SettingsRepository,
    private val onLessonsChanged: () -> Unit = {},
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Диапазон свайпа: ±год от сегодня. Стартовая страница — сегодня. */
    val pagerDates: List<LocalDate> =
        (-PAGER_RADIUS_DAYS..PAGER_RADIUS_DAYS).map { _selectedDate.value.plusDays(it.toLong()) }
    val initialPageIndex: Int = PAGER_RADIUS_DAYS

    /**
     * Поток расписания произвольной даты для текущей группы.
     *
     * ВАЖНО: инстанс на дату создаётся один раз и кэшируется в [dayCaches].
     * Если отдавать свежесобранный Flow при каждой рекомпозиции, collectAsState
     * будет перезапускать подписку (Room-перезапрос + мигание спиннера) —
     * именно это и давало лаги свайпа. Плюс это L1-кэш в памяти: возврат
     * свайпом назад показывает значение мгновенно, без нового запроса.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun dayFlow(date: LocalDate): StateFlow<DaySchedule?> = synchronized(dayCaches) {
        dayCaches.getOrPut(date) {
            settings.groupQuery.distinctUntilChanged()
                .flatMapLatest { group ->
                    if (group.isBlank()) flowOf(null)
                    else schedule.observeDay(date, group.trim())
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }
    }

    /**
     * L1-кеш потоков дней с доступ-порядком: давно не использованные
     * даты вытесняются, чтобы за долгую сессию свайпов не накопились
     * сотни постоянных StateFlow. 24 слотов с запасом покрывают видимые
     * страницы пейджера + окно DaySelector.
     */
    private val dayCaches =
        object : LinkedHashMap<LocalDate, StateFlow<DaySchedule?>>(MAX_CACHED_DAYS, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<LocalDate, StateFlow<DaySchedule?>>,
            ): Boolean = size > MAX_CACHED_DAYS
        }

    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _viewMode = MutableStateFlow(ScheduleViewMode.DAY)
    private val _lastChanges = MutableStateFlow<List<ScheduleChange>?>(null)

    /**
     * Состояние экрана. ВНИМАНИЕ: само расписание дня в uiState сознательно
     * НЕ дублируется — каждая страница пейджера читает своё через [dayFlow],
     * а эта сборка отвечает только за навигацию/статусы. Иначе на каждую
     * смену даты было бы два подписчённых Room-запроса на один день плюс
     * кратковременный «мисматч» (новый заголовок с данными старого дня).
     */
    val uiState: StateFlow<ScheduleUiState> = combine(
        combine(_selectedDate, settings.groupQuery, _isRefreshing, _error) {
                date, group, refreshing, error,
            ->
            ScheduleUiState(
                selectedDate = date,
                groupQuery = group,
                isRefreshing = refreshing,
                error = error,
            )
        },
        _viewMode,
        _lastChanges,
    ) { base, mode, changes ->
        base.copy(viewMode = mode, lastChanges = changes)
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScheduleUiState(),
        )

    init {
        // Тихое автообновление при старте, если группа уже сохранена.
        // force=false: свежий кэш (моложе 3 ч) сеть не тронет.
        viewModelScope.launch {
            val group = settings.groupQuery.first().trim()
            if (group.isNotEmpty()) refreshInternal(group, silent = true, force = false)
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _error.value = null
    }

    fun today() = selectDate(LocalDate.now())

    fun setViewMode(mode: ScheduleViewMode) {
        _viewMode.value = mode
    }

    fun dismissChanges() {
        _lastChanges.value = null
    }

    /**
     * Понедельник учебной недели для [date] (Пн–Сб).
     * Воскресенье — выходной: в этот день показываем следующую
     * учебную неделю, а не только что прошедшую.
     */
    fun weekMonday(date: LocalDate): LocalDate = weekMondayOf(date)

    /**
     * Даты учебной недели (Пн–Сб), содержащей [date].
     * Воскресенье не показываем: пар там не бывает.
     */
    fun weekDates(date: LocalDate): List<LocalDate> {
        val monday = weekMonday(date)
        return (0 until WEEK_DAYS).map { monday.plusDays(it.toLong()) }
    }

    /** Ручное обновление по кнопке — всегда в сеть, кэш игнорируется. */
    fun refresh() {
        viewModelScope.launch {
            val group = settings.groupQuery.first().trim()
            if (group.isEmpty()) {
                _error.value = "Сначала укажи группу в Настройках"
                return@launch
            }
            refreshInternal(group, silent = false, force = true)
        }
    }

    private suspend fun refreshInternal(group: String, silent: Boolean, force: Boolean) {
        _isRefreshing.value = true
        if (!silent) _error.value = null
        schedule.refresh(group, force)
            .onSuccess { outcome ->
                _error.value = null
                // Диалог с изменениями — только после ручного обновления:
                // тихий автоапдейт при старте не должен всплывать.
                if (!silent && outcome.changes.isNotEmpty()) {
                    _lastChanges.value = outcome.changes
                }
                onLessonsChanged()
            }
            .onFailure { e ->
                // Кэш из Room продолжает показываться; ошибка — текстом.
                _error.value = e.message ?: "Не удалось обновить расписание"
            }
        _isRefreshing.value = false
    }

    fun dismissError() {
        _error.value = null
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val schedule: ScheduleRepository,
        private val settings: SettingsRepository,
        private val onLessonsChanged: () -> Unit = {},
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScheduleViewModel(schedule, settings, onLessonsChanged) as T
    }

    companion object {
        private const val PAGER_RADIUS_DAYS = 365

        /** Максимум закешированных потоков дней (LRU, доступ-порядок). */
        private const val MAX_CACHED_DAYS = 24

        /** Дней в учебной неделе для WeekView: Пн–Сб. */
        private const val WEEK_DAYS = 6
    }
}
