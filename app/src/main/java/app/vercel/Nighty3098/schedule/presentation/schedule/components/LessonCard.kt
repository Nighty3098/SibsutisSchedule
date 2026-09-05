package app.vercel.Nighty3098.schedule.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.model.LessonType
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.ui.theme.accentFor

private fun LessonType.label(typeRaw: String): String? {
    if (typeRaw.isBlank()) return null
    // Короткая подпись для бейджа: "Лекция", "Практика", "Лабораторная" или как есть.
    val v = typeRaw.lowercase()
    return when {
        "лекц" in v -> "Лекция"
        "практ" in v -> "Практика"
        "лабор" in v -> "Лабораторная"
        else -> typeRaw
    }
}

/**
 * Карточка пары в стиле Google Calendar:
 * слева время, цветная полоска типа, предмет, аудитория + преподаватель.
 */
@Composable
fun LessonCard(
    lesson: Lesson,
    isCurrent: Boolean,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val accent = lesson.type.accentFor(themeMode)
    val highlighted = isCurrent || isNext

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) accent else accent.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                accent.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Вертикальная полоска типа занятия.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Spacer(Modifier.width(12.dp))

            // Время слева.
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = lesson.timeFrom.ifEmpty { "—" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = lesson.timeTo.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (lesson.number > 0) {
                    Text(
                        text = "${lesson.number} пара",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))

            // Основное содержимое.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                lesson.type.label(lesson.typeRaw)?.let { label ->
                    Spacer(Modifier.height(4.dp))
                    TypeBadge(text = label, color = accent)
                }
                Spacer(Modifier.height(4.dp))
                val secondary = buildList {
                    if (lesson.room.isNotBlank()) add("ауд. ${lesson.room}")
                    if (lesson.subgroup.isNotBlank()) add(lesson.subgroup)
                }.joinToString(" • ")
                if (secondary.isNotEmpty()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (lesson.teachers.isNotEmpty()) {
                    Text(
                        text = lesson.teachers.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isCurrent) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "● Сейчас идёт",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                } else if (isNext) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Следующая",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
