package io.github.kuscher.booklight.ai

import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import io.github.kuscher.booklight.BooklightApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * The device's own model (Gemini Nano, kept and run by the system's AICore service), asked through
 * Google's ML Kit library. Booklight ships no model and hosts none; what is asked and what comes
 * back stay on the device.
 *
 * The system serves only the app in front, so everything here is called while the panel is open.
 * A device without the service, or with the model not yet fetched, simply answers "not here":
 * every row that could be answered on the device has its hand-over to the Gemini app to fall back on.
 */
class OnDevice(private val scope: CoroutineScope) {
    enum class State {
        /** Not asked yet. */ UNKNOWN,
        /** This device has no on-device model for apps. */ NONE,
        /** The system can fetch the model if asked to. */ DOWNLOADABLE,
        /** The system is fetching it. */ DOWNLOADING,
        READY,
    }

    private val _state = MutableStateFlow(State.UNKNOWN)
    val state: StateFlow<State> = _state
    val ready: Boolean get() = _state.value == State.READY

    /** How much of the model has been fetched, in megabytes, and of how many; null when no download is known of. */
    val progress = MutableStateFlow<Pair<Long, Long>?>(null)

    private var model: GenerativeModel? = null
    private fun client(): GenerativeModel = model ?: Generation.getClient().also { model = it }

    /** Debug builds: a state to show instead of the device's (`./bl debug ai downloadable`), to see the rows a device without the model gets. */
    @Volatile var pretend: State? = null

    /** One at a time: a second asker waits for the first one's answer (the try of a "downloadable" model takes a moment). */
    private val checking = kotlinx.coroutines.sync.Mutex()

    /** Asks the system what it has. Cheap enough to do each time an answering scope is entered; never throws. */
    suspend fun check(): State = checking.withLock { checked() }

    private suspend fun checked(): State {
        val s = pretend ?: try {
            when (client().checkStatus()) {
                FeatureStatus.AVAILABLE -> State.READY
                // "Downloadable" is what the system also says of a model it has and answers with, to an app that never
                // asked for a download (the HP Googlebook: docs/research/device-findings.md). So it is tried.
                FeatureStatus.DOWNLOADABLE -> if (answers()) State.READY else State.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> State.DOWNLOADING
                else -> State.NONE
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e       // the panel closed while the system was being asked: that says nothing about the model
        } catch (e: Throwable) {
            Log.i(BooklightApp.TAG, "on-device model: not here (${e.javaClass.simpleName})", e)
            State.NONE
        }
        _state.value = s
        return s
    }

    /** The model has answered in this process although the system called it downloadable; and when it was last tried in vain. */
    @Volatile private var proven = false
    private var triedAt = 0L

    /**
     * Whether a model the system calls "downloadable" answers all the same: one question of a few
     * words, a few tokens back. Nothing is fetched by it. Once it has answered it is not asked again;
     * in vain, not again for a minute.
     */
    private suspend fun answers(): Boolean {
        if (proven) return true
        val now = android.os.SystemClock.uptimeMillis()
        if (triedAt != 0L && now - triedAt < RETRY_MS) return false
        proven = kotlinx.coroutines.withTimeoutOrNull(PROBE_MS) {
            runCatching {
                // Asked the way every answer is asked (as a stream): the one path that is known to work in a release build.
                val request = GenerateContentRequest.Builder(TextPart("Say OK.")).apply { maxOutputTokens = 4 }.build()
                client().generateContentStream(request).firstOrNull() != null
            }.getOrElse { e -> if (e is kotlinx.coroutines.CancellationException) throw e; Log.i(BooklightApp.TAG, "on-device model: tried, ${e.javaClass.simpleName}"); false }
        } == true
        // (A try that the panel's closing cut short left by its exception above: it is not "in vain", and the next opening tries again.)
        if (!proven) triedAt = android.os.SystemClock.uptimeMillis()
        Log.i(BooklightApp.TAG, "on-device model: called downloadable, ${if (proven) "and it answers" else "no answer"} (${android.os.SystemClock.uptimeMillis() - now} ms)")
        return proven
    }

    /** Loads the model ahead of the first question (about two seconds the first time, nothing after). */
    fun warm() {
        if (!ready) return
        scope.launch(Dispatchers.Default) { runCatching { client().warmup() } }
    }

    /**
     * The answer to [prompt], in the pieces it arrives in. Ends without a piece if the model cannot
     * answer (busy, over its quota, the panel no longer in front): the caller falls back.
     */
    fun ask(prompt: String): Flow<String> = flow {
        // Close to the most likely words: a correction or a translation should be the same each time it is asked for.
        val request = GenerateContentRequest.Builder(TextPart(prompt)).apply { temperature = 0.2f; topK = 10; maxOutputTokens = MAX_OUT }.build()
        client().generateContentStream(request).collect { r -> r.candidates.firstOrNull()?.text?.let { if (it.isNotEmpty()) emit(it) } }
    }.catch { e -> Log.i(BooklightApp.TAG, "on-device model: no answer (${e.javaClass.simpleName})") }.flowOn(Dispatchers.Default)

    /** Asks the system to fetch the model. The system does the fetching and goes on with it whether Booklight stays open or not. */
    fun download() {
        if (_state.value != State.DOWNLOADABLE || pretend != null) return
        _state.value = State.DOWNLOADING
        scope.launch(Dispatchers.Default) {
            var total = 0L
            runCatching {
                client().download().collect { d ->
                    when (d) {
                        is DownloadStatus.DownloadStarted -> { total = d.bytesToDownload; progress.value = 0L to total / MB }
                        is DownloadStatus.DownloadProgress -> progress.value = d.totalBytesDownloaded / MB to total / MB
                        is DownloadStatus.DownloadCompleted -> { progress.value = null; _state.value = State.READY }
                        is DownloadStatus.DownloadFailed -> { progress.value = null; _state.value = State.DOWNLOADABLE }
                    }
                }
            }
            progress.value = null
            if (_state.value == State.DOWNLOADING) runCatching { check() }
        }
    }

    /** The panel closed: let go of the connection to the system's service. */
    fun close() {
        runCatching { model?.close() }
        model = null
        triedAt = 0L      // a try that failed because the panel was on its way out says nothing about the next opening
    }

    private companion object {
        const val MB = 1_000_000L
        const val PROBE_MS = 4_000L
        const val RETRY_MS = 60_000L
        /** About 2,700 characters: more than any row lets in to be rewritten or translated (`TextScope.HERE`), so an answer is not cut by this. */
        const val MAX_OUT = 768
    }
}
