package app.vercel.Nighty3098.schedule.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Настройки: группа и тема — в открытом DataStore,
 * логин/пароль Bitrix — только в [SecureCredentialsStore] (шифрование AES256).
 */
class SettingsDataStore(
    private val context: Context,
    private val secure: SecureCredentialsStore,
) : SettingsRepository {

    private object Keys {
        val GROUP = stringPreferencesKey("group_query")
        val THEME = stringPreferencesKey("theme_mode")
        val CALENDAR_SYNC = booleanPreferencesKey("calendar_sync")
        val CALENDAR_ID = longPreferencesKey("calendar_id")
        val CALENDAR_NAME = stringPreferencesKey("calendar_name")
    }

    override val groupQuery: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.GROUP]?.trim().orEmpty() }

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { ThemeMode.fromNameOrDefault(it[Keys.THEME]) }

    override val login: Flow<String> = secure.loginFlow

    override val calendarSync: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.CALENDAR_SYNC] == true }

    override val calendarId: Flow<Long?> =
        context.settingsDataStore.data.map { it[Keys.CALENDAR_ID] }

    override val calendarName: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.CALENDAR_NAME].orEmpty() }

    override suspend fun setGroupQuery(value: String) {
        context.settingsDataStore.edit { it[Keys.GROUP] = value.trim() }
    }

    override suspend fun setThemeMode(value: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = value.name }
    }

    override suspend fun setCalendarSync(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CALENDAR_SYNC] = enabled }
    }

    override suspend fun setCalendarAccount(id: Long?, name: String) {
        context.settingsDataStore.edit {
            if (id == null) it.remove(Keys.CALENDAR_ID) else it[Keys.CALENDAR_ID] = id
            it[Keys.CALENDAR_NAME] = name
        }
    }

    override suspend fun setCredentials(login: String, password: String) {
        secure.save(login, password)
    }

    override suspend fun getPassword(): String? = secure.getPassword()

    suspend fun getLoginOnce(): String = secure.getLogin()
}
