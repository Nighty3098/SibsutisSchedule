package app.vercel.Nighty3098.schedule.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Доменная модель одного занятия.
 *
 * @param weekIndex 0 — числитель, 1 — знаменатель (двухнедельный цикл сайта).
 * @param dayIndex 0 — понедельник … 6 — воскресенье.
 */
data class Lesson(
    val id: Long = 0,
    val groupQuery: String,
    val weekIndex: Int,
    val dayIndex: Int,
    val number: Int,
    val timeFrom: String,
    val timeTo: String,
    val subject: String,
    val typeRaw: String = "",
    val type: LessonType = LessonType.fromRaw(typeRaw),
    val teachers: List<String> = emptyList(),
    val room: String = "",
    val subgroup: String = "",
    val groups: List<String> = emptyList(),
) {
    val startTime: LocalTime? get() = timeFrom.toLocalTimeOrNull()
    val endTime: LocalTime? get() = timeTo.toLocalTimeOrNull()

    /** true, если пара идёт прямо сейчас (для подсветки текущей пары). */
    fun isNow(now: LocalTime = LocalTime.now()): Boolean {
        val s = startTime ?: return false
        val e = endTime ?: return false
        return !now.isBefore(s) && now.isBefore(e)
    }

    companion object {
        private fun String.toLocalTimeOrNull(): LocalTime? =
            try {
                LocalTime.parse(trim())
            } catch (_: Exception) {
                null
            }
    }
}

/** Расписание одного календарного дня (уже спроецированное на дату). */
data class DaySchedule(
    val date: LocalDate,
    val weekIndex: Int,
    val lessons: List<Lesson>,
)
