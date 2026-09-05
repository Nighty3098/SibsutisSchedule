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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.domain.model.WeekParity
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

/** Один день повестки: дата + пары этого дня. */
private data class AgendaDay(
    val date: LocalDate,
    val lessons: List<Lesson>,
)

/**
 * Плоская строка повестки для LazyColumn (в Glance-lazy нет одиночного
 * item(), поэтому заголовки дней и счётчики — такие же элементы списка).
 */
private sealed interface AgendaRow {
    data class DayHeader(val date: LocalDate) : AgendaRow
    data class Event(val lesson: Lesson) : AgendaRow
    data class More(val count: Int) : AgendaRow
    data object NoLessons : AgendaRow
}

/** Максимум пар на день в повестке, остальное — счётчиком «…и ещё N». */
private const val AGENDA_MAX_LESSONS = 5

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
 * Виджет расписания в стиле повестки Google Календаря (Jetpack Glance).
 *
 * - Скруглённая карточка (28.dp), шапка с месяцем и кнопкой «+»
 *   (обновление), ниже — секции дней («Сегодня, 5 сентября»)
 *   с плоскими строками пар и цветной полоской типа слева.
 * - Пустые дни пропускаются (кроме сегодня — там «Пар нет»),
 *   показ: сегодня + до 2 ближайших учебных дней, максимум 5 пар
 *   на день, список прокручивается (LazyColumn, как у Google).
 * - Читает кэш Room напрямую (офлайн), поэтому работает без сети.
 * - Стиль берётся из настроек приложения, SYSTEM — динамические
 *   Material You цвета Android (GlanceTheme).
 * - Адаптив через SizeMode.Responsive + LocalSize: 2x2 — только
 *   сегодня в ультракомпакте, средний — 2 секции, большой — 3.
 * - Тап по шапке/событию открывает приложение (actionStartActivity).
 * - Ресайз включается флагом resizeMode в xml/schedule_widget_info.xml.
 */
class ScheduleWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "ScheduleWidget"

        /** На сколько дней вперёд ищем пары для повестки. */
        private const val AGENDA_LOOKAHEAD_DAYS = 6

        /** Максимум секций дней в повестке (сегодня + ближайшие с парами). */
        private const val AGENDA_MAX_SECTIONS = 3
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

        // Повестка: сегодня + ближайшие дни с парами (максимум 3 секции).
        // Разовые запросы (не подписки): виджет перерисовывается целиком,
        // реактивность не нужна, а subscribe/collect обходится дороже.
        // Ранний выход: дальше заполненных секций данные не нужны.
        val agenda: List<AgendaDay> = if (group.isBlank()) {
            emptyList()
        } else {
            buildList {
                for (offset in 0..AGENDA_LOOKAHEAD_DAYS) {
                    val date = today.plusDays(offset.toLong())
                    val dayLessons = container.scheduleRepository.getDayLessons(date, group)
                    if (offset == 0 || dayLessons.isNotEmpty()) {
                        add(AgendaDay(date, dayLessons))
                    }
                    if (size >= AGENDA_MAX_SECTIONS) break
                }
            }
        }

        provideContent {
            GlanceTheme {
                WidgetBody(
                    group = group,
                    today = today,
                    agenda = agenda,
                    theme = theme,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(
    group: String,
    today: LocalDate,
    agenda: List<AgendaDay>,
    theme: ThemeMode,
) {
    val size = LocalSize.current
    val ultraCompact = size.height <= 125.dp // 2x2 клетки
    val compact = !ultraCompact && size.height < 170.dp
    val palette = widgetPalette(theme)

    // Акцент — в стиле приложения: фиксированный цвет темы
    // либо системный dynamic-акцент Android (GlanceTheme, API 31+).
    val accentFg: ColorProvider = palette?.title?.let(::ColorProvider)
        ?: GlanceTheme.colors.primary
    val bg: ColorProvider = palette?.bg?.let(::ColorProvider)
        ?: GlanceTheme.colors.surface
    val fg: ColorProvider = palette?.text?.let(::ColorProvider)
        ?: GlanceTheme.colors.onSurface
    val secondary: ColorProvider = palette?.secondary?.let(::ColorProvider)
        ?: GlanceTheme.colors.onSurfaceVariant

    val ru = Locale.forLanguageTag("ru")
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
        if (ultraCompact) {
            UltraCompactBody(
                today = today,
                lessons = agenda.firstOrNull { it.date == today }?.lessons.orEmpty(),
                group = group,
                theme = theme,
                fg = fg,
                secondary = secondary,
                accentFg = accentFg,
                ru = ru,
            )
            return@Box
        }

        if (group.isBlank()) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                AgendaHeader(
                    today = today,
                    group = group,
                    bg = bg,
                    fg = fg,
                    secondary = secondary,
                    accentFg = accentFg,
                    ru = ru,
                )
                Spacer(modifier = GlanceModifier.height(10.dp))
                Text(
                    "Укажи группу в приложении",
                    style = TextStyle(color = secondary, fontSize = 14.sp),
                )
            }
            return@Box
        }

        // Повестка как у Google: шапка + прокручиваемый список секций дней.
        val sections = if (compact) agenda.take(2) else agenda.take(3)
        Column(modifier = GlanceModifier.fillMaxSize()) {
            AgendaHeader(
                today = today,
                group = group,
                bg = bg,
                fg = fg,
                secondary = secondary,
                accentFg = accentFg,
                ru = ru,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            // Плоский список строк: заголовки дней, события, счётчики.
            val rows = buildList {
                sections.forEach { day ->
                    add(AgendaRow.DayHeader(day.date))
                    if (day.lessons.isEmpty()) {
                        add(AgendaRow.NoLessons)
                    } else {
                        val visible = day.lessons.take(AGENDA_MAX_LESSONS)
                        visible.forEach { add(AgendaRow.Event(it)) }
                        if (day.lessons.size > visible.size) {
                            add(AgendaRow.More(day.lessons.size - visible.size))
                        }
                    }
                }
            }
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(rows) { row ->
                    when (row) {
                        is AgendaRow.DayHeader -> AgendaDayHeader(
                            date = row.date,
                            today = today,
                            fg = fg,
                            secondary = secondary,
                            accentFg = accentFg,
                            ru = ru,
                        )
                        is AgendaRow.Event -> AgendaEventRow(
                            lesson = row.lesson,
                            theme = theme,
                            fg = fg,
                            secondary = secondary,
                        )
                        is AgendaRow.More -> Text(
                            "…и ещё ${row.count}",
                            style = TextStyle(color = secondary, fontSize = 12.sp),
                        )
                        AgendaRow.NoLessons -> Text(
                            "Пар нет 🎉",
                            style = TextStyle(color = secondary, fontSize = 13.sp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Шапка в духе Google Календаря: значок-«календарик», месяц + год,
 * группа второй строкой и кнопка «+» справа (обновление данных).
 */
@androidx.compose.runtime.Composable
private fun AgendaHeader(
    today: LocalDate,
    group: String,
    bg: ColorProvider,
    fg: ColorProvider,
    secondary: ColorProvider,
    accentFg: ColorProvider,
    ru: Locale,
) {
    // Месяц шапки — всегда текущий, как у Google (повестка может уходить
    // в следующий месяц, но шапка показывает «где мы сейчас»).
    val monthTitle = today
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", ru))
        .replaceFirstChar { it.uppercaseChar() }
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Бейдж «31» — отсылка к иконке Google Календаря.
        Box(
            modifier = GlanceModifier
                .width(34.dp)
                .height(34.dp)
                .background(accentFg)
                .cornerRadius(10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "31",
                style = TextStyle(fontWeight = FontWeight.Bold, color = bg, fontSize = 15.sp),
            )
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = monthTitle,
                style = TextStyle(fontWeight = FontWeight.Bold, color = fg, fontSize = 16.sp),
                maxLines = 1,
            )
            if (group.isNotBlank()) {
                Text(
                    text = "Группа $group",
                    style = TextStyle(color = secondary, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        // «+» как у Google, но создаёт не событие (пары привозит сайт вуза),
        // а запускает обновление данных.
        Text(
            text = "+",
            style = TextStyle(color = accentFg, fontSize = 24.sp),
            modifier = GlanceModifier
                .padding(6.dp)
                .clickable(actionRunCallback<RefreshAction>()),
        )
    }
}

/**
 * Заголовок секции дня: крупное число слева, день недели
 * и «Сегодня • Числитель» справа — как дата-хедер повестки Google.
 */
@androidx.compose.runtime.Composable
private fun AgendaDayHeader(
    date: LocalDate,
    today: LocalDate,
    fg: ColorProvider,
    secondary: ColorProvider,
    accentFg: ColorProvider,
    ru: Locale,
) {
    val isToday = date == today
    val relative = when (date) {
        today -> "Сегодня"
        today.plusDays(1) -> "Завтра"
        else -> null
    }
    val sub = listOfNotNull(relative, WeekParity.weekName(date)).joinToString(" • ")
    val weekday = date
        .format(DateTimeFormatter.ofPattern("EEEE", ru))
        .replaceFirstChar { it.uppercaseChar() }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = if (isToday) accentFg else fg,
                fontSize = 22.sp,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = weekday,
                style = TextStyle(fontWeight = FontWeight.Medium, color = fg, fontSize = 13.sp),
                maxLines = 1,
            )
            Text(
                text = sub,
                style = TextStyle(color = secondary, fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}

/**
 * Строка пары: тонкая цветная полоска типа слева (цвета те же,
 * что в приложении), предмет жирным, время + аудитория и
 * преподаватель — вторичным. Тап открывает приложение.
 */
@androidx.compose.runtime.Composable
private fun AgendaEventRow(
    lesson: Lesson,
    theme: ThemeMode,
    fg: ColorProvider,
    secondary: ColorProvider,
) {
    val accent = lesson.type.accentFor(theme)
    val time = listOf(lesson.timeFrom, lesson.timeTo)
        .filter { it.isNotBlank() }
        .joinToString("–")
    val timeRoom = listOfNotNull(
        time.takeIf { it.isNotEmpty() },
        lesson.room.takeIf { it.isNotBlank() }?.let { "ауд. $it" },
    ).joinToString(" • ")
    val teachers = lesson.teachers.joinToString(", ")
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(38.dp)
                .background(ColorProvider(accent))
                .cornerRadius(2.dp),
            contentAlignment = Alignment.Center,
        ) { }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = lesson.subject,
                style = TextStyle(fontWeight = FontWeight.Bold, color = fg, fontSize = 14.sp),
                maxLines = 2,
            )
            if (timeRoom.isNotEmpty()) {
                Text(
                    text = timeRoom,
                    style = TextStyle(color = secondary, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            if (teachers.isNotEmpty()) {
                Text(
                    text = teachers,
                    style = TextStyle(color = secondary, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Ультракомпакт 2x2: дата + пары сегодня плоским списком. */
@androidx.compose.runtime.Composable
private fun UltraCompactBody(
    today: LocalDate,
    lessons: List<Lesson>,
    group: String,
    theme: ThemeMode,
    fg: ColorProvider,
    secondary: ColorProvider,
    accentFg: ColorProvider,
    ru: Locale,
) {
    val weekdayShort = today.dayOfWeek
        .getDisplayName(java.time.format.TextStyle.SHORT, ru)
        .replaceFirstChar { it.uppercaseChar() }
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = today.dayOfMonth.toString(),
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
                    val time = listOf(lesson.timeFrom, lesson.timeTo)
                        .filter { it.isNotBlank() }
                        .joinToString("–")
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    ) {
                        Text(
                            text = lesson.subject,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                fontSize = 13.sp,
                            ),
                            maxLines = 1,
                        )
                        if (time.isNotEmpty()) {
                            Text(
                                text = time,
                                style = TextStyle(
                                    color = ColorProvider(lesson.type.accentFor(theme)),
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        // В 2x2 места под полную кнопку нет — компактная текстовая.
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
    }
}
