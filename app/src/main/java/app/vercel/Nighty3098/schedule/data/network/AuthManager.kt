package app.vercel.Nighty3098.schedule.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * Авторизация в личном кабинете (Bitrix CMS).
 *
 * Сайт отдаёт расписание только авторизованным (иначе 302 → /auth/).
 * Форма стандартная Bitrix: AUTH_FORM=Y, TYPE=AUTH, USER_LOGIN/USER_PASSWORD.
 * Имена полей дополнительно проверяем через Jsoup на странице /auth/
 * (устойчиво к переименованиям).
 */
class AuthManager(
    private val client: OkHttpClient,
    private val baseUrl: String = SibsutisHttpClient.BASE_URL,
) {
    class AuthFailedException(message: String) : Exception(message)

    /**
     * Логин. Возвращает true при успехе.
     * Успех = на повторном GET /auth/ больше нет <input type=password>.
     */
    suspend fun login(login: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val authUrl = baseUrl + SibsutisHttpClient.AUTH_PATH

            // 1. GET страницы входа: забираем PHPSESSID + точные имена полей.
            val (loginField, passwordField, hidden) = parseLoginForm(authUrl)

            // 2. POST credentials.
            val form = FormBody.Builder().apply {
                hidden.forEach { (k, v) -> add(k, v) }
                add("AUTH_FORM", "Y")
                add("TYPE", "AUTH")
                add(loginField, login)
                add(passwordField, password)
                add("USER_REMEMBER", "Y")
                if ("Login" !in hidden) add("Login", "Войти")
            }.build()

            val post = Request.Builder()
                .url(authUrl + "?login=yes")
                .post(form)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", authUrl)
                .build()
            client.newCall(post).execute().use { resp ->
                // Bitrix отвечает 200 или редиректом на backurl — оба варианта норма.
                if (resp.code >= 400) throw AuthFailedException("Сервер ответил ${resp.code}")
            }

            // 3. Проверка: поле пароля должно исчезнуть.
            !hasPasswordField(authUrl)
        }

    private fun parseLoginForm(authUrl: String): Triple<String, String, Map<String, String>> {
        var loginField = "USER_LOGIN"
        var passwordField = "USER_PASSWORD"
        val hidden = mutableMapOf<String, String>()
        try {
            val get = Request.Builder().url(authUrl).get().build()
            client.newCall(get).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                val doc = Jsoup.parse(html, authUrl)
                // Ищем именно форму входа (в ней есть input[type=password]).
                val forms = doc.select("form")
                for (form in forms) {
                    if (form.select("input[type=password]").isEmpty()) continue
                    form.select("input[name]").forEach { input ->
                        val name = input.attr("name")
                        val type = input.attr("type").lowercase().ifEmpty { "text" }
                        when {
                            type == "password" -> passwordField = name
                            (type == "text" || type == "email") &&
                                "login" in name.uppercase() -> loginField = name
                            type == "hidden" || type == "submit" ->
                                hidden[name] = input.attr("value")
                        }
                    }
                    return Triple(loginField, passwordField, hidden)
                }
            }
        } catch (_: Exception) {
            // Игнорируем: ниже сработает запасной стандартный набор Bitrix.
        }
        return Triple(loginField, passwordField, hidden)
    }

    private fun hasPasswordField(authUrl: String): Boolean {
        return try {
            val get = Request.Builder().url(authUrl).get().build()
            client.newCall(get).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                Jsoup.parse(html).select("input[type=password]").isNotEmpty()
            }
        } catch (_: Exception) {
            true // при сетевой ошибке считаем, что не вошли
        }
    }
}
