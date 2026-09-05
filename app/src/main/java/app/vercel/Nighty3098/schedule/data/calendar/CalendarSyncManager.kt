package app.vercel.Nighty3098.schedule.data.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import app.vercel.Nighty3098.schedule.data.datastore.SettingsDataStore
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Календарь устройства, доступный для записи. */
data class DeviceCalendar(
    val id: Long,
    val name: String,
    val account: String,
)

/** Готовые поля события — чистая проекция пары на дату (покрыта юнит-тестами). */
data class CalendarFields(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val location: String,
    val description: String,
)

/**
 * Синхронизация пар в календарь устройства через CalendarProvider.
 *
 * Двухнедельный шаблон из Room проецируется на реальные даты
 * ([SYNC_DAYS_AHEAD] дней вперёд от сегодня) и пишется событиями.
 * Все события помечены маркером [MARKER] в описании: по нему же
 * они находятся и удаляются при пересинхронизации/выключении —
 * чужие события не трогаем, дублей не создаём (перед записью
 * старые наши события удаляются).
 *
 * Если на устройстве подключён Google-аккаунт, записанные события
 * сами синхронизируются в Google Calendar системой.
 */
class CalendarSyncManager(
    context: Context,
    private val schedule: ScheduleRepository,
    private val settings: SettingsDataStore,
) {
    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver

    /**
     * Сериализация синков: два параллельных syncNow (тап + воркер,
     * refresh + воркер) чередовались как delete/delete/insert/insert
     * и давали дубли. С мьютексом каждый синк — атомарное
     * «удалить всё наше → записать заново».
     */
    private val syncMutex = Mutex()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Календари, доступные для записи. Требует READ_CALENDAR, иначе пусто. */
    suspend fun availableCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        runCatching {
            val out = mutableListOf<DeviceCalendar>()
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.ACCOUNT_TYPE,
                ),
                null, null,
                CalendarContract.Calendars.ACCOUNT_NAME,
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameI = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accI = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val typeI = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                while (c.moveToNext()) {
                    out += DeviceCalendar(
                        id = c.getLong(idI),
                        name = c.getString(nameI).orEmpty().ifEmpty { c.getString(accI).orEmpty() },
                        account = "${c.getString(accI).orEmpty()} (${c.getString(typeI).orEmpty()})",
                    )
                }
            }
            // Google-календари первыми — в них, скорее всего, и хотят писать.
            out.sortedBy { !it.account.contains("google", ignoreCase = true) }
        }.getOrElse {
            Log.w(TAG, "availableCalendars failed: ${it.message}")
            emptyList()
        }.also { Log.d(TAG, "availableCalendars: found=${it.size}") }
    }

    /** Синхронизация, только если включена в настройках. null — выключена. */
    suspend fun syncIfEnabled(): Result<Int>? {
        if (!settings.calendarSync.first()) return null
        if (!hasPermission()) {
            Log.w(TAG, "sync пропущен: нет разрешений календаря")
            return Result.failure(SecurityException("Нет доступа к календарю"))
        }
        return syncNow()
    }

    /**
     * Полная пересинхронизация: удалить наши события, записать заново.
     * @return Result с числом записанных событий.
     */
    suspend fun syncNow(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            syncMutex.withLock {
                doSyncLocked()
            }
        }
    }

    private suspend fun doSyncLocked(): Int {
        val group = settings.groupQuery.first().trim()
        Log.d(TAG, "syncNow start: group='$group'")
        require(group.isNotEmpty()) { "Не указана группа" }
        require(hasPermission()) { "Нет доступа к календарю" }
        // Один запрос списка на весь синк: если провайдер transient-пуст —
        // прерываемся ДО вставки, иначе старый батч не удалится и будут дубли.
        val calendars = availableCalendars()
        require(calendars.isNotEmpty()) { "Нет доступных календарей" }
        // Протухший сохранённый id (календарь/аккаунт удалён с телефона) —
        // перевыбираем, а не пишем в пустоту.
        val storedId = settings.calendarId.first()
        val target = calendars.firstOrNull { it.id == storedId }
            ?: calendars.first().also {
                settings.setCalendarAccount(it.id, it.name)
            }
        val calendarId = target.id
        // Чистим маркер ВО ВСЕХ календарях, а не только в выбранном:
        // иначе при смене календаря или серверных «воскрешениях» старые
        // батчи копятся рядом с новым и выглядят как дубли.
        calendars.forEach { deleteAppEvents(it.id) }
        var count = 0
        val today = LocalDate.now()
        for (offset in 0 until SYNC_DAYS_AHEAD) {
            val date = today.plusDays(offset.toLong())
            // Схлопываем подгруппы: один слот — одно событие, без подгрупп.
            val dayLessons = collapseSubgroups(schedule.getDayLessons(date, group))
            // Пары без валидного времени в событие не превратить — пропускаем.
            for (lesson in dayLessons) {
                val fields = lesson.toCalendarFields(date, marker(group)) ?: continue
                insertEvent(calendarId, fields)
                count++
            }
        }
        Log.d(TAG, "sync: записано событий=$count группа=$group календарь=$calendarId")
        return count
    }

    /** Удалить все наши события (при выключении синхронизации). */
    suspend fun clearAppEvents() = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext
        runCatching {
            // Под мьютексом: иначе гонка с летящим syncNow даёт рваное
            // состояние (часть батча стёрта конкурентной чисткой).
            syncMutex.withLock {
                val stored = settings.calendarId.first()
                if (stored != null) {
                    deleteAppEvents(stored)
                } else {
                    // Календарь не выбран (или удалён) — чистим маркер везде.
                    availableCalendars().forEach { deleteAppEvents(it.id) }
                }
            }
        }.onFailure { Log.w(TAG, "clear failed: ${it.message}") }
    }

    // ---------- provider ----------

    private fun deleteAppEvents(calendarId: Long) {
        val deleted = resolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.CALENDAR_ID}=? AND " +
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "$MARKER%"),
        )
        Log.d(TAG, "deleteAppEvents: удалено=$deleted календарь=$calendarId")
    }

    private fun insertEvent(calendarId: Long, f: CalendarFields) {
        resolver.insert(
            CalendarContract.Events.CONTENT_URI,
            ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, f.title)
                put(CalendarContract.Events.DESCRIPTION, f.description)
                put(CalendarContract.Events.EVENT_LOCATION, f.location)
                put(CalendarContract.Events.DTSTART, f.startMillis)
                put(CalendarContract.Events.DTEND, f.endMillis)
                put(
                    CalendarContract.Events.EVENT_TIMEZONE,
                    ZoneId.systemDefault().id,
                )
                // Без напоминаний по умолчанию — меньше шума.
                put(CalendarContract.Events.HAS_ALARM, 0)
                put(
                    CalendarContract.Events.AVAILABILITY,
                    CalendarContract.Events.AVAILABILITY_BUSY,
                )
            },
        )
    }

    companion object {
        const val TAG = "CalendarSync"

        /** Дней вперёд от сегодня пишется в календарь (4 недели). */
        const val SYNC_DAYS_AHEAD = 28

        /** Маркер наших событий — первая строка DESCRIPTION. */
        const val MARKER = "[SibsutisSchedule]"

        fun marker(group: String): String = "$MARKER group=$group"

        /**
         * Схлопывание подгрупп: пары одного слота (время + предмет),
         * разбитые на подгруппы 1/2/3, превращаются в ОДНО событие.
         * Преподаватели и аудитории объединяются, подгруппа затирается —
         * поэтому в заголовок и описание она не попадает.
         * Пары разного времени или разных предметов не трогаем.
         */
        fun collapseSubgroups(lessons: List<Lesson>): List<Lesson> =
            lessons.groupBy { Triple(it.timeFrom.trim(), it.timeTo.trim(), it.subject.trim()) }
                .values.map { slot ->
                    if (slot.size == 1) {
                        slot.first().copy(subgroup = "")
                    } else {
                        val first = slot.first()
                        first.copy(
                            number = slot.minOf { it.number },
                            teachers = slot.flatMap { it.teachers }.distinct(),
                            room = slot.map { it.room.trim() }
                                .filter { it.isNotEmpty() }.distinct()
                                .joinToString(", "),
                            subgroup = "",
                            groups = slot.flatMap { it.groups }.distinct(),
                            typeRaw = slot.firstOrNull { it.typeRaw.isNotBlank() }
                                ?.typeRaw.orEmpty(),
                        )
                    }
                }.sortedWith(compareBy({ it.startTime }, { it.number }))

        /**
         * Проекция пары на конкретную дату. null — писать нечего
         * (нет валидного времени начала/конца).
         */
        fun Lesson.toCalendarFields(date: LocalDate, marker: String): CalendarFields? {
            val start = startTime ?: return null
            val end = endTime ?: return null
            if (!end.isAfter(start)) return null
            val zone = ZoneId.systemDefault()
            val subgroupName = subgroup.trim()
            // Подгруппу — в заголовок: иначе пары подгрупп 1/2/3 в одно время
            // выглядят в календаре как три одинаковые записи («дубли»).
            val title = if (subgroupName.isEmpty()) {
                subject
            } else {
                "$subject ($subgroupName)"
            }
            val lines = mutableListOf(marker)
            if (typeRaw.isNotBlank()) lines += "Тип: $typeRaw"
            if (teachers.isNotEmpty()) lines += "Преподаватель: ${teachers.joinToString(", ")}"
            // На сайте подгруппа уже приходит как «Подгруппа N» — не дублируем слово.
            if (subgroupName.isNotEmpty()) {
                lines += if (subgroupName.startsWith("подгрупп", ignoreCase = true)) {
                    subgroupName
                } else {
                    "Подгруппа: $subgroupName"
                }
            }
            if (groups.isNotEmpty()) lines += "Группы: ${groups.joinToString(", ")}"
            return CalendarFields(
                title = title,
                startMillis = date.atTime(start).atZone(zone).toInstant().toEpochMilli(),
                endMillis = date.atTime(end).atZone(zone).toInstant().toEpochMilli(),
                location = room,
                description = lines.joinToString("\n"),
            )
        }
    }
}
