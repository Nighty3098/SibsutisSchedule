package app.vercel.Nighty3098.schedule.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
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
    }

    override val groupQuery: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.GROUP]?.trim().orEmpty() }

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { ThemeMode.fromNameOrDefault(it[Keys.THEME]) }

    override val login: Flow<String> = secure.loginFlow

    override suspend fun setGroupQuery(value: String) {
        context.settingsDataStore.edit { it[Keys.GROUP] = value.trim() }
    }

    override suspend fun setThemeMode(value: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = value.name }
    }

    override suspend fun setCredentials(login: String, password: String) {
        secure.save(login, password)
    }

    override suspend fun getPassword(): String? = secure.getPassword()

    suspend fun getLoginOnce(): String = secure.getLogin()
}
