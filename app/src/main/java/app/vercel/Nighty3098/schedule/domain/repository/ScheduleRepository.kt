package app.vercel.Nighty3098.schedule.domain.repository

import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.model.RefreshOutcome
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Источник расписания: сеть (с кэшем в Room) + офлайн-фолбэк. */
interface ScheduleRepository {
    /** Реактивный поток пар на дату для сохранённой группы. */
    fun observeDay(date: LocalDate, groupQuery: String): Flow<DaySchedule>

    /**
     * Обновить кэш группы с сайта.
     *
     * @param force true — всегда идти в сеть (кнопка «Обновить»);
     * false — пропустить сеть, если кэш в Room свежее [CACHE_TTL_MILLIS].
     * @return Result с [RefreshOutcome]: количество пар в кэше + diff
     * изменений относительно предыдущего кэша (пуст при пропуске сети
     * и при первичной загрузке); при отсутствии сети и пустом кэше —
     * failure, при наличии кэша сеть не обязательна
     * (данные уже доступны через [observeDay]).
     */
    suspend fun refresh(groupQuery: String, force: Boolean = false): Result<RefreshOutcome>

    /** Когда последний раз успешно обновляли группу (null — никогда). */
    suspend fun lastUpdated(groupQuery: String): Long?

    /**
     * Разовый (не Flow) список пар на дату — для фоновых потребителей
     * вроде синхронизации с календарём, которым не нужна подписка.
     */
    suspend fun getDayLessons(date: LocalDate, groupQuery: String): List<Lesson>

    companion object {
        /** Время жизни кэша: 3 часа. */
        const val CACHE_TTL_MILLIS = 3 * 60 * 60 * 1000L

        /** Чистая функция для тестов: свеж ли кэш возраста [ageMillis]. */
        fun isCacheFresh(updatedAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
            if (updatedAtMillis == null || updatedAtMillis <= 0) return false
            return nowMillis - updatedAtMillis < CACHE_TTL_MILLIS
        }
    }
}
