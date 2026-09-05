package app.vercel.Nighty3098.schedule

import app.vercel.Nighty3098.schedule.data.parser.ScheduleParser
import app.vercel.Nighty3098.schedule.domain.model.LessonType
import app.vercel.Nighty3098.schedule.domain.model.WeekParity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Проверяет ключевое знание о сайте: расписание встроено в страницу как
 * JS-переменные days[1..14] (см. фикстуру — структура повторяет реальную
 * страницу my.sibsutis.ru / sibsutis.ru).
 */
class ScheduleParserTest {

    private val sampleHtml = """
        <!DOCTYPE html><html lang="ru"><head><meta charset="utf-8"></head><body>
        <script>
        var days = []
        var fact_schedule_days = []
        days[1] = '{"ScheduleCell":[{"DateBegin":"0001-01-01T08:00:00","DateEnd":"0001-01-01T09:35:00","Subgroup":[]},{"DateBegin":"0001-01-01T09:50:00","DateEnd":"0001-01-01T11:25:00","Subgroup":[{"DISCIPLINE":"Базы данных","TYPE_LESSON":"Лекционные занятия","TEACHER":["Преподаватель А.А."],"CLASSROOM":"а.210","GROUP":["ТЕСТ-11"],"SUBGROUP":null}]}]}'
        days[2] = '{"ScheduleCell":[{"DateBegin":"0001-01-02T11:40:00","DateEnd":"0001-01-02T13:15:00","Subgroup":[{"DISCIPLINE":"Программирование","TYPE_LESSON":"Лабораторные занятия","TEACHER":"Преподаватель Б.Б.","CLASSROOM":"а.312","GROUP":["ТЕСТ-11"],"SUBGROUP":"1 подгруппа"}]}]}'
        fact_schedule_days[1] = 'null'
        days[8] = '{"ScheduleCell":[{"DateBegin":"0001-01-08T08:00:00","DateEnd":"0001-01-08T09:35:00","Subgroup":[{"DISCIPLINE":"Физика","TYPE_LESSON":"Практические занятия","TEACHER":["Преподаватель В.В.","Преподаватель Г.Г."],"CLASSROOM":"а.101","GROUP":["ТЕСТ-11"],"SUBGROUP":null}]}]}'
        </script></body></html>
    """.trimIndent()

    @Test
    fun parsesDaysJsonIntoTwoWeeks() {
        val sched = ScheduleParser.parse("3414", sampleHtml)

        assertEquals(3, sched.lessons.size)

        // Числитель, понедельник, 2-й слот.
        val db = sched.lessons.first { it.subject == "Базы данных" }
        assertEquals(0, db.weekIndex)
        assertEquals(0, db.dayIndex)
        assertEquals(2, db.number)
        assertEquals("09:50", db.timeFrom)
        assertEquals("11:25", db.timeTo)
        assertEquals(LessonType.LECTURE, db.type)
        assertEquals(listOf("Преподаватель А.А."), db.teachers)
        assertEquals("а.210", db.room)

        // TEACHER строкой (не массивом) — тоже разбирается.
        val prog = sched.lessons.first { it.subject == "Программирование" }
        assertEquals(listOf("Преподаватель Б.Б."), prog.teachers)
        assertEquals("1 подгруппа", prog.subgroup)
        assertEquals(LessonType.LAB, prog.type)

        // Знаменатель, понедельник; fact_schedule_days проигнорирован.
        val phys = sched.lessons.first { it.subject == "Физика" }
        assertEquals(1, phys.weekIndex)
        assertEquals(0, phys.dayIndex)
        assertEquals(LessonType.PRACTICE, phys.type)
        assertEquals(2, phys.teachers.size)
    }

    @Test(expected = ScheduleParser.NoScheduleDataException::class)
    fun authPageThrowsNoData() {
        ScheduleParser.parse(
            "3414",
            "<html><body><form><input type='password' name='USER_PASSWORD'/></form></body></html>",
        )
    }

    @Test
    fun weekParityMatchesSiteLogic() {
        // Репер с сайта: пн 25.05.2026 — числитель.
        assertEquals(0, WeekParity.of(LocalDate.of(2026, 5, 25)))
        // Следующий понедельник — знаменатель.
        assertEquals(1, WeekParity.of(LocalDate.of(2026, 6, 1)))
        // Опора: пн 01.09.2025 — числитель.
        assertEquals(0, WeekParity.of(LocalDate.of(2025, 9, 1)))
        assertEquals("Знаменатель", WeekParity.weekName(LocalDate.of(2026, 6, 1)))
        assertTrue(WeekParity.isNumerator(LocalDate.of(2026, 5, 25)))
        // Числитель — нечётная неделя, знаменатель — чётная.
        assertEquals("Нечётная", WeekParity.evenOddName(LocalDate.of(2026, 5, 25)))
        assertEquals("Чётная", WeekParity.evenOddName(LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun lessonTypeNormalization() {
        assertEquals(LessonType.LECTURE, LessonType.fromRaw("Лекционные занятия"))
        assertEquals(LessonType.PRACTICE, LessonType.fromRaw("Практические занятия"))
        assertEquals(LessonType.LAB, LessonType.fromRaw("Лабораторные занятия"))
        assertEquals(LessonType.OTHER, LessonType.fromRaw("Консультация"))
    }
}
