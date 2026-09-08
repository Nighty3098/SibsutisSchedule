package app.vercel.Nighty3098.schedule.domain.model

/**
 * Снимок одной пары для сравнения кэша с вновь загруженным расписанием.
 *
 * У пар с сайта нет стабильных ID (Room раздаёт свои при вставке),
 * поэтому бизнес-ключ — позиция в двухнедельном цикле:
 * (weekIndex, dayIndex, number). Номер слота стабилен, т.к. парсер
 * нумерует слоты времени подряд с 1.
 */
data class LessonSnapshot(
    val weekIndex: Int,
    val dayIndex: Int,
    val number: Int,
    val timeFrom: String,
    val timeTo: String,
    val subject: String,
    val typeRaw: String,
    val teachers: List<String>,
    val room: String,
    val subgroup: String,
)

/** Одно изменение расписания между двумя загрузками. */
sealed interface ScheduleChange {
    val weekIndex: Int
    val dayIndex: Int

    data class Added(val lesson: LessonSnapshot) : ScheduleChange {
        override val weekIndex: Int get() = lesson.weekIndex
        override val dayIndex: Int get() = lesson.dayIndex
    }

    data class Removed(val lesson: LessonSnapshot) : ScheduleChange {
        override val weekIndex: Int get() = lesson.weekIndex
        override val dayIndex: Int get() = lesson.dayIndex
    }

    data class Modified(
        val old: LessonSnapshot,
        val new: LessonSnapshot,
        /** Что именно поменялось: «время», «аудитория», «предмет» и т.д. */
        val aspects: List<String>,
    ) : ScheduleChange {
        override val weekIndex: Int get() = new.weekIndex
        override val dayIndex: Int get() = new.dayIndex
    }
}

/** Итог обновления: сколько пар в кэше + что изменилось. */
data class RefreshOutcome(
    val lessonCount: Int,
    val changes: List<ScheduleChange> = emptyList(),
)

private val ShortWeekdays = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun parityName(weekIndex: Int): String =
    if (weekIndex == 0) "нечётная" else "чётная"

private fun dayRef(weekIndex: Int, dayIndex: Int): String =
    "${ShortWeekdays.getOrElse(dayIndex) { "?" }} · ${parityName(weekIndex)}"

private fun LessonSnapshot.timeRange(): String =
    listOf(timeFrom, timeTo).filter { it.isNotBlank() }.joinToString("–")

private fun LessonSnapshot.short(): String {
    val parts = listOfNotNull(
        timeRange().takeIf { it.isNotEmpty() },
        subject.takeIf { it.isNotBlank() },
        room.takeIf { it.isNotBlank() }?.let { "ауд. $it" },
        subgroup.takeIf { it.isNotBlank() },
    )
    return parts.joinToString(", ")
}

/** Человекочитаемая строка для уведомления/диалога. */
fun ScheduleChange.describe(): String = when (this) {
    is ScheduleChange.Added -> "+ ${dayRef(weekIndex, dayIndex)}: ${lesson.short()}"
    is ScheduleChange.Removed -> "− ${dayRef(weekIndex, dayIndex)}: ${lesson.short()}"
    is ScheduleChange.Modified -> {
        val what = aspects.joinToString("; ")
        "~ ${dayRef(weekIndex, dayIndex)}: ${new.short()} ($what)"
    }
}

/**
 * Сравнение старого кэша с новым расписанием.
 *
 * Выравнивание — по бизнес-ключу (неделя, день, номер слота):
 * нет старого → [Added], нет нового → [Removed], есть оба —
 * посимвольное сравнение полей → [Modified] или ничего.
 * Пустой старый список = первичная загрузка, изменений нет
 * (иначе каждая первая синхронизация давала бы десятки «добавлений»).
 */
fun diffLessonSnapshots(
    old: List<LessonSnapshot>,
    new: List<LessonSnapshot>,
): List<ScheduleChange> {
    if (old.isEmpty()) return emptyList()
    val oldByKey = old.associateBy { Triple(it.weekIndex, it.dayIndex, it.number) }
    val newByKey = new.associateBy { Triple(it.weekIndex, it.dayIndex, it.number) }
    val out = mutableListOf<ScheduleChange>()
    for ((key, newLesson) in newByKey) {
        val oldLesson = oldByKey[key]
        if (oldLesson == null) {
            out += ScheduleChange.Added(newLesson)
        } else {
            modifiedAspects(oldLesson, newLesson)?.let { aspects ->
                out += ScheduleChange.Modified(oldLesson, newLesson, aspects)
            }
        }
    }
    for ((key, oldLesson) in oldByKey) {
        if (key !in newByKey) out += ScheduleChange.Removed(oldLesson)
    }
    return out.sortedWith(
        compareBy(
            { it.weekIndex },
            { it.dayIndex },
            {
                when (it) {
                    is ScheduleChange.Added -> it.lesson.number
                    is ScheduleChange.Removed -> it.lesson.number
                    is ScheduleChange.Modified -> it.new.number
                }
            },
        ),
    )
}

private fun norm(s: String): String = s.trim()

/** null — пара не изменилась, иначе список «время: было → стало». */
private fun modifiedAspects(old: LessonSnapshot, new: LessonSnapshot): List<String>? {
    val out = mutableListOf<String>()
    if (norm(old.timeFrom) != norm(new.timeFrom) || norm(old.timeTo) != norm(new.timeTo)) {
        out += "время: ${old.timeRange().ifEmpty { "—" }} → ${new.timeRange().ifEmpty { "—" }}"
    }
    if (norm(old.subject) != norm(new.subject)) {
        out += "предмет: ${old.subject.ifEmpty { "—" }} → ${new.subject.ifEmpty { "—" }}"
    }
    if (norm(old.room) != norm(new.room)) {
        out += "аудитория: ${old.room.ifEmpty { "—" }} → ${new.room.ifEmpty { "—" }}"
    }
    if (norm(old.typeRaw) != norm(new.typeRaw)) {
        out += "тип: ${old.typeRaw.ifEmpty { "—" }} → ${new.typeRaw.ifEmpty { "—" }}"
    }
    if (old.teachers.map(::norm) != new.teachers.map(::norm)) {
        out += "преподаватель: ${old.teachers.joinToString(", ").ifEmpty { "—" }} → " +
            new.teachers.joinToString(", ").ifEmpty { "—" }
    }
    if (norm(old.subgroup) != norm(new.subgroup)) {
        out += "подгруппа: ${old.subgroup.ifEmpty { "—" }} → ${new.subgroup.ifEmpty { "—" }}"
    }
    return out.takeIf { it.isNotEmpty() }
}
