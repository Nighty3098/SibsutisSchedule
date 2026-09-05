package app.vercel.Nighty3098.schedule.domain.repository

import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Настройки приложения (DataStore). */
interface SettingsRepository {
    val groupQuery: Flow<String>
    val themeMode: Flow<ThemeMode>
    val login: Flow<String>

    /** Включена ли запись пар в календарь устройства. */
    val calendarSync: Flow<Boolean>

    /** Выбранный календарь для записи (null — не выбран). */
    val calendarId: Flow<Long?>
    val calendarName: Flow<String>

    suspend fun setGroupQuery(value: String)
    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setCalendarSync(enabled: Boolean)
    suspend fun setCalendarAccount(id: Long?, name: String)

    /**
     * Логин/пароль от личного кабинета Bitrix.
     * Расписание доступно только после авторизации, поэтому без них
     * сайт отвечает 302 → /auth/. Хранятся в DataStore; для продакшена
     * рекомендуется EncryptedSharedPreferences / Encrypted DataStore.
     */
    suspend fun setCredentials(login: String, password: String)
    suspend fun getPassword(): String?
}
