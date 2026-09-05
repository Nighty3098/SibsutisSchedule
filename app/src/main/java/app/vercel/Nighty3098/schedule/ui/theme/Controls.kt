package app.vercel.Nighty3098.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Единый стиль контролов, видимый во всех 7 темах.
 *
 * Проблема: M3-компоненты по умолчанию берут приглушённые роли схемы
 * (прозрачный контейнер у OutlinedTextField, surfaceContainerHighest
 * у выключенного Switch). В тёмных кастомных темах, где surface
 * почти равен background, контролы сливаются с фоном — в отличие
 * от Material You, где dynamic-цвета дают тональное разделение.
 *
 * Здесь цвета заданы явно через роли [MaterialTheme.colorScheme],
 * поэтому стиль адаптируется под каждую тему автоматически.
 */

/** Заливка селекторов (выпадающие списки): сплошной фон + без полоски-индикатора. */
@Composable
fun selectorFieldColors(): TextFieldColors {
    val s = MaterialTheme.colorScheme
    val fill = controlFill()
    return TextFieldDefaults.colors(
        focusedContainerColor = fill,
        unfocusedContainerColor = fill,
        disabledContainerColor = fill,
        errorContainerColor = fill,
        focusedTextColor = s.onSurface,
        unfocusedTextColor = s.onSurface,
        disabledTextColor = s.onSurfaceVariant,
        errorTextColor = s.onSurface,
        cursorColor = s.primary,
        errorCursorColor = s.primary,
        focusedLabelColor = s.primary,
        unfocusedLabelColor = s.onSurfaceVariant,
        disabledLabelColor = s.onSurfaceVariant,
        errorLabelColor = s.error,
        focusedTrailingIconColor = s.onSurfaceVariant,
        unfocusedTrailingIconColor = s.onSurfaceVariant,
        disabledTrailingIconColor = s.onSurfaceVariant,
        errorTrailingIconColor = s.onSurfaceVariant,
        focusedSupportingTextColor = s.onSurfaceVariant,
        unfocusedSupportingTextColor = s.onSurfaceVariant,
        focusedPlaceholderColor = s.onSurfaceVariant,
        unfocusedPlaceholderColor = s.onSurfaceVariant,
        disabledPlaceholderColor = s.onSurfaceVariant,
        errorPlaceholderColor = s.onSurfaceVariant,
        // Без нижней полоски — поле выглядит цельной плашкой.
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    )
}

/** Тоглы: яркое яблоко и читаемый трек в обоих состояниях. */
@Composable
fun appSwitchColors(): SwitchColors {
    val s = MaterialTheme.colorScheme
    return SwitchDefaults.colors(
        checkedTrackColor = s.primary,
        checkedThumbColor = s.onPrimary,
        checkedBorderColor = Color.Transparent,
        uncheckedTrackColor = controlFill(),
        // Яблоко — чистый onSurface: видно на любом треке.
        uncheckedThumbColor = s.onSurface,
        uncheckedBorderColor = s.outline,
        disabledCheckedTrackColor = s.surfaceContainerHighest,
        disabledUncheckedTrackColor = s.surfaceContainerHighest,
    )
}

/**
 * Заливка контролов — ступенью выше карточек.
 *
 * Замер показал: Card красится в surfaceContainerHighest, поэтому
 * заливка тем же Highest сливается с карточкой. Кладём поверх
 * Highest 10% onSurface: в тёмных темах осветляет, в светлой —
 * затемняет. Видимо везде, включая dynamic Material You.
 */
@Composable
fun controlFill(): Color {
    val s = MaterialTheme.colorScheme
    return s.onSurface.copy(alpha = 0.10f).compositeOver(s.surfaceContainerHighest)
}
