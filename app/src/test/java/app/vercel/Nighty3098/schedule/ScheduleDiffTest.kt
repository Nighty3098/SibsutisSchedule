package app.vercel.Nighty3098.schedule

import app.vercel.Nighty3098.schedule.domain.model.LessonSnapshot
import app.vercel.Nighty3098.schedule.domain.model.ScheduleChange
import app.vercel.Nighty3098.schedule.domain.model.describe
import app.vercel.Nighty3098.schedule.domain.model.diffLessonSnapshots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Дифф кэша и нового расписания: added/removed/modified. */
class ScheduleDiffTest {

    private fun snap(
        number: Int,
        subject: String = "Математика",
        from: String = "09:00",
        to: String = "10:30",
        room: String = "101",
        teachers: List<String> = listOf("Иванов И.И."),
        subgroup: String = "",
        week: Int = 0,
        day: Int = 0,
        type: String = "Лекция",
    ) = LessonSnapshot(
        weekIndex = week,
        dayIndex = day,
        number = number,
        timeFrom = from,
        timeTo = to,
        subject = subject,
        typeRaw = type,
        teachers = teachers,
        room = room,
        subgroup = subgroup,
    )

    @Test
    fun emptyOldMeansFirstLoadNoChanges() {
        // Первичная загрузка: всё «новое», но изменениями не считаем.
        val changes = diffLessonSnapshots(emptyList(), listOf(snap(1), snap(2)))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun identicalListsNoChanges() {
        val changes = diffLessonSnapshots(listOf(snap(1), snap(2)), listOf(snap(1), snap(2)))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun addedLessonDetected() {
        val changes = diffLessonSnapshots(listOf(snap(1)), listOf(snap(1), snap(2)))
        assertEquals(1, changes.size)
        val added = changes.first() as ScheduleChange.Added
        assertEquals(2, added.lesson.number)
        assertTrue(added.describe().startsWith("+"))
    }

    @Test
    fun removedLessonDetected() {
        val changes = diffLessonSnapshots(listOf(snap(1), snap(2)), listOf(snap(1)))
        assertEquals(1, changes.size)
        val removed = changes.first() as ScheduleChange.Removed
        assertEquals(2, removed.lesson.number)
        assertTrue(removed.describe().startsWith("−"))
    }

    @Test
    fun roomChangeDetected() {
        val changes = diffLessonSnapshots(listOf(snap(1, room = "101")), listOf(snap(1, room = "102")))
        assertEquals(1, changes.size)
        val modified = changes.first() as ScheduleChange.Modified
        assertEquals(listOf("аудитория: 101 → 102"), modified.aspects)
        assertTrue(modified.describe().startsWith("~"))
    }

    @Test
    fun timeAndTeacherChangeDetected() {
        val changes = diffLessonSnapshots(
            listOf(snap(1, from = "09:00", to = "10:30", teachers = listOf("A"))),
            listOf(snap(1, from = "10:45", to = "12:15", teachers = listOf("B"))),
        )
        assertEquals(1, changes.size)
        val modified = changes.first() as ScheduleChange.Modified
        assertEquals(2, modified.aspects.size)
        assertTrue(modified.aspects.any { it.startsWith("время:") })
        assertTrue(modified.aspects.any { it.startsWith("преподаватель:") })
    }

    @Test
    fun changesSortedByWeekDayNumber() {
        val old = listOf(snap(1, week = 0, day = 1), snap(1, week = 1, day = 0))
        val new = listOf(
            snap(1, week = 0, day = 1, room = "999"),
            snap(1, week = 1, day = 0, room = "999"),
            snap(2, week = 0, day = 0),
        )
        val changes = diffLessonSnapshots(old, new)
        assertEquals(3, changes.size)
        // Сортировка по (неделя, день, номер).
        assertTrue(changes[0] is ScheduleChange.Added)
        assertEquals(0, changes[0].dayIndex)
        assertTrue(changes[1] is ScheduleChange.Modified)
        assertEquals(1, changes[1].dayIndex)
        assertTrue(changes[2] is ScheduleChange.Modified)
        assertEquals(1, changes[2].weekIndex)
    }

    @Test
    fun describeContainsDayAndParity() {
        val added = ScheduleChange.Added(snap(1, week = 1, day = 2))
        // Среда, знаменатель → «чётная».
        assertEquals("+ Ср · чётная: 09:00–10:30, Математика, ауд. 101", added.describe())
    }
}
