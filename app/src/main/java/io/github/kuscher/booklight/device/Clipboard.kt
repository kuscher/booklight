package io.github.kuscher.booklight.device

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.PersistableBundle
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLanguage
import android.view.textclassifier.TextLinks
import io.github.kuscher.booklight.core.Look
import io.github.kuscher.booklight.core.Thing

/** One thing the system found in a text, as it stands there. */
data class Found(val thing: Thing, val text: String)

/**
 * The one place that touches the system's clipboard. The clipboard answers only an app whose
 * window has the focus, which the panel has; asked from anywhere else, everything here says
 * "nothing".
 *
 * [look] reads the copy's description only (its kind, its age, what the system found in it), and
 * the system shows no "pasted" message for that. [text] reads the content, and for another app's
 * copy the system then says so, once.
 */
object Clipboard {
    /** The label of every copy Booklight makes: how a copy of its own is told from someone else's, also after a restart. */
    const val LABEL = "Booklight"

    /** When Booklight last copied something, by the wall clock (a picture's copy cannot carry the label). */
    @Volatile private var ownAt = 0L

    private fun manager(context: Context) = context.getSystemService(ClipboardManager::class.java)

    /** What the system says about the copy without its content being read; null when nothing is copied or it cannot be asked now. */
    fun look(context: Context): Look? = runCatching {
        val d = manager(context).primaryClipDescription ?: return null
        val text = d.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || d.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        val status = d.classificationStatus
        fun score(vararg entities: String) = entities.maxOf { e -> runCatching { d.getConfidenceScore(e) }.getOrDefault(0f) }
        val found = if (status != ClipDescription.CLASSIFICATION_COMPLETE) null else mapOf(
            Thing.LINK to score(TextClassifier.TYPE_URL),
            Thing.DATE to score(TextClassifier.TYPE_DATE, TextClassifier.TYPE_DATE_TIME),
            Thing.PHONE to score(TextClassifier.TYPE_PHONE),
            Thing.MAIL to score(TextClassifier.TYPE_EMAIL),
        )
        Look(
            text = text,
            ageMs = System.currentTimeMillis() - d.timestamp,
            private = private(d),
            own = d.label?.toString() == LABEL || (ownAt != 0L && kotlin.math.abs(d.timestamp - ownAt) < OWN_MS),
            found = found,
            looking = status == ClipDescription.CLASSIFICATION_NOT_COMPLETE,
        )
    }.getOrNull()

    private fun private(d: ClipDescription) = d.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true

    /** Whoever copied it said it is private (a password manager does; Booklight's own passwords do). Such a copy is never read. */
    fun private(context: Context): Boolean = runCatching { manager(context).primaryClipDescription?.let(::private) == true }.getOrDefault(false)

    /** What the clipboard holds as plain text, or null: nothing, only blanks, or marked private. */
    fun text(context: Context): String? = runCatching {
        if (private(context)) return null
        // The item's own text only, and not without end: a copied file is not opened and read on the way to a list.
        manager(context).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.take(MAX_TEXT)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun set(context: Context, text: String, sensitive: Boolean = false) {
        val clip = ClipData.newPlainText(LABEL, text)
        // A password: the system's clipboard preview and history are asked not to show it.
        if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        ownAt = System.currentTimeMillis()
        manager(context).setPrimaryClip(clip)
    }

    /** A picture Booklight made (a QR code), by its address. */
    fun set(context: Context, resolver: ContentResolver, uri: Uri) {
        ownAt = System.currentTimeMillis()
        manager(context).setPrimaryClip(ClipData.newUri(resolver, LABEL, uri))
    }

    /**
     * What the system's classifier finds in [text], in the order it stands there: links, dates,
     * phone numbers, mail addresses. It can take a while for a long text, so it is asked off the
     * main thread and whoever asks decides how long to wait. Never throws.
     */
    fun find(context: Context, text: String): List<Found> = runCatching {
        val tc = context.getSystemService(TextClassificationManager::class.java).textClassifier
        tc.generateLinks(TextLinks.Request.Builder(text).build()).links.sortedBy { it.start }.mapNotNull { l ->
            val best = (0 until l.entityCount).map { l.getEntity(it) }.maxByOrNull { l.getConfidenceScore(it) } ?: return@mapNotNull null
            if (l.getConfidenceScore(best) < io.github.kuscher.booklight.core.Clip.SURE) return@mapNotNull null
            val thing = when (best) {
                TextClassifier.TYPE_URL -> Thing.LINK
                TextClassifier.TYPE_DATE, TextClassifier.TYPE_DATE_TIME -> Thing.DATE
                TextClassifier.TYPE_PHONE -> Thing.PHONE
                TextClassifier.TYPE_EMAIL -> Thing.MAIL
                else -> return@mapNotNull null
            }
            Found(thing, text.substring(l.start, l.end))
        }
    }.getOrDefault(emptyList())

    /** The language [text] is written in, as a tag ("de"), or null when the system cannot tell with some certainty. */
    fun language(context: Context, text: String): String? = runCatching {
        val tc = context.getSystemService(TextClassificationManager::class.java).textClassifier
        val r = tc.detectLanguage(TextLanguage.Request.Builder(text.take(LANGUAGE_CHARS)).build())
        (0 until r.localeHypothesisCount).map { r.getLocale(it) }.maxByOrNull { r.getConfidenceScore(it) }?.takeIf { r.getConfidenceScore(it) >= LANGUAGE_SURE }?.toLanguageTag()
    }.getOrNull()

    /** A copy within this long of one Booklight made is Booklight's own. */
    private const val OWN_MS = 1500L
    /** As much of a copy as is ever read (the same as of text another app hands over). */
    private const val MAX_TEXT = 20_000
    private const val LANGUAGE_CHARS = 2000
    private const val LANGUAGE_SURE = 0.5f
}
