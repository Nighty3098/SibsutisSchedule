package app.vercel.Nighty3098.schedule.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Чётность недели в двухнедельном цикле «числитель / знаменатель».
 *
 * Повторяет JS-логику страницы my.sibsutis.ru / sibsutis.ru:
 * опорная точка — понедельник учебной недели, на которую приходится
 * 1 сентября соответствующего учебного года. Чётный сдвиг от опоры —
 * числитель (0), нечётный — знаменатель (1).
 *
 * Проверено по данным сайта: пн 25.05.2026 → числитель.
 */
object WeekParity {
    /** 0 — числитель, 1 — знаменатель. */
    fun of(date: LocalDate): Int {
        val anchorMonday = mondayOf(defaultAnchor(date))
        val dateMonday = mondayOf(date)
        val weeks = ChronoUnit.WEEKS.between(anchorMonday, dateMonday).toInt()
        return ((weeks % 2) + 2) % 2
    }

    /** Индекс дня недели: 0 — понедельник … 6 — воскресенье. */
    fun dayIndex(date: LocalDate): Int =
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }

    fun isNumerator(date: LocalDate): Boolean = of(date) == 0

    fun weekName(date: LocalDate): String =
        if (isNumerator(date)) "Числитель" else "Знаменатель"

    private fun defaultAnchor(date: LocalDate): LocalDate {
        val sep1 = LocalDate.of(date.year, 9, 1)
        return if (date.isBefore(sep1)) sep1.minusYears(1) else sep1
    }

    private fun mondayOf(date: LocalDate): LocalDate {
        var d = date
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        return d
    }
}
