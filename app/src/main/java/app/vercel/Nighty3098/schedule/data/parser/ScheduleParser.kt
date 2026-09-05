package app.vercel.Nighty3098.schedule.data.parser

import app.vercel.Nighty3098.schedule.domain.model.LessonType
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * Разбор страницы расписания СибГУТИ.
 *
 * Ключевое знание о сайте (подтверждено разбором go-парсера
 * BLXCKBXXST/sibsutis-schedule и python-парсера yvlmm/sibsutis-schedule-api):
 * расписание НЕ лежит в HTML-таблице напрямую — страница встраивает его как
 * JS-переменные `days[1]..days[14]` (JSON-строки в одинарных кавычках),
 * а браузер рендерит на клиенте. days[1..7] — неделя «числитель»,
 * days[8..14] — «знаменатель»; день недели = (idx-1) % 7.
 *
 * Поэтому: Jsoup — чтобы надёжно достать <script> блоки,
 * Regex — чтобы вытащить days[i] = '...',
 * org.json — чтобы разобрать JSON слотов времени.
 *
 * Структура JSON слота:
 * ScheduleCell[] { DateBegin: "0001-01-01T09:50:00", DateEnd: …,
 *   Subgroup[] { DISCIPLINE, TYPE_LESSON, TEACHER (string|array|null),
 *     CLASSROOM, GROUP, SUBGROUP } }
 *
 * PROJECT_DATES сознательно игнорируем: это даты сдачи семестрового проекта,
 * одинаковые для всех пар группы, а не даты проведения пар.
 */
object ScheduleParser {

    /** days[7] = '{...}' — \b важно, чтобы не цеплять fact_schedule_days. */
    private val DaysRe = Regex("""\bdays\[(\d{1,2})]\s*=\s*'(.+)'""")

    private val TimeRe = Regex("""(\d{1,2}):(\d{2})""")

    class NoScheduleDataException :
        Exception("На странице нет данных расписания (требуется вход или изменилась вёрстка).")

    data class ParsedLesson(
        val weekIndex: Int,
        val dayIndex: Int,
        val number: Int,
        val timeFrom: String,
        val timeTo: String,
        val subject: String,
        val typeRaw: String,
        val type: LessonType,
        val teachers: List<String>,
        val room: String,
        val subgroup: String,
        val groups: List<String>,
    )

    data class ParsedSchedule(
        val lessons: List<ParsedLesson>,
        /** Подпись с сайта (название группы), если удалось вытащить. */
        val title: String = "",
    )

    fun parse(groupQuery: String, html: String): ParsedSchedule {
        val doc = Jsoup.parse(html)

        // Заголовок/название группы — best effort, не критично.
        val title = doc.selectFirst("h1, h2, .schedule__title, title")
            ?.text()?.trim().orEmpty()
            .takeIf { it.isNotEmpty() && "расписание" !in it.lowercase() }
            .orEmpty()

        val lessons = parseDaysJson(doc)
        if (lessons.isNotEmpty()) return ParsedSchedule(lessons, title)

        // Фолбэк: вдруг сайт стал отдавать таблицу (требование ТЗ упоминает таблицы).
        val tableLessons = parseTableFallback(groupQuery, doc)
        if (tableLessons.isNotEmpty()) return ParsedSchedule(tableLessons, title)

        throw NoScheduleDataException()
    }

    // ---------- Основной путь: days[1..14] ----------

    private fun parseDaysJson(doc: org.jsoup.nodes.Document): List<ParsedLesson> {
        val out = mutableListOf<ParsedLesson>()
        // Скрипты перебираем через Jsoup (требование: парсинг через Jsoup).
        val scripts = doc.select("script")
        for (script in scripts) {
            // data() — сырое содержимое script без HTML-сущностей.
            val js = script.data().ifEmpty { script.html() }
            if ("days[" !in js) continue
            for (m in DaysRe.findAll(js)) {
                val idx = m.groupValues[1].toIntOrNull() ?: continue
                if (idx !in 1..14) continue
                val weekIndex = if (idx <= 7) 0 else 1
                val dayIndex = (idx - 1) % 7
                // Снимаем JS-экранирование одинарных кавычек.
                val rawJson = m.groupValues[2].replace("\\'", "'")
                out += parseDayCells(rawJson, weekIndex, dayIndex)
            }
        }
        return out.sortedWith(compareBy({ it.weekIndex }, { it.dayIndex }, { it.number }))
    }

    private fun parseDayCells(dayJson: String, weekIndex: Int, dayIndex: Int): List<ParsedLesson> {
        val out = mutableListOf<ParsedLesson>()
        val root = try {
            JSONObject(dayJson)
        } catch (_: Exception) {
            return out
        }
        val cells = root.optJSONArray("ScheduleCell") ?: return out
        for (i in 0 until cells.length()) {
            val cell = cells.optJSONObject(i) ?: continue
            val from = hhmm(cell.optString("DateBegin"))
            val to = hhmm(cell.optString("DateEnd"))
            val subgroups = cell.optJSONArray("Subgroup") ?: continue
            for (j in 0 until subgroups.length()) {
                val item = subgroups.optJSONObject(j) ?: continue
                val subject = item.optString("DISCIPLINE").trim()
                if (subject.isEmpty()) continue // пустой слот
                out += ParsedLesson(
                    weekIndex = weekIndex,
                    dayIndex = dayIndex,
                    number = i + 1, // порядковый номер пары в дне — по слоту времени
                    timeFrom = from,
                    timeTo = to,
                    subject = subject,
                    typeRaw = item.optString("TYPE_LESSON").trim(),
                    type = LessonType.fromRaw(item.optString("TYPE_LESSON")),
                    teachers = flexStrings(item, "TEACHER"),
                    room = item.optString("CLASSROOM").trim(),
                    subgroup = item.optString("SUBGROUP").trim().removeNullLiteral(),
                    groups = flexStrings(item, "GROUP"),
                )
            }
        }
        return out
    }

    /** Поле бывает массивом строк, одной строкой или null. */
    private fun flexStrings(obj: JSONObject, key: String): List<String> {
        if (!obj.has(key) || obj.isNull(key)) return emptyList()
        val raw = obj.opt(key) ?: return emptyList()
        return when (raw) {
            is JSONArray -> buildList {
                for (i in 0 until raw.length()) {
                    val s = raw.optString(i, "").trim()
                    if (s.isNotEmpty() && s != "null") add(s)
                }
            }
            is String -> {
                val s = raw.trim()
                if (s.isEmpty() || s.equals("null", ignoreCase = true)) emptyList() else listOf(s)
            }
            else -> {
                val s = raw.toString().trim()
                if (s.isEmpty() || s == "null") emptyList() else listOf(s)
            }
        }
    }

    private fun String.removeNullLiteral(): String =
        if (equals("null", ignoreCase = true)) "" else this

    private fun hhmm(dateTime: String): String {
        val m = TimeRe.find(dateTime) ?: return ""
        val h = m.groupValues[1].padStart(2, '0')
        return "$h:${m.groupValues[2]}"
    }

    // ---------- Запасной путь: HTML-таблица ----------

    /**
     * Если сайт когда-нибудь станет отдавать расписание таблицей,
     * пробуем разобрать строки вида: время | предмет | тип | аудитория | преподаватель.
     * Best-effort: ищем строки с временем ЧЧ:ММ.
     */
    private fun parseTableFallback(
        groupQuery: String,
        doc: org.jsoup.nodes.Document,
    ): List<ParsedLesson> {
        val out = mutableListOf<ParsedLesson>()
        val rows = doc.select("table tr")
        val timeRowRe = Regex("""(\d{1,2}:\d{2})\s*[–—-]\s*(\d{1,2}:\d{2})""")
        for (row in rows) {
            val cells = row.select("td, th").map { it.text().trim() }
            if (cells.size < 2) continue
            val joined = cells.joinToString(" | ")
            val tm = timeRowRe.find(joined) ?: continue
            val subject = cells.getOrElse(1) { "" }.trim()
            if (subject.isEmpty()) continue
            out += ParsedLesson(
                weekIndex = 0,
                dayIndex = 0,
                number = out.size + 1,
                timeFrom = tm.groupValues[1],
                timeTo = tm.groupValues[2],
                subject = subject,
                typeRaw = cells.getOrElse(2) { "" },
                type = LessonType.fromRaw(cells.getOrElse(2) { "" }),
                teachers = listOfNotNull(cells.getOrNull(4)?.takeIf { it.isNotBlank() }),
                room = cells.getOrElse(3) { "" },
                subgroup = "",
                groups = listOf(groupQuery),
            )
        }
        return out
    }
}
