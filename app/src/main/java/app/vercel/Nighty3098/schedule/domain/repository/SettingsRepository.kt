package app.vercel.Nighty3098.schedule.domain.repository

import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Настройки приложения (DataStore). */
interface SettingsRepository {
    val groupQuery: Flow<String>
    val themeMode: Flow<ThemeMode>
    val login: Flow<String>

    suspend fun setGroupQuery(value: String)
    suspend fun setThemeMode(value: ThemeMode)

    /**
     * Логин/пароль от личного кабинета Bitrix.
     * Расписание доступно только после авторизации, поэтому без них
     * сайт отвечает 302 → /auth/. Хранятся в DataStore; для продакшена
     * рекомендуется EncryptedSharedPreferences / Encrypted DataStore.
     */
    suspend fun setCredentials(login: String, password: String)
    suspend fun getPassword(): String?
}
