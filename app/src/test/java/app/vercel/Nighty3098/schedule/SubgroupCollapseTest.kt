package app.vercel.Nighty3098.schedule

import app.vercel.Nighty3098.schedule.data.calendar.CalendarSyncManager.Companion.collapseSubgroups
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Схлопывание подгрупп в одно событие на слот. */
class SubgroupCollapseTest {

    private fun lesson(
        number: Int = 1,
        from: String = "09:00",
        to: String = "10:30",
        subject: String = "Математика",
        subgroup: String = "",
        teachers: List<String> = listOf("Иванов И.И."),
        room: String = "305",
    ) = Lesson(
        groupQuery = "3414",
        weekIndex = 0,
        dayIndex = 0,
        number = number,
        timeFrom = from,
        timeTo = to,
        subject = subject,
        teachers = teachers,
        room = room,
        subgroup = subgroup,
    )

    @Test
    fun tripletCollapsesToOne() {
        val collapsed = collapseSubgroups(
            listOf(
                lesson(subgroup = "Подгруппа 1", teachers = listOf("A"), room = "101"),
                lesson(subgroup = "Подгруппа 2", teachers = listOf("B"), room = "102"),
                lesson(subgroup = "Подгруппа 3", teachers = listOf("B"), room = "102"),
            ),
        )
        assertEquals(1, collapsed.size)
        val one = collapsed.first()
        // Подгруппа затёрта — в заголовок/описание не попадёт.
        assertEquals("", one.subgroup)
        assertEquals("Математика", one.subject)
        // Преподаватели и аудитории объединены без дублей.
        assertEquals(listOf("A", "B"), one.teachers)
        assertEquals("101, 102", one.room)
    }

    @Test
    fun differentSlotsKept() {
        val collapsed = collapseSubgroups(
            listOf(
                lesson(from = "09:00", to = "10:30"),
                lesson(from = "10:45", to = "12:15"),
            ),
        )
        assertEquals(2, collapsed.size)
    }

    @Test
    fun differentSubjectsSameTimeKept() {
        val collapsed = collapseSubgroups(
            listOf(
                lesson(subject = "Математика"),
                lesson(subject = "Физика"),
            ),
        )
        assertEquals(2, collapsed.size)
    }

    @Test
    fun soloSubgroupCleared() {
        // Одиночная пара с пометкой подгруппы — тоже без подгруппы в календаре.
        val collapsed = collapseSubgroups(listOf(lesson(subgroup = "1")))
        assertEquals(1, collapsed.size)
        assertEquals("", collapsed.first().subgroup)
    }

    @Test
    fun collapsedStaysOrdered() {
        val collapsed = collapseSubgroups(
            listOf(
                lesson(number = 2, from = "10:45", to = "12:15"),
                lesson(number = 1, from = "09:00", to = "10:30"),
            ),
        )
        assertTrue(collapsed[0].startTime!!.isBefore(collapsed[1].startTime))
    }
}
