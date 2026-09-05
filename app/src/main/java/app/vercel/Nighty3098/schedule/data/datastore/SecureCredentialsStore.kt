package app.vercel.Nighty3098.schedule.data.datastore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Общий DataStore настроек (строго один инстанс на файл "settings"). */
internal val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Логин/пароль Bitrix в шифрованном виде (Android Keystore, AES256).
 *
 * Ключ шифрования хранится в аппаратном/системном Keystore и не покидает
 * устройство; сам файл преференсов без него — шум. При первом доступе
 * автоматически переносит значения из старого открытого DataStore
 * (auth_login/auth_password) и затирает их там — вводить заново не нужно.
 */
class SecureCredentialsStore(private val appContext: Context) {

    companion object {
        private const val TAG = "SecureCreds"
        private const val PREFS_NAME = "secure_credentials"
        private const val KEY_LOGIN = "auth_login"
        private const val KEY_PASSWORD = "auth_password"
        private val LEGACY_LOGIN = stringPreferencesKey("auth_login")
        private val LEGACY_PASSWORD = stringPreferencesKey("auth_password")
    }

    private val _loginFlow = MutableStateFlow("")
    val loginFlow: Flow<String> = _loginFlow.asStateFlow()

    private val mutex = Mutex()
    private var ready = false

    private val prefs: SharedPreferences by lazy { openPrefs() }

    private fun openPrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // Сломанный Keystore на экзотических прошивках: лучше обычное
            // приватное хранилище приложения, чем падение при старте.
            Log.w(TAG, "EncryptedSharedPreferences недоступен, fallback: ${e.message}")
            appContext.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    /** Миграция из открытого DataStore + прогрев loginFlow. Идемпотентно. */
    suspend fun ensureReady() {
        if (ready) return
        mutex.withLock {
            if (ready) return
            withContext(Dispatchers.IO) {
                if (prefs.getString(KEY_LOGIN, "").isNullOrEmpty()) {
                    val legacy = appContext.settingsDataStore.data
                        .map { it[LEGACY_LOGIN].orEmpty() to it[LEGACY_PASSWORD].orEmpty() }
                        .first()
                    if (legacy.first.isNotEmpty() || legacy.second.isNotEmpty()) {
                        prefs.edit()
                            .putString(KEY_LOGIN, legacy.first)
                            .putString(KEY_PASSWORD, legacy.second)
                            .apply()
                        appContext.settingsDataStore.edit {
                            it.remove(LEGACY_LOGIN)
                            it.remove(LEGACY_PASSWORD)
                        }
                        Log.i(TAG, "Credentials перенесены из открытого DataStore в шифрованное хранилище")
                    }
                }
                _loginFlow.value = prefs.getString(KEY_LOGIN, "").orEmpty()
                ready = true
            }
        }
    }

    suspend fun save(login: String, password: String) = withContext(Dispatchers.IO) {
        ensureReady()
        prefs.edit()
            .putString(KEY_LOGIN, login.trim())
            .putString(KEY_PASSWORD, password)
            .apply()
        _loginFlow.value = login.trim()
    }

    suspend fun getPassword(): String? = withContext(Dispatchers.IO) {
        ensureReady()
        prefs.getString(KEY_PASSWORD, null).takeIf { !it.isNullOrEmpty() }
    }

    suspend fun getLogin(): String = withContext(Dispatchers.IO) {
        ensureReady()
        prefs.getString(KEY_LOGIN, "").orEmpty()
    }
}
