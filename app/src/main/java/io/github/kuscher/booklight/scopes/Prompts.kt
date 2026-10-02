package io.github.kuscher.booklight.scopes

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.ai.OnDevice
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Prompts
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.data.PromptEntry
import io.github.kuscher.booklight.device.Clipboard

/**
 * A scope with a row the device's own model answers, in the row. The panel asks; the scope says
 * what the row looks like while it is asked, as the words arrive, and when none come.
 */
interface Answering {
    /** The id of the row that is asked after a pause in typing (a prompt typed by its keyword); null when rows are asked on Enter only. */
    val row: String?
    /** False where the model is a helper beside what the scope does (a song's name, for `play`): it is not loaded before somebody asks it. */
    val eager: Boolean get() = true
    /** [r] as it is the moment the model is asked for it ([e] is what Enter ran): at an answer's height, with its caption. */
    fun asking(r: Result, e: Effect.Ask): Result
    /** [r] with the model's [text] in it: so far ([busy]), or all of it. */
    fun answered(r: Result, text: String, busy: Boolean): Result
    /** [r] when the model gave no answer (busy, over its allowance, gone): the text again, and the way to the Gemini app. */
    fun unanswered(r: Result): Result
}

/** What the clipboard holds as text, and whether whoever copied it said it is private (a password manager does; Booklight's own passwords do). */
class Clipped(val text: String?, val private: Boolean)

fun clipboard(context: Context): Clipped {
    val private = Clipboard.private(context)
    // Something private is not read at all.
    return Clipped(if (private) null else Clipboard.text(context), private)
}

/**
 * A prompt the user keeps: `fix teh text`. Its row shows the text it is about (what is typed; with
 * nothing typed, what the clipboard holds), and then the answer of the device's own model, in the
 * same row. Where the device has no such model, the row hands the prompt and the text to the Gemini
 * app instead.
 *
 * The model can be wrong. Its words are only ever shown, copied, pinned, or put back where the text
 * came from: a row of this scope never runs anything.
 */
class PromptScope(
    private val context: Context, private val p: PromptEntry,
    /** The prompt's keyword, or empty when somebody else already has it: then it is entered from its row. */
    keyword: String,
    private val ai: OnDevice,
    /** The panel was opened from a text field of another app, which takes text back. */
    private val replaces: () -> Boolean,
) : Scope, Answering {
    override val key = key(p)
    override val keywords: List<String> = listOfNotNull(keyword.takeIf { it.isNotEmpty() })
    override val name: String = p.name
    override val symbol = "spark"
    override val hint: String = context.getString(R.string.prompt_hint)
    override val about: String? = p.text.replace(Prompts.MARK, "").lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }

    /** The id of the row that is answered. */
    override val row = "answer:${p.id}"

    override fun asking(r: Result, e: Effect.Ask): Result = r

    override suspend fun rows(arg: String): List<Result> {
        val typed = arg.trim()
        val clip = if (typed.isEmpty()) clipboard(context) else null
        val text = typed.ifEmpty { clip?.text?.trim().orEmpty() }
        // Nothing to ask about: a row with nothing to run.
        if (text.isEmpty()) return listOf(Result(
            id = row, provider = key, kind = Kind.OTHER, title = p.name, subtitle = context.getString(if (clip?.private == true) R.string.prompt_private else R.string.prompt_empty),
            icon = Icon.Symbol(symbol), score = 1.0, learnable = false, actions = emptyList(),
        ))
        val full = Prompts.fill(p.text, text)
        val state = ai.state.value
        // The same limit as under a copy's chip: a longer rewrite would come back cut and look whole. (A summary may read more.)
        val fits = full.length <= if (p.seed == io.github.kuscher.booklight.core.Seeds.SUMMARY) TextScope.HERE_SUMMARY else TextScope.HERE
        val here = state == OnDevice.State.READY && fits
        val caption = when {
            state == OnDevice.State.READY && !fits -> context.getString(R.string.prompt_long)
            clip != null -> context.getString(R.string.prompt_clip)
            else -> p.name
        }
        // The clipboard may hold pages: the row shows where it starts.
        val shown = if (clip != null) text.lineSequence().first().let { if (it.length < text.length || it.length > SHOWN) "${it.take(SHOWN)} …" else it } else text.take(MAX)
        val out = ArrayList<Result>()
        out += Result(
            id = row, provider = key, kind = Kind.OTHER, title = p.name, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
            body = Body.Stream(shown, busy = false, caption = caption, ask = full.takeIf { here }),
            actions = listOfNotNull(
                Action("ask", context.getString(R.string.action_ask), ASK, keepOpen = true, symbol = "spark").takeIf { here },
                gemini(full, here),
            ),
        )
        if (state == OnDevice.State.DOWNLOADABLE || state == OnDevice.State.DOWNLOADING) out += model(state)
        return out
    }

    private fun gemini(full: String, second: Boolean) =
        Action("gemini", context.getString(if (second) R.string.action_in_gemini else R.string.gemini_title), Effect.AskGemini(full.take(HAND_OVER)), symbol = "send")

    override fun answered(r: Result, text: String, busy: Boolean): Result {
        val was = r.body as? Body.Stream ?: return r
        val full = was.ask ?: return r
        // The model likes Markdown, two spaces after a full stop and an empty line between paragraphs; the row has four lines of plain text.
        val said = io.github.kuscher.booklight.core.Plain.of(text).replace(SPACES, " ")
        return r.copy(
            body = Body.Stream(said.replace(BREAKS, "\n"), busy, context.getString(R.string.prompt_device), answer = true, ask = full),
            actions = listOfNotNull(
                Action("copy", context.getString(R.string.action_copy), Effect.CopyText(said)),
                Action("replace", context.getString(R.string.action_replace), Effect.Replace(said), symbol = "again", done = context.getString(R.string.done_replaced)).takeIf { replaces() },
                Action("pin", context.getString(R.string.action_pin), Effect.Pin("text", said), symbol = "pin"),
                gemini(full, true),
            ),
        )
    }

    override fun unanswered(r: Result): Result {
        val was = r.body as? Body.Stream ?: return r
        val full = was.ask ?: return r
        return r.copy(body = was.copy(caption = context.getString(R.string.prompt_failed), ask = null, busy = false), actions = listOf(gemini(full, false)))
    }

    /** The row that fetches the model, where the system has one to give and has not fetched it yet. */
    private fun model(state: OnDevice.State): Result {
        val getting = state == OnDevice.State.DOWNLOADING
        val sub = ai.progress.value?.let { (done, total) -> context.getString(R.string.model_progress, done, total) }
            ?: context.getString(if (getting) R.string.model_waiting else R.string.model_get_sub)
        return Result(
            id = "answer:model", provider = key, kind = Kind.OTHER, title = context.getString(if (getting) R.string.model_getting else R.string.model_get), subtitle = sub,
            icon = Icon.Symbol("save"), score = 1.0, learnable = false,
            actions = if (getting) emptyList() else listOf(Action("get", context.getString(R.string.action_get), Effect.Internal("model"), keepOpen = true, symbol = "save")),
        )
    }

    companion object {
        fun key(p: PromptEntry) = "prompt:${p.id}"
        /** "Ask the model on this device now": the panel does this itself. */
        val ASK: Effect = Effect.Internal("answer")
        /** The model reads about 4,000 tokens; a prompt and its text longer than this are for the Gemini app. */
        const val MAX = 8000
        /** How much of the clipboard's first line a row shows, and how much text is handed to another app at most (an intent holds no more). */
        private const val SHOWN = 300
        private const val HAND_OVER = 100_000
        private val SPACES = Regex("[ \\t]{2,}")
        private val BREAKS = Regex("\\n{2,}")
    }
}
