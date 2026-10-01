package io.github.kuscher.booklight.providers

import android.content.Context
import android.util.LruCache
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Sites
import io.github.kuscher.booklight.core.Suggest
import io.github.kuscher.booklight.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Search suggestions from the chosen search engine: the one thing in Booklight that uses the
 * network, and only once the user has turned it on. The typed text goes to the engine's
 * suggestion address over HTTPS; nothing else is sent (no cookies, no identifiers), nothing is kept
 * beyond a small in-memory cache. Slow or failing requests are simply no suggestions.
 */
class SuggestProvider(private val context: Context, private val prefs: Prefs) {
    private val cache = LruCache<String, List<String>>(64)

    suspend fun fetch(text: String): List<Result> {
        val s = prefs.now
        val t = text.trim()
        if (!s.suggestions || !Suggest.worthAsking(t) || Sites.parse(t, s.sites()) != null) return emptyList()
        val engine = s.engine()
        val key = engine.id + "\n" + t.lowercase()
        val words = cache.get(key) ?: withContext(Dispatchers.IO) { runCatching { request(engine.suggest(t)) }.getOrDefault(emptyList()) }
            .also { if (it.isNotEmpty()) cache.put(key, it) }
        return words.filter { !it.equals(t, ignoreCase = true) }.map { w ->
            Result(
                id = "suggest:$w", provider = "suggest", kind = Kind.SUGGESTION, title = w,
                icon = Icon.Symbol("search"), score = 0.2, learnable = false,
                actions = listOf(Action("search", context.getString(R.string.action_search_engine, engine.name), Effect.OpenUrl(engine.search(w)))),
            )
        }
    }

    private fun request(url: String): List<String> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 1500
            c.readTimeout = 1500
            c.instanceFollowRedirects = false
            c.setRequestProperty("Accept", "application/json")
            if (c.responseCode != 200) return emptyList()
            val charset = Regex("charset=([\\w-]+)").find(c.contentType.orEmpty())?.groupValues?.get(1) ?: "UTF-8"
            val body = c.inputStream.use { it.readNBytes(64 * 1024) }.toString(runCatching { charset(charset) }.getOrDefault(Charsets.UTF_8))
            return Suggest.parse(body, limit = 6)
        } finally {
            c.disconnect()
        }
    }
}
