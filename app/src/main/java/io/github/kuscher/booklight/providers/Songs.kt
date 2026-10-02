package io.github.kuscher.booklight.providers

import android.content.Context
import android.os.SystemClock
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.PlayRequest
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Song
import io.github.kuscher.booklight.core.SongFailure
import io.github.kuscher.booklight.core.Spotify
import io.github.kuscher.booklight.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * Play in Spotify finds the song and plays it: Spotify's app plays a link (`spotify:track:…`),
 * and the link for a name is found in Spotify's catalogue.
 *
 * That needs a key of the user's own for Spotify's Web API (a client id and its secret, set in the
 * Booklight window › Labs; Booklight ships none). With it, the text typed under Spotify's chip while
 * Play is armed and the key's token go to api.spotify.com over HTTPS, the key itself to
 * accounts.spotify.com for the token, and nothing else does (no cookies, no identifiers, a plain
 * "Booklight" user agent, as in `FlightsProvider`). When: once, after a pause in typing, while
 * Spotify is the chip and Play is armed; never for each letter, and never under Search. The token is
 * the app's, not a person's: nothing of anybody's account is read. An answer is kept for five minutes
 * and the token until a minute before it ends, both in memory only. Without a key nothing is sent,
 * and Spotify's row has no Play.
 *
 * The row is `AppChips`'. The panel asks ([waits], [asks], [answer]); this class looks up and remembers.
 */
class Songs(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    /** What Spotify said for a request, or why it said nothing; [at] by the clock that never jumps. */
    class Kept(val song: Song?, val failure: SongFailure?, internal val key: Int, internal val at: Long = SystemClock.elapsedRealtime())

    /** A token, the key it was got with, and until when it is used. */
    private class Held(val value: String, val key: Int, val until: Long)

    /** A row that stands waiting: what it asks for, whether only Enter asks it ([unasked]), and how the row is made again once the answer is in. */
    private class Waiting(val what: PlayRequest, val unasked: Boolean, val row: suspend () -> Result?)

    private val kept = ConcurrentHashMap<String, Kept>()
    private val asking = HashMap<String, Deferred<Kept>>()
    private val waiting = ConcurrentHashMap<String, Waiting>()
    @Volatile private var token: Held? = null

    init {
        // Another key, or none: what the old one's token found is not this one's.
        scope.launch { prefs.spotifyKey.drop(1).collect { token = null; kept.clear() } }
    }

    /** A key is in. */
    val ready: Boolean get() = prefs.spotifyKey.value.isNotEmpty()

    /** What is known for [what], while it is the key's own and young enough (`Spotify.keep`); null when it is still to be asked. */
    fun known(what: PlayRequest): Kept? = kept[name(what)]?.takeIf {
        it.key == prefs.spotifyKey.value.hashCode() && SystemClock.elapsedRealtime() - it.at < Spotify.keep(it.failure).toMillis()
    }

    /**
     * The row [id] stands waiting for what Spotify has for [what]; [row] makes it again when that is
     * known. [unasked]: nothing is sent for it until the user's Enter (Spotify is the chip merely for
     * being first by name, or the text may be an app's name).
     */
    fun wait(id: String, what: PlayRequest, unasked: Boolean = false, row: suspend () -> Result?) { waiting[id] = Waiting(what, unasked, row) }

    /**
     * The row [id] waits no more, if what it waits for is [what] (null: whatever it waits for). A row
     * made late for an earlier text must not take away the wait of the text typed after it.
     */
    fun settle(id: String, what: PlayRequest? = null) { waiting.computeIfPresent(id) { _, w -> if (what == null || w.what == what) null else w } }

    /** True if [r] is a row that stands waiting for Spotify's answer. */
    fun waits(r: Result): Boolean = r.provider == PROVIDER && waiting.containsKey(r.id)

    /** True if [r] waits and may be looked up without an Enter: after a pause in typing, or when the user goes to it. */
    fun asks(r: Result): Boolean = r.provider == PROVIDER && waiting[r.id]?.unasked == false

    /**
     * Asks Spotify for the row [id], after a pause if [pause] (a pause that is cancelled sends
     * nothing), and gives the row with the answer in it, or with what went wrong. A request that is
     * on its way is waited for, not sent again, and is kept when it lands even if nobody waits any
     * more. One that takes too long is not waited for: the row then says there was no answer.
     */
    suspend fun answer(id: String, pause: Boolean): Result? {
        val w = waiting[id] ?: return null
        if (pause) delay(PAUSE_MS)
        find(w.what) ?: return null
        return w.row()
    }

    /** What Spotify has for [what]: what is kept, or asked now. Null without a key. */
    suspend fun find(what: PlayRequest): Kept? {
        known(what)?.let { return it }
        val key = prefs.spotifyKey.value.ifEmpty { return null }
        val name = name(what)
        val job = synchronized(asking) {
            asking.getOrPut(name) { scope.async(Dispatchers.IO) { try { lookup(what, key).also { put(name, it) } } finally { synchronized(asking) { asking.remove(name) } } } }
        }
        // Too long: the row says there was no answer, until the answer that is still on its way takes that sentence's place.
        return withTimeoutOrNull(WAIT_MS) { job.await() } ?: known(what) ?: Kept(null, SongFailure.NO_ANSWER, key.hashCode()).also { put(name, it) }
    }

    private fun put(name: String, k: Kept) {
        if (kept.size > 64) kept.entries.sortedBy { it.value.at }.take(32).forEach { kept.remove(it.key) }
        kept[name] = k
    }

    /** What a request is kept under: its kind and its words. */
    private fun name(what: PlayRequest) = "${what.mode} ${what.query.lowercase()}"

    /**
     * The token, then the search (`Spotify.lookup`: one request in the usual case, three at most).
     * A token that Spotify no longer takes before its hour is up is got anew, once.
     */
    private fun lookup(what: PlayRequest, key: String): Kept {
        // (Nothing of a failed request is logged: it was sent with the key or its token.)
        val mine = key.hashCode()
        return try {
            val market = context.resources.configuration.locales[0].country
            val had = token?.takeIf { it.key == mine && SystemClock.elapsedRealtime() < it.until }
            var held = had ?: fresh(key).let { it.value ?: return Kept(null, it.failure ?: SongFailure.NO_ANSWER, mine) }
            var a = Spotify.lookup(what, market) { fetch(it, "Bearer ${held.value}", null) }
            if (a.failure == SongFailure.REFUSED && had != null) {
                token = null
                held = fresh(key).let { it.value ?: return Kept(null, it.failure ?: SongFailure.NO_ANSWER, mine) }
                a = Spotify.lookup(what, market) { fetch(it, "Bearer ${held.value}", null) }
            }
            Kept(a.song, a.failure, mine)
        } catch (_: Exception) {
            // A reply that reads as JSON and still makes no sense: no answer, never a crash.
            Kept(null, SongFailure.NO_ANSWER, mine)
        }
    }

    /** A new token for [key], kept for as long as `Spotify.lasts` says. */
    private fun fresh(key: String): Spotify.Said<Held> {
        val halves = key.split('\n')
        if (halves.size != 2) return Spotify.Said(null, SongFailure.REFUSED)
        val reply = try { fetch(Spotify.TOKEN, Spotify.basic(halves[0], halves[1]), Spotify.TOKEN_FORM) } catch (_: Spotify.Unreachable) { return Spotify.Said(null, SongFailure.OFFLINE) } catch (_: Exception) { return Spotify.Said(null, SongFailure.NO_ANSWER) }
        val t = Spotify.token(reply)
        val got = t.value ?: return Spotify.Said(null, t.failure ?: SongFailure.NO_ANSWER)
        return Spotify.Said(Held(got.value, key.hashCode(), SystemClock.elapsedRealtime() + Spotify.lasts(got).toMillis()).also { token = it })
    }

    private fun fetch(url: String, authorization: String, form: String?): Spotify.Fetched =
        try { send(url, authorization, form) } catch (e: Exception) { if (e is UnknownHostException || e is ConnectException || e is NoRouteToHostException) throw Spotify.Unreachable() else throw e }

    /** A GET, or a POST of [form]. Spotify says what is wrong in the text of a reply that is not 200: that text is read too. */
    private fun send(url: String, authorization: String, form: String?): Spotify.Fetched {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 4000
            c.readTimeout = 6000
            c.instanceFollowRedirects = false
            c.setRequestProperty("Accept", "application/json")
            // Not Android's default, which names the device model and build.
            c.setRequestProperty("User-Agent", "Booklight")
            c.setRequestProperty("Authorization", authorization)
            if (form != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                c.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
            }
            val status = c.responseCode
            val body = (if (status in 200..299) c.inputStream else c.errorStream)?.use { it.readNBytes(512 * 1024) }?.toString(Charsets.UTF_8).orEmpty()
            return Spotify.Fetched(status, body)
        } finally {
            c.disconnect()
        }
    }

    companion object {
        /** The provider of the rows that wait here (an app's row with Play armed), and the key of `play`. */
        const val PROVIDER = "play"
        /** "This needs the answer, which is on its way": the panel runs the action once the answer is in. */
        val WAIT: Effect = Effect.Internal("song")
        /** The place in Booklight's window where the key is set. */
        val KEY: Effect = Effect.Internal("songs")
        /** How long typing rests before Spotify is asked. */
        private const val PAUSE_MS = 400L
        /** How long an answer is waited for: after that the row says there was none (the request goes on, and what it brings is kept). */
        private const val WAIT_MS = 8000L
    }
}
