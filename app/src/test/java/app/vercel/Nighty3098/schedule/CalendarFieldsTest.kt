package app.vercel.Nighty3098.schedule

import app.vercel.Nighty3098.schedule.data.calendar.CalendarSyncManager.Companion.MARKER
import app.vercel.Nighty3098.schedule.data.calendar.CalendarSyncManager.Companion.marker
import app.vercel.Nighty3098.schedule.data.calendar.CalendarSyncManager.Companion.toCalendarFields
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проекция пары на событие календаря: поля, маркер, пропуск битого времени. */
class CalendarFieldsTest {

    private val date = LocalDate.of(2026, 9, 7) // понедельник

    private fun lesson(
        from: String = "09:00",
        to: String = "10:30",
    ) = Lesson(
        groupQuery = "3414",
        weekIndex = 0,
        dayIndex = 0,
        number = 1,
        timeFrom = from,
        timeTo = to,
        subject = "Математика",
        typeRaw = "Лекция",
        teachers = listOf("Иванов И.И."),
        room = "305",
        subgroup = "1",
    )

    @Test
    fun fullLessonMapsToEvent() {
        val f = lesson().toCalendarFields(date, marker("3414")) ?: error("fields null")
        // Подгруппа видна в заголовке — тройняшки различимы в календаре.
        assertEquals("Математика (1)", f.title)
        assertEquals("305", f.location)
        assertTrue(f.endMillis - f.startMillis == 90 * 60 * 1000L)
        assertTrue(f.description.startsWith(MARKER))
        assertTrue("group" in f.description)
        assertTrue("Иванов И.И." in f.description)
        assertTrue("Лекция" in f.description)
        assertTrue("Подгруппа: 1" in f.description)
    }

    @Test
    fun fullSubgroupNameNotDoubled() {
        // «Подгруппа 2» с сайта не превращается в «Подгруппа: Подгруппа 2».
        val f = lesson().copy(subgroup = "Подгруппа 2")
            .toCalendarFields(date, marker("3414")) ?: error("fields null")
        assertEquals("Математика (Подгруппа 2)", f.title)
        assertTrue("Подгруппа: Подгруппа" !in f.description)
        assertTrue("Подгруппа 2" in f.description)
    }

    @Test
    fun noSubgroupKeepsPlainTitle() {
        val f = lesson().copy(subgroup = "")
            .toCalendarFields(date, marker("3414")) ?: error("fields null")
        assertEquals("Математика", f.title)
    }

    @Test
    fun eventSpansCorrectWallClock() {
        val f = lesson().toCalendarFields(date, marker("3414")) ?: error("fields null")
        val zone = java.time.ZoneId.systemDefault()
        val expectedStart = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, f.startMillis)
    }

    @Test
    fun badTimeIsSkipped() {
        assertNull(lesson(from = "??", to = "10:30").toCalendarFields(date, marker("3414")))
        assertNull(lesson(from = "09:00", to = "").toCalendarFields(date, marker("3414")))
        // Конец раньше начала — тоже пропуск.
        assertNull(lesson(from = "10:30", to = "09:00").toCalendarFields(date, marker("3414")))
    }

    @Test
    fun emptyOptionalFieldsOmitted() {
        val f = (
            lesson().copy(teachers = emptyList(), subgroup = "", typeRaw = "")
                .toCalendarFields(date, marker("3414")) ?: error("fields null")
            )
        assertTrue("Преподаватель" !in f.description)
        assertTrue("Подгруппа" !in f.description)
        assertTrue("Тип" !in f.description)
    }
}
