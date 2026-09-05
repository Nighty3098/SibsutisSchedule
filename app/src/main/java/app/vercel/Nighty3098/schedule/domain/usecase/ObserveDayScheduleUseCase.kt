package app.vercel.Nighty3098.schedule.domain.usecase

import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import app.vercel.Nighty3098.schedule.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

/**
 * Реактивное расписание на день для текущей (сохранённой) группы.
 * При смене группы или даты поток автоматически переключается.
 */
class ObserveDayScheduleUseCase(
    private val schedule: ScheduleRepository,
    private val settings: SettingsRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate): Flow<DaySchedule> =
        settings.groupQuery
            .distinctUntilChanged()
            .flatMapLatest { group -> schedule.observeDay(date, group) }
}
