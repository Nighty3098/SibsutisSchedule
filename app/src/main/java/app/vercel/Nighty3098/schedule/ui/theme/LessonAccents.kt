package app.vercel.Nighty3098.schedule.ui.theme

import app.vercel.Nighty3098.schedule.domain.model.LessonType
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode

/**
 * Цвет полоски/бейджа типа занятия для активной темы.
 * SYSTEM и AMOLED делят общую сине-зелёную кодировку, у остальных —
 * собственные акценты из палитры темы (у монохрома — градации серого).
 */
fun LessonType.accentFor(theme: ThemeMode) = when (theme) {
    ThemeMode.MONOCHROME -> when (this) {
        LessonType.LECTURE -> MonoLecture
        LessonType.PRACTICE -> MonoPractice
        LessonType.LAB -> MonoLab
        LessonType.OTHER -> MonoOther
    }
    ThemeMode.SOLARIZED_OSAKA -> when (this) {
        LessonType.LECTURE -> SolLecture
        LessonType.PRACTICE -> SolPractice
        LessonType.LAB -> SolLab
        LessonType.OTHER -> SolOther
    }
    ThemeMode.GRUVBOX -> when (this) {
        LessonType.LECTURE -> GrLecture
        LessonType.PRACTICE -> GrPractice
        LessonType.LAB -> GrLab
        LessonType.OTHER -> GrOther
    }
    ThemeMode.TOKYO_NIGHT -> when (this) {
        LessonType.LECTURE -> TnLecture
        LessonType.PRACTICE -> TnPractice
        LessonType.LAB -> TnLab
        LessonType.OTHER -> TnOther
    }
    ThemeMode.CATPPUCCIN -> when (this) {
        LessonType.LECTURE -> CpLecture
        LessonType.PRACTICE -> CpPractice
        LessonType.LAB -> CpLab
        LessonType.OTHER -> CpOther
    }
    ThemeMode.SYSTEM, ThemeMode.AMOLED -> when (this) {
        LessonType.LECTURE -> LectureBlue
        LessonType.PRACTICE -> PracticeGreen
        LessonType.LAB -> LabOrange
        LessonType.OTHER -> OtherGrey
    }
}
