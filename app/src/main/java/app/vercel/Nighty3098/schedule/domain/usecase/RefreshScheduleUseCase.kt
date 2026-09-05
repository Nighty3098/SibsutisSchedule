package app.vercel.Nighty3098.schedule.domain.usecase

import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import app.vercel.Nighty3098.schedule.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** Обновить расписание текущей группы (с TTL-кэшем 3 часа, см. ScheduleRepository). */
class RefreshScheduleUseCase(
    private val schedule: ScheduleRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(groupQuery: String? = null, force: Boolean = false): Result<Int> {
        val group = groupQuery ?: settings.groupQuery.first()
        if (group.isBlank()) return Result.failure(IllegalArgumentException("Не указана группа"))
        return schedule.refresh(group.trim(), force)
    }
}
