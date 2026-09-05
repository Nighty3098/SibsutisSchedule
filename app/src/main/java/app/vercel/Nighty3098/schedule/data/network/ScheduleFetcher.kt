package app.vercel.Nighty3098.schedule.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Загрузка HTML страницы расписания авторизованным клиентом.
 *
 * URL вида /students/schedule/?type=student&group={id}.
 * Анонимов сайт разворачивает 302 → /auth/ — это отлавливаем и
 * бросаем [AuthRequiredException], чтобы репозиторий сделал login + retry.
 */
class ScheduleFetcher(
    private val client: OkHttpClient,
    private val baseUrl: String = SibsutisHttpClient.BASE_URL,
) {
    class AuthRequiredException :
        Exception("Требуется вход: расписание доступно только после авторизации.")

    class SiteUnavailableException(cause: String, parent: Throwable? = null) :
        Exception(cause, parent)

    suspend fun fetchHtml(groupId: String): String = withContext(Dispatchers.IO) {
        val url = (baseUrl + SibsutisHttpClient.SCHEDULE_PATH).toHttpUrl()
            .newBuilder()
            .addQueryParameter("type", "student")
            .addQueryParameter("group", groupId)
            .build()
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Referer", baseUrl + "/")
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                // OkHttp следует за редиректами: проверяем, куда пришли.
                val finalPath = resp.request.url.encodedPath
                if ("/auth/" in finalPath) throw AuthRequiredException()
                if (resp.code != 200) {
                    throw SiteUnavailableException("Сервер ответил ${resp.code}")
                }
                val html = resp.body?.string().orEmpty()
                // Эвристика истёкшей сессии: вместо расписания — форма входа.
                if ("AUTH_FORM" in html && "password" in html.lowercase() &&
                    "days[" !in html
                ) {
                    throw AuthRequiredException()
                }
                if (html.isBlank()) throw SiteUnavailableException("Пустой ответ сервера")
                html
            }
        } catch (e: AuthRequiredException) {
            throw e
        } catch (e: SiteUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw SiteUnavailableException("Нет соединения: ${e.message}", e)
        }
    }
}
