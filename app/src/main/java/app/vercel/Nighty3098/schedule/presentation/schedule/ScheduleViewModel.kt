package app.vercel.Nighty3098.schedule.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import app.vercel.Nighty3098.schedule.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.time.LocalDate

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val groupQuery: String = "",
    val day: DaySchedule? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

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

    private val dayCaches = mutableMapOf<LocalDate, StateFlow<DaySchedule?>>()

    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayFlow: Flow<DaySchedule?> =
        combine(_selectedDate, settings.groupQuery.distinctUntilChanged()) { date, group ->
            date to group
        }.flatMapLatest { (date, group) ->
            if (group.isBlank()) flowOf(null)
            else schedule.observeDay(date, group.trim())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleUiState> =
        combine(dayFlow, _selectedDate, settings.groupQuery, _isRefreshing, _error) {
                day, date, group, refreshing, error,
            ->
            ScheduleUiState(
                selectedDate = date,
                groupQuery = group,
                day = day,
                isRefreshing = refreshing,
                error = error,
            )
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
            .onSuccess {
                _error.value = null
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
    }
}
