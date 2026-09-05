package app.vercel.Nighty3098.schedule.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Превращает человекочитаемый запрос в ID для URL расписания.
 *
 * - Если ввод — чисто цифры (например, "3414"), это уже ID: возвращаем как есть.
 *   Так работает пример из ТЗ (?type=student&group=3414).
 * - Иначе ищем через тот же AJAX-эндпоинт, что и выпадающий список select2
 *   на сайте: GET /ajax/get_groups_soap.php?search_group=<query>
 *   → {"results":[{"id":..,"text":".."}]}.
 */
class GroupResolver(
    private val client: OkHttpClient,
    private val baseUrl: String = SibsutisHttpClient.BASE_URL,
) {
    data class Match(val id: String, val text: String)

    class NotFoundException(query: String) :
        Exception("Группа «$query» не найдена. Проверь номер/название.")

    class AmbiguousException(val options: List<Match>) :
        Exception(
            "Найдено несколько групп, уточни запрос:\n" +
                options.take(15).joinToString("\n") { "• ${it.text}" },
        )

    suspend fun resolveId(query: String): Match = withContext(Dispatchers.IO) {
        val q = query.trim()
        require(q.isNotEmpty()) { "Пустой запрос группы" }
        if (q.all { it.isDigit() }) return@withContext Match(id = q, text = q)

        val url = (baseUrl + SibsutisHttpClient.AJAX_GROUPS_PATH).toHttpUrl()
            .newBuilder()
            .addQueryParameter("search_group", q)
            .build()
        val req = Request.Builder()
            .url(url)
            .get()
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", baseUrl + SibsutisHttpClient.SCHEDULE_PATH)
            .build()
        val body = client.newCall(req).execute().use { resp ->
            if (resp.code != 200) throw Exception("Поиск группы: сервер ответил ${resp.code}")
            resp.body?.string().orEmpty()
        }
        val matches = parseResults(body)
        when {
            matches.isEmpty() -> throw NotFoundException(q)
            matches.size == 1 -> matches.first()
            else -> {
                // Точное совпадение по названию выигрывает, иначе — просим уточнить.
                val exact = matches.filter { it.text.trim().equals(q, ignoreCase = true) }
                if (exact.size == 1) exact.first() else throw AmbiguousException(matches)
            }
        }
    }

    internal fun parseResults(body: String): List<Match> {
        if (body.isBlank()) return emptyList()
        val root = JSONObject(body)
        val arr = root.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                // id бывает и числом, и строкой.
                val id = when (val raw = o.opt("id")) {
                    null, JSONObject.NULL -> continue
                    is Number -> raw.toString()
                    else -> raw.toString()
                }.trim()
                val text = o.optString("text", "").trim()
                if (id.isNotEmpty() && text.isNotEmpty()) add(Match(id, text))
            }
        }
    }
}
