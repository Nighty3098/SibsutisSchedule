package app.vercel.Nighty3098.schedule.data.network

import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

/**
 * Единый OkHttp-клиент для сайта СибГУТИ.
 *
 * Важно:
 * - CookieJar обязателен: авторизация Bitrix живёт в cookies
 *   (PHPSESSID + BITRIX_SM_*), без них — 302 на /auth/.
 * - Реалистичные заголовки, иначе nginx/Bitrix может отдать пустой ответ
 *   или страницу входа (перехвачено в браузере, см. ТЗ).
 */
object SibsutisHttpClient {
    const val BASE_URL = "https://sibsutis.ru"
    const val AUTH_PATH = "/auth/"
    const val SCHEDULE_PATH = "/students/schedule/"
    const val AJAX_GROUPS_PATH = "/ajax/get_groups_soap.php"

    /** Тот самый User-Agent из перехваченного запроса (Firefox/Linux). */
    const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64; rv:154.0) Gecko/20100101 Firefox/154.0"

    fun create(debug: Boolean = false): OkHttpClient {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        val builder = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            // Дефолтные заголовки браузера для каждого запроса.
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("DNT", "1")
                    .build()
                chain.proceed(req)
            }
        if (debug) {
            builder.addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC),
            )
        }
        return builder.build()
    }

    /** Создаёт клиент с изолированной cookie-сессией (для тестов/виджета). */
    fun createWithJar(cookieJar: CookieJar): OkHttpClient =
        create().newBuilder().cookieJar(cookieJar).build()
}
