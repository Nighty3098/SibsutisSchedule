package app.vercel.Nighty3098.schedule.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Кэш одной пары. Расписание сайта — двухнедельный цикл, поэтому храним
 * позицию (weekIndex × dayIndex), а не конкретную дату:
 * weekIndex: 0 — числитель, 1 — знаменатель;
 * dayIndex: 0 — Пн … 6 — Вс.
 */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupQuery: String,
    val weekIndex: Int,
    val dayIndex: Int,
    val number: Int,
    val timeFrom: String,
    val timeTo: String,
    val subject: String,
    val typeRaw: String,
    /** Преподаватели, склеенные через '\n'. */
    val teachers: String,
    val room: String,
    val subgroup: String,
    /** Группы (важно для расписаний преподавателя/аудитории), через '\n'. */
    val groups: String,
)
