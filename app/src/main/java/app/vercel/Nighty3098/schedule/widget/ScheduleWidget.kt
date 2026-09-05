package app.vercel.Nighty3098.schedule.widget

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.vercel.Nighty3098.schedule.MainActivity
import app.vercel.Nighty3098.schedule.appContainer
import app.vercel.Nighty3098.schedule.domain.model.LessonType
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.ui.theme.AmoledPrimary
import app.vercel.Nighty3098.schedule.ui.theme.CpBackground
import app.vercel.Nighty3098.schedule.ui.theme.CpOnSurface
import app.vercel.Nighty3098.schedule.ui.theme.CpOnSurfaceVariant
import app.vercel.Nighty3098.schedule.ui.theme.CpPrimary
import app.vercel.Nighty3098.schedule.ui.theme.CpSurfaceContainer
import app.vercel.Nighty3098.schedule.ui.theme.GrBackground
import app.vercel.Nighty3098.schedule.ui.theme.GrOnSurface
import app.vercel.Nighty3098.schedule.ui.theme.GrOnSurfaceVariant
import app.vercel.Nighty3098.schedule.ui.theme.GrPrimary
import app.vercel.Nighty3098.schedule.ui.theme.GrSurfaceContainer
import app.vercel.Nighty3098.schedule.ui.theme.MonoPrimary
import app.vercel.Nighty3098.schedule.ui.theme.SolBackground
import app.vercel.Nighty3098.schedule.ui.theme.SolOnSurface
import app.vercel.Nighty3098.schedule.ui.theme.SolOnSurfaceVariant
import app.vercel.Nighty3098.schedule.ui.theme.SolPrimary
import app.vercel.Nighty3098.schedule.ui.theme.SolSurfaceContainer
import app.vercel.Nighty3098.schedule.ui.theme.TnBackground
import app.vercel.Nighty3098.schedule.ui.theme.TnOnSurface
import app.vercel.Nighty3098.schedule.ui.theme.TnOnSurfaceVariant
import app.vercel.Nighty3098.schedule.ui.theme.TnPrimary
import app.vercel.Nighty3098.schedule.ui.theme.TnSurfaceContainer
import app.vercel.Nighty3098.schedule.ui.theme.accentFor
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class WidgetLesson(
    val time: String,
    val subject: String,
    val meta: String,
    val type: LessonType,
)

/** Статичная палитра виджета. null = SYSTEM (динамические цвета GlanceTheme). */
private data class WidgetPalette(
    val bg: Color,
    val card: Color,
    val title: Color,
    val text: Color,
    val secondary: Color,
)

private fun widgetPalette(theme: ThemeMode): WidgetPalette? = when (theme) {
    ThemeMode.SYSTEM -> null
    ThemeMode.AMOLED -> WidgetPalette(
        bg = Color(0xFF000000),
        card = Color(0xFF1A1A1A),
        title = AmoledPrimary,
        text = Color.White,
        secondary = Color(0xFFCAC4D0),
    )
    ThemeMode.MONOCHROME -> WidgetPalette(
        bg = Color(0xFF000000),
        card = Color(0xFF1A1A1A),
        title = MonoPrimary,
        text = Color.White,
        secondary = Color(0xFFCAC4D0),
    )
    ThemeMode.SOLARIZED_OSAKA -> WidgetPalette(
        bg = SolBackground,
        card = SolSurfaceContainer,
        title = SolPrimary,
        text = SolOnSurface,
        secondary = SolOnSurfaceVariant,
    )
    ThemeMode.GRUVBOX -> WidgetPalette(
        bg = GrBackground,
        card = GrSurfaceContainer,
        title = GrPrimary,
        text = GrOnSurface,
        secondary = GrOnSurfaceVariant,
    )
    ThemeMode.TOKYO_NIGHT -> WidgetPalette(
        bg = TnBackground,
        card = TnSurfaceContainer,
        title = TnPrimary,
        text = TnOnSurface,
        secondary = TnOnSurfaceVariant,
    )
    ThemeMode.CATPPUCCIN -> WidgetPalette(
        bg = CpBackground,
        card = CpSurfaceContainer,
        title = CpPrimary,
        text = CpOnSurface,
        secondary = CpOnSurfaceVariant,
    )
}

/**
 * Виджет расписания на текущий день (Jetpack Glance).
 *
 * - Читает кэш Room напрямую (офлайн), поэтому работает без сети.
 * - Скруглённые углы (28.dp), стиль берётся из настроек приложения:
 *   AMOLED → чёрный фон + сиреневый акцент,
 *   SYSTEM → динамические Material You цвета Android API (GlanceTheme).
 * - Адаптив под размер через SizeMode.Responsive + LocalSize:
 *   2x2 — ультракомпакт (одна строка заголовка + ближайшая пара стеком),
 *   маленький — только первая пара + счётчик, большой — весь день.
 * - Тап по виджету открывает приложение (actionStartActivity).
 * - Ресайз включается флагом resizeMode в xml/schedule_widget_info.xml.
 */
class ScheduleWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "ScheduleWidget"
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp), // 2x2 клетки
            DpSize(220.dp, 160.dp),
            DpSize(320.dp, 260.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.applicationContext.appContainer()
        val group = container.settings.groupQuery.first().trim()
        val theme = container.settings.themeMode.first()
        val today = LocalDate.now()
        // Лог ДО provideContent: он сессию композиции не возвращает,
        // всё после него недостижимо.
        Log.d(TAG, "provideGlance: group='$group' theme=$theme")

        val lessons: List<WidgetLesson> = if (group.isBlank()) {
            emptyList()
        } else {
            container.scheduleRepository
                .observeDay(today, group)
                .first()
                .lessons
                .map {
                    WidgetLesson(
                        time = listOf(it.timeFrom, it.timeTo)
                            .filter { t -> t.isNotBlank() }
                            .joinToString("–"),
                        subject = it.subject,
                        meta = listOfNotNull(
                            it.room.takeIf { r -> r.isNotBlank() }?.let { r -> "ауд. $r" },
                            it.teachers.firstOrNull(),
                        ).joinToString(" • "),
                        type = it.type,
                    )
                }
        }

        provideContent {
            GlanceTheme {
                WidgetBody(
                    group = group,
                    date = today,
                    lessons = lessons,
                    theme = theme,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(
    group: String,
    date: LocalDate,
    lessons: List<WidgetLesson>,
    theme: ThemeMode,
) {
    val size = LocalSize.current
    val ultraCompact = size.height <= 125.dp // 2x2 клетки
    val compact = !ultraCompact && size.height < 170.dp
    val palette = widgetPalette(theme)

    // Акцент заголовка — в стиле приложения: фиксированный цвет темы
    // либо системный dynamic-акцент Android (GlanceTheme, API 31+).
    val accentFg: ColorProvider = palette?.title?.let(::ColorProvider)
        ?: GlanceTheme.colors.primary
    // Текст кнопки: тёмный на светлом акценте, иначе системный onPrimary.
    // Без явных цветов Glance рисует кнопку одинаково во всех темах.
    val buttonContent: ColorProvider = palette?.bg?.let(::ColorProvider)
        ?: GlanceTheme.colors.onPrimary
    val bg: ColorProvider = palette?.bg?.let(::ColorProvider)
        ?: GlanceTheme.colors.surface
    val cardBg: ColorProvider = palette?.card?.let(::ColorProvider)
        ?: GlanceTheme.colors.surfaceVariant
    val fg: ColorProvider = palette?.text?.let(::ColorProvider)
        ?: GlanceTheme.colors.onSurface
    val secondary: ColorProvider = palette?.secondary?.let(::ColorProvider)
        ?: GlanceTheme.colors.onSurfaceVariant

    val dateLine = date
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru")))
        .replaceFirstChar { it.uppercaseChar() }

    val outerPad = if (ultraCompact) 10.dp else 14.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .cornerRadius(if (ultraCompact) 22.dp else 28.dp)
            .padding(outerPad)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (ultraCompact) {
                // Раскладка как у виджета Google Calendar 2x2: дата слева
                // («5 Сб»), ниже — все пары дня блоками «предмет / время».
                // Кнопки «+» нет осознанно: пары привозит сайт вуза,
                // добавить событие вручную нельзя.
                val weekdayShort = date.dayOfWeek
                    .getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("ru"))
                    .replaceFirstChar { it.uppercaseChar() }
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = TextStyle(fontWeight = FontWeight.Bold, color = fg, fontSize = 22.sp),
                    )
                    Spacer(modifier = GlanceModifier.width(5.dp))
                    Text(
                        text = weekdayShort,
                        style = TextStyle(color = secondary, fontSize = 13.sp),
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
                when {
                    group.isBlank() -> {
                        Text(
                            "Укажи группу в приложении",
                            style = TextStyle(color = secondary, fontSize = 12.sp),
                            maxLines = 2,
                        )
                    }
                    lessons.isEmpty() -> {
                        Text(
                            "Пар нет 🎉",
                            style = TextStyle(color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                    else -> {
                        lessons.forEach { lesson ->
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(cardBg)
                                    .cornerRadius(12.dp)
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                            ) {
                                Column(modifier = GlanceModifier.fillMaxWidth()) {
                                    Text(
                                        text = lesson.subject,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = fg,
                                            fontSize = 13.sp,
                                        ),
                                        maxLines = 1,
                                    )
                                    if (lesson.time.isNotEmpty()) {
                                        Text(
                                            text = lesson.time,
                                            style = TextStyle(
                                                color = ColorProvider(lesson.type.accentFor(theme)),
                                                fontSize = 11.sp,
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
                // Кнопка обновления — в самом низу, после пар. В 2x2 места
                // под полную кнопку нет, поэтому компактная текстовая.
                if (group.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "Обновить",
                            style = TextStyle(color = accentFg, fontSize = 11.sp),
                            modifier = GlanceModifier.clickable(
                                actionRunCallback<RefreshAction>(),
                            ),
                        )
                    }
                }
                return@Column
            }

            // Заголовок: только группа и дата, без числителя/знаменателя.
            Text(
                text = if (group.isBlank()) "Расписание" else "Группа $group",
                style = TextStyle(fontWeight = FontWeight.Bold, color = accentFg, fontSize = 17.sp),
                maxLines = 1,
            )
            Text(
                text = dateLine,
                style = TextStyle(color = secondary, fontSize = 13.sp),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(10.dp))

            when {
                group.isBlank() -> {
                    Text(
                        "Укажи группу в приложении",
                        style = TextStyle(color = secondary, fontSize = 14.sp),
                    )
                }
                lessons.isEmpty() -> {
                    Text(
                        "Сегодня пар нет 🎉",
                        style = TextStyle(color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    )
                }
                compact -> {
                    // Средний размер (3 колонки): все пары плотными
                    // строками, как в 2x2, — ничего не прячем.
                    lessons.forEach { lesson ->
                        UltraLessonRow(lesson = lesson, fg = fg, secondary = secondary, theme = theme)
                    }
                }
                else -> {
                    val visible = lessons.take(5)
                    visible.forEachIndexed { i, lesson ->
                        LessonRow(lesson = lesson, fg = fg, secondary = secondary, cardBg = cardBg, theme = theme)
                        if (i < visible.size - 1) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                    if (lessons.size > visible.size) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            "…и ещё ${lessons.size - visible.size}",
                            style = TextStyle(color = secondary, fontSize = 13.sp),
                        )
                    }
                }
            }

            // Кнопка обновления — всегда в самом низу списка, после пар.
            // В компактных размерах места под полную кнопку нет.
            if (group.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(if (compact) 6.dp else 10.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (compact) {
                        Text(
                            text = "Обновить",
                            style = TextStyle(color = accentFg, fontSize = 11.sp),
                            modifier = GlanceModifier.clickable(
                                actionRunCallback<RefreshAction>(),
                            ),
                        )
                    } else {
                        androidx.glance.Button(
                            text = "Обновить",
                            onClick = actionRunCallback<RefreshAction>(),
                            colors = androidx.glance.ButtonDefaults.buttonColors(
                                backgroundColor = accentFg,
                                contentColor = buttonContent,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Плотная строка для маленьких размеров: цветная точка + время + предмет. */
@androidx.compose.runtime.Composable
private fun UltraLessonRow(
    lesson: WidgetLesson,
    fg: ColorProvider,
    secondary: ColorProvider,
    theme: ThemeMode,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(6.dp)
                .height(6.dp)
                .background(ColorProvider(lesson.type.accentFor(theme)))
                .cornerRadius(3.dp),
            contentAlignment = Alignment.Center,
        ) { }
        Spacer(modifier = GlanceModifier.width(5.dp))
        if (lesson.time.isNotEmpty()) {
            Text(
                text = lesson.time,
                style = TextStyle(color = secondary, fontSize = 11.sp),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(5.dp))
        }
        Text(
            text = lesson.subject,
            style = TextStyle(fontWeight = FontWeight.Bold, color = fg, fontSize = 12.sp),
            maxLines = 1,
        )
    }
}

/** Строка пары: цветной бейдж времени + предмет и детали. */
@androidx.compose.runtime.Composable
private fun LessonRow(
    lesson: WidgetLesson,
    fg: ColorProvider,
    secondary: ColorProvider,
    cardBg: ColorProvider,
    theme: ThemeMode,
) {
    val accent = lesson.type.accentFor(theme)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(cardBg)
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lesson.time.isNotEmpty()) {
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(accent.copy(alpha = 0.18f)))
                    .cornerRadius(8.dp)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = lesson.time,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(accent),
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.width(9.dp))
        }
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = lesson.subject,
                style = TextStyle(fontWeight = FontWeight.Bold, color = fg, fontSize = 14.sp),
                maxLines = 1,
            )
            if (lesson.meta.isNotEmpty()) {
                Text(
                    text = lesson.meta,
                    style = TextStyle(color = secondary, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
