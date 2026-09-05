package app.vercel.Nighty3098.schedule.domain.model

/** Тип занятия. Используется для цветового кодирования карточек. */
enum class LessonType {
    LECTURE,
    PRACTICE,
    LAB,
    OTHER;

    companion object {
        /**
         * Нормализация сырого TYPE_LESSON с сайта.
         * Примеры с продa: "Лекционные занятия", "Практические занятия",
         * "Лабораторные занятия".
         */
        fun fromRaw(raw: String?): LessonType {
            if (raw == null) return OTHER
            val v = raw.lowercase()
            return when {
                "лекц" in v -> LECTURE
                "практ" in v -> PRACTICE
                "лабор" in v -> LAB
                else -> OTHER
            }
        }
    }
}
