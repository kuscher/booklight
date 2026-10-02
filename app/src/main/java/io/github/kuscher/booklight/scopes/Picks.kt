package io.github.kuscher.booklight.scopes

import android.content.Context
import io.github.kuscher.booklight.R
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import io.github.kuscher.booklight.device.Found
import io.github.kuscher.booklight.core.Seeds
import io.github.kuscher.booklight.core.Thing
import io.github.kuscher.booklight.core.Prompts
import io.github.kuscher.booklight.core.Plain
import io.github.kuscher.booklight.core.Links
import io.github.kuscher.booklight.core.Languages
import io.github.kuscher.booklight.core.Jot
import io.github.kuscher.booklight.ai.OnDevice
import android.content.Intent
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Cell
import io.github.kuscher.booklight.core.Clip
import io.github.kuscher.booklight.core.Colors
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.EmojiIndex
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.ImageUse
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Letters
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Scope
import io.github.kuscher.booklight.core.Slot
import io.github.kuscher.booklight.core.SlotState
import io.github.kuscher.booklight.data.Prefs
import io.github.kuscher.booklight.device.Clipboard
import io.github.kuscher.booklight.data.PromptEntry
import io.github.kuscher.booklight.overlay.qr
import io.github.kuscher.booklight.providers.Answers

/** What the clipboard holds as plain text, or null: nothing, or a copy marked private, which is never read. Readable only while one of Booklight's windows has focus, which the panel has. */
fun clipboardText(context: Context): String? = Clipboard.text(context)

/**
 * `emoji party`, `sym arrow`: a grid to pick from. Nothing typed: the ones picked lately, then the
 * table's own order (smileys first). The table is read from the app's assets the first time.
 */
class EmojiScope(private val context: Context, private val prefs: Prefs, private val symbols: Boolean) : Scope {
    override val key = if (symbols) "sym" else "emoji"
    override val keywords: List<String> = context.getString(if (symbols) R.string.sym_keys else R.string.emoji_keys).split(',')
    override val name: String = context.getString(if (symbols) R.string.sym_name else R.string.emoji_name)
    override val symbol = if (symbols) "text" else "smile"
    override val hint: String = context.getString(if (symbols) R.string.sym_hint else R.string.emoji_hint)
    override val about: String = context.getString(if (symbols) R.string.sym_about else R.string.emoji_about)

    override suspend fun rows(arg: String): List<Result> {
        val index = index(context)
        val found = index.search(arg.trim(), symbols, GRID)
        val cells = if (arg.isBlank() && !symbols) {
            // The ones picked lately come first, then the rest without repeating them.
            val recent = prefs.now.emojiRecent.mapNotNull { index.find(it) }.filter { !it.symbol }
            (recent + found.filter { f -> recent.none { it.glyph == f.glyph } }).take(GRID)
        } else found
        if (cells.isEmpty()) return emptyList()
        return listOf(Result(
            id = "pick:$key", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
            body = Body.Grid(cells.map { Cell(it.glyph, it.name) }),
            actions = listOf(Action("copy", context.getString(R.string.action_copy), Effect.CopyText(""), done = context.getString(R.string.copied))),
        ))
    }

    companion object {
        /** Five lines of fourteen: what fits without scrolling. */
        const val GRID = 70
        @Volatile private var loaded: EmojiIndex? = null

        fun index(context: Context): EmojiIndex = loaded ?: synchronized(this) {
            loaded ?: context.assets.open("emoji.tsv").bufferedReader().useLines { lines ->
                EmojiIndex(lines, german = context.resources.configuration.locales[0].language == "de")
            }.also { loaded = it }
        }
    }
}

/**
 * `abc german`, `abc ss`, `abc umlaut`: the letters of other languages in the same grid, to pick and
 * copy. Nothing typed: the ones picked lately, then every small letter. Its row is also found by
 * what people call these ("umlaut", "accent"), without those words being keywords.
 */
class LettersScope(private val context: Context, private val prefs: Prefs) : Scope {
    override val key = "abc"
    override val keywords: List<String> = context.getString(R.string.abc_keys).split(',')
    override val name: String = context.getString(R.string.abc_name)
    override val symbol = "letters"
    override val hint: String = context.getString(R.string.abc_hint)
    override val about: String = context.getString(R.string.abc_about)
    override val words: List<String> = context.getString(R.string.abc_words).split(',')
    private val letters by lazy { Letters(german = context.resources.configuration.locales[0].language == "de") }

    override suspend fun rows(arg: String): List<Result> {
        val found = letters.find(arg, EmojiScope.GRID)
        val cells = if (arg.isBlank()) {
            val recent = prefs.now.lettersRecent.mapNotNull { letters.named(it) }
            (recent + found.filter { f -> recent.none { it.glyph == f.glyph } }).take(EmojiScope.GRID)
        } else found
        if (cells.isEmpty()) return emptyList()
        return listOf(Result(
            id = "pick:$key", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
            body = Body.Grid(cells.map { Cell(it.glyph, it.name) }),
            actions = listOf(Action("copy", context.getString(R.string.action_copy), Effect.CopyText(""), done = context.getString(R.string.copied))),
        ))
    }
}

/** `qr https://…`: the code, to scan from the screen, copy, save or share. */
class QrScope(private val context: Context) : Scope {
    override val key = "qr"
    override val keywords = listOf("qr")
    override val name: String = context.getString(R.string.qr_name)
    override val symbol = "qr"
    override val hint: String = context.getString(R.string.qr_hint)
    override val about: String = context.getString(R.string.qr_about)

    override suspend fun rows(arg: String): List<Result> {
        val text = arg.trim()
        if (text.isEmpty()) return emptyList()
        // A code holds bytes, not characters: whether it fits is asked of the encoder, not guessed from the length.
        if (text.length > 2000 || qr(text) == null) return listOf(Result(
            id = "pick:qr:long", provider = key, kind = Kind.OTHER, title = context.getString(R.string.qr_too_long), icon = Icon.Symbol("qr"), score = 1.0, learnable = false, actions = emptyList(),
        ))
        return listOf(Result(
            id = "pick:qr", provider = key, kind = Kind.OTHER, title = text, subtitle = context.getString(R.string.qr_sub, text.codePointCount(0, text.length)),
            icon = Icon.Symbol("qr"), score = 1.0, learnable = false, body = Body.Code(text),
            actions = listOf(
                Action("copy", context.getString(R.string.action_copy_image), Effect.QrImage(text, ImageUse.COPY), done = context.getString(R.string.copied)),
                Action("save", context.getString(R.string.action_save), Effect.QrImage(text, ImageUse.SAVE), done = context.getString(R.string.done_saved)),
                Action("share", context.getString(R.string.action_share), Effect.QrImage(text, ImageUse.SHARE)),
                Action("pin", context.getString(R.string.action_pin), Effect.Pin("qr", text), symbol = "pin"),
            ),
        ))
    }
}

/** `color 3478f6`, `color rgb(52,120,246)`: the colour and its other ways of being written. (`#3478f6` works without the keyword.) */
class ColorScope(private val context: Context) : Scope {
    override val key = "color"
    override val keywords: List<String> = context.getString(R.string.color_keys).split(',')
    override val name: String = context.getString(R.string.color_name)
    override val symbol = "edit"
    override val hint: String = context.getString(R.string.color_hint)
    override val about: String = context.getString(R.string.color_about)

    override suspend fun rows(arg: String): List<Result> {
        val c = Colors.parse(arg) ?: Colors.parse("#" + arg.trim()) ?: return emptyList()
        return listOf(Answers.color(context, c))
    }
}

/** `snip sig` copies a saved piece of text; `snip add sig / Best, Alex` saves one. */
class SnipScope(private val context: Context, private val prefs: Prefs) : Scope {
    override val key = "snip"
    override val keywords: List<String> = context.getString(R.string.snip_keys).split(',')
    override val name: String = context.getString(R.string.snip_name)
    override val symbol = "text"
    override val hint: String = context.getString(R.string.snip_hint)
    override val about: String = context.getString(R.string.snip_about)

    override suspend fun rows(arg: String): List<Result> {
        val t = arg.trim()
        val add = context.getString(R.string.snip_add_keys).split(',').firstOrNull { t.equals(it, ignoreCase = true) || t.startsWith("$it ", ignoreCase = true) }
        if (add != null) {
            val rest = t.substring(add.length).trim()
            val slash = rest.indexOf(" / ")
            val name = (if (slash >= 0) rest.substring(0, slash) else rest).trim().substringBefore(' ')
            val text = if (slash >= 0) rest.substring(slash + 3).trim() else ""
            fun slot(label: Int, v: String) = Slot(context.getString(label), v, if (v.isEmpty()) SlotState.EMPTY else SlotState.TYPED)
            return listOf(Result(
                id = "snip:add", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol("plus"), score = 1.0, learnable = false,
                body = Body.Slots(context.getString(R.string.snip_caption), listOf(slot(R.string.slot_name, name), slot(R.string.slot_text, text)), null),
                actions = if (name.isEmpty() || text.isEmpty()) emptyList()
                    else listOf(Action("save", context.getString(R.string.action_save), Effect.SaveSnippet(name, text), symbol = "check", done = context.getString(R.string.done_snippet))),
            ))
        }
        val all = prefs.now.snippets
        if (all.isEmpty()) return listOf(Result(
            id = "snip:none", provider = key, kind = Kind.OTHER, title = context.getString(R.string.snip_none), subtitle = context.getString(R.string.snip_none_sub),
            icon = Icon.Symbol("text"), score = 1.0, learnable = false, actions = emptyList(),
        ))
        return all.map { it to if (t.isEmpty()) 1.0 else Matcher.score(t, it.key) }.filter { it.second > 0 }.sortedByDescending { it.second }.map { (s, _) ->
            Result(
                id = "snip:${s.key}", provider = key, kind = Kind.OTHER, title = s.key, subtitle = s.text.lineSequence().first(),
                icon = Icon.Glyph(s.key.take(1).uppercase()), score = 1.0, learnable = false,
                actions = listOf(
                    Action("copy", context.getString(R.string.action_copy), Effect.CopyText(s.text)),
                    Action("edit", context.getString(R.string.action_edit), Effect.Edit("snippet", s.key)),
                    Action("delete", context.getString(R.string.action_delete), Effect.Delete("snippet", s.key), keepOpen = true, symbol = "trash", danger = true, confirm = true, done = context.getString(R.string.done_deleted)),
                ),
            )
        }
    }
}

/** Where the text a [TextScope] is about came from. */
enum class TextFrom {
    /** The keyword `clip`: the clipboard, read when the list is made. */
    CLIP,
    /** The line for a fresh copy, opened with Tab: the copy is the chip. */
    COPY,
    /** Another app handed it over (its selection menu, its share sheet): it is the chip. */
    HANDED,
}

/**
 * A piece of text and what can be done with it: what you copied (`clip`, or Tab on the line for a
 * fresh copy), or text another app handed over. The text is the chip; under it stand what was
 * found in it (a link, a date, a phone number, a mail address) and what the device's model can do
 * with it (your first three prompts; the translation first when the text is not in your language).
 * Typing narrows the rows by name and finds the others (Note, Mail, QR code, the other writings, a
 * web search). A language's name is the translation into it. What matches nothing is an
 * instruction: the model is asked to do that with the text.
 *
 * A model's row is answered where it stands ([asking], [answered]): the chip stays the text. The
 * text goes nowhere unless a row is run.
 */
class TextScope(
    private val context: Context, private val fixed: String?, private val search: (String) -> Result,
    private val prompts: () -> List<PromptEntry> = { emptyList() },
    private val ai: OnDevice? = null,
    /** The text came from a field of another app that takes text back: an answer can replace it there. */
    private val replaces: () -> Boolean = { false },
    val from: TextFrom = if (fixed == null) TextFrom.CLIP else TextFrom.HANDED,
    /** The row of a flight number that stands in the text, if one does. It answers when the user goes to it, never unasked. */
    private val flight: (String) -> Result? = { null },
) : Scope, Answering {
    override val key = if (from == TextFrom.CLIP) "clip" else "text"
    override val keywords: List<String> = if (from == TextFrom.CLIP) context.getString(R.string.clip_keys).split(',') else emptyList()
    override val name: String = fixed?.let { it.trim().replace('\n', ' ').let { t -> if (t.length > 28) t.take(27).trimEnd() + "…" else t } } ?: context.getString(R.string.clip_name)
    override val symbol = if (from == TextFrom.HANDED) "text" else "clip"
    override val hint: String = context.getString(R.string.thing_hint)
    override val about: String = context.getString(R.string.clip_about)
    override val listed = from == TextFrom.CLIP
    /** A row of this list is asked on Enter only, never after a pause in typing. */
    override val row: String? = null

    /** What the system's classifier found in the text and the language it is in: looked up once for a text. */
    private var readFor: String? = null
    private var reading: Pair<String, kotlinx.coroutines.Deferred<Pair<List<Found>, String?>>>? = null
    private val lookups = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var found: List<Found> = emptyList()
    private var language: String? = null

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)
    private fun row(id: String, title: String, symbol: String, sub: String?, label: String? = null, vararg actions: Action) = Result(
        id = "text:$id", provider = key, kind = Kind.OTHER, title = title, subtitle = sub, icon = Icon.Symbol(symbol), score = 1.0, learnable = false, actions = actions.toList(), label = label,
    )

    override suspend fun rows(arg: String): List<Result> {
        if (fixed == null && Clipboard.private(context)) return listOf(row("private", text(R.string.clip_private), "clip", null))
        val text = fixed ?: clipboardText(context) ?: return listOf(row("none", text(R.string.clip_empty), "clip", null))
        val one = text.trim().replace('\n', ' ')
        read(text)
        // (A device in a language the table does not hold counts as English here: its translation rows go to and from English.)
        val app = context.resources.configuration.locales[0].language.takeIf { Languages.of(it) != null } ?: "en"
        val all = prompts()
        val foreign = Clip.foreign(language, app)
        val translation = translate(text, Clip.target(language, app))

        // What stands there with nothing typed. The translation first when the text is not in your language: you may not be able to read the rest.
        val rows = ArrayList<Result>()
        if (foreign) rows += translation
        rows += things(text)
        for (p in all.take(3)) if (p.seed == Seeds.TRANSLATION) { if (!foreign) rows += translation } else rows += asks(p, text)
        all.firstOrNull { it.seed == Seeds.SUMMARY }?.takeIf { text.length >= LONG && all.indexOf(it) >= 3 }?.let { rows += asks(it, text) }
        val filter = arg.trim()
        if (filter.isEmpty()) return rows

        // Typed: a row's name narrows the list, and finds the rows that are not shown unasked. A row is found by what it is
        // called (its name, its kind, the thing it holds), never by the copied text it carries: that would match almost anything.
        val names = mapOf("upper" to R.string.clip_upper, "lower" to R.string.clip_lower, "title" to R.string.clip_title, "line" to R.string.clip_line, "count" to R.string.clip_count, "url" to R.string.clip_url)
        val copy = text(R.string.action_copy)
        val named = ArrayList<Pair<String, Result>>()
        for (r in rows) named += listOfNotNull(r.title, r.label?.takeIf { r.id.substringAfter(':') in THINGS || r.provider == io.github.kuscher.booklight.providers.FlightsProvider.ID }, text(R.string.tr_name).takeIf { r.id == translation.id }).joinToString(" ") to r
        for (p in all.drop(3)) if (p.seed != Seeds.TRANSLATION && rows.none { it.id == "text:ask:${p.id}" }) named += p.name to asks(p, text)
        if (rows.none { it.id == translation.id }) named += "${translation.title} ${text(R.string.tr_name)}" to translation
        named += text(R.string.note_name) to row("note", text(R.string.note_name), "note", one, null, Action("note", text(R.string.scope_type), Effect.EnterScope("note", one), keepOpen = true, symbol = "edit"))
        named += text(R.string.mail_name) to row("mail", text(R.string.mail_name), "mail", one, null, Action("compose", text(R.string.action_compose), Effect.Compose(emptyList(), "", text), symbol = "edit"))
        if (one.length <= 1200) named += text(R.string.qr_name) to row("qr", text(R.string.qr_name), "qr", one, null, Action("qr", text(R.string.scope_type), Effect.EnterScope("qr", one), keepOpen = true, symbol = "qr"))
        for (t in Clip.transforms(text)) {
            val label = text(names[t.id] ?: continue)
            if (t.id == "count") {
                val (words, chars) = t.value.split(' ').let { it[0] to it.getOrElse(1) { "0" } }
                named += label to row("count", text(R.string.clip_counted, words, chars), "text", label, null, Action("copy", copy, Effect.CopyText(t.value.replace(' ', '\t'))))
            } else named += label to row(t.id, t.value.trim().replace('\n', ' '), "text", label, null, Action("copy", copy, Effect.CopyText(t.value)))
        }
        // (Named by the title it has for no text: in German the text stands in the middle of the sentence.)
        named += search("").title to search(one)
        gemini(context, text, 1.0).let { named += it.title to it }
        // Not by letters scattered through a name: a row that was not on screen has to be an obvious find. The best match first.
        val scored = named.map { (name, r) -> Matcher.score(filter, name) to r }.filter { it.first >= Matcher.INSIDE }.sortedByDescending { it.first }
        val hits = scored.map { it.second }
        // A language is the translation into it: "danish", "in danish", "auf dänisch".
        val leads = text(R.string.tr_lead).split(',')
        val word = filter.split(' ').filter { it.isNotEmpty() }.dropWhile { it.lowercase() in leads }.singleOrNull()
        val into = word?.let(Languages::find)?.let { translate(text, it.tag) }
        if (into != null) {
            // A row's name that begins the way a language's short tag reads ("fi" for Fix spelling, "no" for Note) is the nearer meaning.
            val named = filter.length < 4 && scored.firstOrNull()?.first?.let { it >= Matcher.PREFIX } == true
            return if (named) hits + listOf(into).filter { t -> hits.none { it.id == t.id } } else listOf(into) + hits.filter { it.id != into.id }
        }
        if (hits.isNotEmpty()) return hits
        // Nothing has that name: it is what to do with the text.
        return listOf(instruction(text, filter))
    }

    /** Asks the system what is in [text] and what language it is, once for a text and without holding the list up for long. */
    private suspend fun read(text: String) {
        if (readFor == text) return
        // One lookup for a text, in a scope of its own: a list that is cancelled or runs out of time neither stops it nor
        // starts it again, and the next list takes its answer. (The two calls block; a cancelled child would be waited for.)
        val lookup = reading?.takeIf { it.first == text }?.second
            ?: lookups.async { Clipboard.find(context, text.take(FIND_CHARS)) to Clipboard.language(context, text) }.also { reading = text to it }
        // What is not there in time is left out of this list.
        val got = kotlinx.coroutines.withTimeoutOrNull(FIND_MS) { lookup.await() }
        found = got?.first ?: emptyList(); language = got?.second
        if (got != null) { readFor = text; reading = null }
    }

    /** What was found in the text, one row for each kind, in the fixed order, three at most. Each row is named by the thing itself. */
    private fun things(text: String): List<Result> = (Thing.entries.mapNotNull { kind ->
        Clip.pick(kind, found.filter { it.thing == kind }.map { it.text })?.let { thing(Found(kind, it), text) }
        // A flight is found by Booklight's own reader, which knows the airlines (the system's also calls "PS5" one). Its row
        // names the airline; nothing is asked for it until the user goes to it.
    } + listOfNotNull(flight(text))).take(3)

    private fun thing(f: Found, text: String): Result? = when (f.thing) {
        Thing.LINK -> {
            // The scheme is read from the start of the link only. A web address opens; anything else the system calls a
            // link (an app's own scheme, a file) is shown as it stands, and can be copied: it is not started from here.
            val scheme = SCHEME.find(f.text)?.groupValues?.get(1)?.lowercase()
            val url = if (scheme == null) "https://${f.text}" else f.text
            if (scheme != null && scheme != "http" && scheme != "https")
                row("link", f.text, "globe", null, text(R.string.label_link), Action("copy", text(R.string.action_copy), Effect.CopyText(f.text)))
            else row("link", Links.shown(url), "globe", null, text(R.string.label_link),
                Action("open", text(R.string.action_open), Effect.OpenUrl(Links.clean(url) ?: url)),
                // Without its tracking tail where it has one; as it is where it has none.
                Links.clean(url)?.let { Action("clean", text(R.string.action_copy_clean), Effect.CopyText(it), symbol = "clean") }
                    ?: Action("copy", text(R.string.action_copy), Effect.CopyText(url)),
            )
        }
        Thing.DATE -> Jot.eventIn(text, f.text, java.time.LocalDateTime.now())?.let { line ->
            // The event row's own reading of it, so the day is seen before the calendar opens; Enter goes on in that row.
            val e = Jot.event(line, java.time.LocalDateTime.now())
            row("date", span(context, e), "event", e.title.ifEmpty { null }, text(R.string.label_event), Action("add", text(R.string.action_add_event), Effect.EnterScope("event", line), keepOpen = true, symbol = "plus"))
        }
        Thing.PHONE -> {
            val call = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + f.text.filter { it.isDigit() || it == '+' }))
            val answered = runCatching { call.resolveActivity(context.packageManager) != null }.getOrDefault(false)
            row("phone", f.text, "phone", null, text(R.string.label_phone), *listOfNotNull(
                Action("call", text(R.string.action_call), Effect.OpenUrl(call.dataString.orEmpty()), symbol = "phone").takeIf { answered },
                Action("copy", text(R.string.action_copy), Effect.CopyText(f.text)),
            ).toTypedArray())
        }
        Thing.MAIL -> row("mailto", f.text, "mail", null, text(R.string.label_mail),
            Action("compose", text(R.string.action_compose), Effect.Compose(listOf(f.text), "", ""), symbol = "edit"), Action("copy", text(R.string.action_copy), Effect.CopyText(f.text)))
    }

    /** A row the device's model answers where it stands: a prompt of the user's, by its name. Where the model cannot take it, the row says "Gemini" and hands over. */
    private fun asks(p: PromptEntry, text: String) = model("ask:${p.id}", p.name, Prompts.fill(p.text, text), if (p.seed == Seeds.SUMMARY) HERE_SUMMARY else HERE)

    private fun translate(text: String, tag: String): Result {
        val l = Languages.of(tag) ?: Languages.all[0]
        val name = text(R.string.tr_in, if (context.resources.configuration.locales[0].language == "de") l.de else l.en)
        return model("ask:tr", name, Prompts.fill(text(R.string.tr_prompt), text, mapOf(Prompts.LANGUAGE to l.en)))
    }

    /** What was typed, when no row has that name: the model is asked to do it with the text. One tall row, as an answer's. */
    private fun instruction(text: String, what: String): Result {
        val full = Prompts.fill(text(R.string.instruction_prompt), text, mapOf(Prompts.INSTRUCTION to what))
        val here = here(full)
        return Result(
            id = "text:do", provider = key, kind = Kind.OTHER, title = what, icon = Icon.Symbol("spark"), score = 1.0, learnable = false,
            body = Body.Stream(shown(text), busy = false, caption = text(if (from == TextFrom.HANDED) R.string.with_text else R.string.with_copy)),
            actions = listOfNotNull(
                Action("ask", text(R.string.action_ask), Effect.Ask(full, ""), keepOpen = true, symbol = "spark").takeIf { here },
                Action("gemini", text(if (here) R.string.action_in_gemini else R.string.gemini_title), Effect.AskGemini(full.take(HAND_OVER)), symbol = "send"),
            ),
        )
    }

    /** Whether the device's model takes this. Not asked the system yet: yes, on trust; the row hands over on Enter if it turns out not to. */
    private fun here(full: String, most: Int = HERE) = ai?.state?.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN } && full.length <= most
    private fun shown(text: String) = text.trim().lineSequence().first().let { if (it.length < text.trim().length || it.length > SHOWN) "${it.take(SHOWN)} …" else it }

    private fun model(id: String, name: String, full: String, most: Int = HERE): Result {
        val here = here(full, most)
        return Result(
            id = "text:$id", provider = key, kind = Kind.OTHER, title = name, icon = Icon.Symbol("spark"), score = 1.0, learnable = false,
            label = text(if (here) R.string.prompt_device else R.string.label_gemini),
            actions = listOfNotNull(
                Action("ask", text(R.string.action_ask), Effect.Ask(full, name), keepOpen = true, symbol = "spark").takeIf { here },
                Action("gemini", text(if (here) R.string.action_in_gemini else R.string.gemini_title), Effect.AskGemini(full.take(HAND_OVER)), symbol = "send"),
            ),
        )
    }

    // ---- an answer where it stands

    /** [r] as it is while the model is asked: the row at an answer's height, the prompt's name over the start of the text. */
    override fun asking(r: Result, e: Effect.Ask): Result {
        val own = (r.body as? Body.Stream)?.text ?: shown(fixed ?: clipboardText(context).orEmpty())
        val caption = e.name.ifEmpty { (r.body as? Body.Stream)?.caption ?: text(R.string.prompt_device) }
        return r.copy(
            label = null, body = Body.Stream(own, busy = false, caption = caption, ask = e.prompt),
            // The hand-over keeps its slot beside Ask, so nothing in the strip moves when the row is asked.
            actions = listOfNotNull(Action("ask", text(R.string.action_ask), e, keepOpen = true, symbol = "spark"), r.actions.firstOrNull { it.id == "gemini" }),
        )
    }

    override fun answered(r: Result, text: String, busy: Boolean): Result {
        val was = r.body as? Body.Stream ?: return r
        val full = was.ask ?: return r
        val said = Plain.of(text).replace(SPACES, " ")
        val name = r.actions.firstNotNullOfOrNull { (it.effect as? Effect.Ask)?.name }.orEmpty()
        return r.copy(
            body = Body.Stream(said.replace(BREAKS, "\n"), busy, if (r.id == "text:do") text(R.string.prompt_device) else text(R.string.answer_here, name), answer = true, ask = full),
            actions = listOfNotNull(
                Action("replace", text(R.string.action_replace), Effect.Replace(said), symbol = "again", done = text(R.string.done_replaced)).takeIf { replaces() },
                Action("copy", text(R.string.action_copy), Effect.CopyText(said)),
                Action("pin", text(R.string.action_pin), Effect.Pin("text", said), symbol = "pin"),
                Action("gemini", text(R.string.action_in_gemini), Effect.AskGemini(full.take(HAND_OVER)), symbol = "send"),
            ),
        )
    }

    override fun unanswered(r: Result): Result {
        val was = r.body as? Body.Stream ?: return r
        val full = was.ask ?: return r
        return r.copy(body = was.copy(caption = text(R.string.prompt_failed), ask = null, busy = false),
            actions = listOf(Action("gemini", text(R.string.gemini_title), Effect.AskGemini(full.take(HAND_OVER)), symbol = "send")))
    }

    companion object {
        /** The rows for things found in the text: these are also found by their kind ("phone"). */
        val THINGS = setOf("link", "date", "phone", "mailto")
        /** From here on a text is long: Summary is offered unasked. */
        const val LONG = 1000
        /**
         * A prompt and its text longer than this are for the Gemini app: a rewrite that is cut looks whole and is not.
         * A translation of 1,900 characters took 36 s on the HP and came back whole (device-findings.md); the answer's
         * own limits ([OnDevice]'s tokens, the model's time) are set a little above what this lets in.
         */
        const val HERE = 1800
        /** A summary is short whatever it is made of: it may read more. */
        const val HERE_SUMMARY = 8000
        /**
         * How long the list waits for the system to say what is in the text (some 20 to 80 ms for up to 4,000 characters
         * on the HP; this is for a bad moment, and under the 150 ms the engine gives a list), and how much of the text is looked at.
         */
        const val FIND_MS = 100L
        const val FIND_CHARS = 4000
        const val SHOWN = 300
        const val HAND_OVER = 100_000
        val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*)://")
        val SPACES = Regex("[ \\t]{2,}")
        val BREAKS = Regex("\\n{2,}")
    }
}

/**
 * `tr danish see you on Saturday`: the text in that language, written by the device's model in the
 * row. With no text typed, what you copied. Asked after a pause in typing, as any prompt typed by
 * its keyword is.
 */
class TranslateScope(private val context: Context, private val ai: OnDevice, /** The keywords of the user's own links: one of those keeps its keyword, and Translate is found by its name. */ private val users: () -> Set<String> = { emptySet() }, private val replaces: () -> Boolean) : Scope, Answering {
    override val key = "tr"
    private val all = context.getString(R.string.tr_keys).split(',')
    override val keywords: List<String> get() = users().let { u -> all.filter { it.lowercase() !in u } }
    override val name: String = context.getString(R.string.tr_name)
    override val symbol = "spark"
    override val hint: String = context.getString(R.string.tr_hint)
    override val about: String = context.getString(R.string.tr_about)
    override val row = "answer:tr"

    /** No "search the web for tr danish…" under it: it is not a search. */
    override val web = false

    override suspend fun rows(arg: String): List<Result> {
        fun plain(title: Int) = listOf(Result(id = row, provider = key, kind = Kind.OTHER, title = name, subtitle = context.getString(title), icon = Icon.Symbol(symbol), score = 1.0, learnable = false, actions = emptyList()))
        val known = Languages.leading(arg)
        if (known == null) {
            // No language Booklight knows comes first: it may be one it does not know ("thai …"), or a text with no language
            // at all ("guten morgen"). Booklight does not guess which: Gemini gets all of it, and is told how to read it.
            val all = arg.trim()
            if (all.split(' ').size < 2) return plain(R.string.tr_which)
            return listOf(Result(
                id = "$row:other", provider = key, kind = Kind.OTHER, title = name, subtitle = all, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
                label = context.getString(R.string.label_gemini),
                actions = listOf(Action("gemini", context.getString(R.string.gemini_title), Effect.AskGemini(Prompts.fill(context.getString(R.string.tr_prompt_other), all.take(100_000))), symbol = "send")),
            ))
        }
        val (l, typed) = known
        val clip = if (typed.isEmpty()) clipboard(context) else null
        val text = typed.ifEmpty { clip?.text?.trim().orEmpty() }
        if (text.isEmpty()) return plain(if (clip?.private == true) R.string.prompt_private else R.string.prompt_empty)
        val german = context.resources.configuration.locales[0].language == "de"
        val into = context.getString(R.string.tr_in, if (german) l.de else l.en)
        val full = Prompts.fill(context.getString(R.string.tr_prompt), text, mapOf(Prompts.LANGUAGE to l.en))
        // The same limit as the translation under a copy's chip: a longer text would come back cut.
        val here = ai.state.value.let { it == OnDevice.State.READY || it == OnDevice.State.UNKNOWN } && full.length <= TextScope.HERE
        val shown = text.lineSequence().first().let { if (it.length < text.length || it.length > 300) "${it.take(300)} …" else it }
        return listOf(Result(
            id = row, provider = key, kind = Kind.OTHER, title = into, icon = Icon.Symbol(symbol), score = 1.0, learnable = false,
            body = Body.Stream(shown, busy = false, caption = into, ask = full.takeIf { here }),
            actions = listOfNotNull(
                Action("ask", context.getString(R.string.action_ask), PromptScope.ASK, keepOpen = true, symbol = "spark").takeIf { here },
                Action("gemini", context.getString(if (here) R.string.action_in_gemini else R.string.gemini_title), Effect.AskGemini(full.take(100_000)), symbol = "send"),
            ),
        ))
    }

    override fun asking(r: Result, e: Effect.Ask): Result = r

    override fun answered(r: Result, text: String, busy: Boolean): Result {
        val was = r.body as? Body.Stream ?: return r
        val full = was.ask ?: return r
        val said = Plain.of(text)
        return r.copy(
            body = Body.Stream(said, busy, context.getString(R.string.answer_here, r.title), answer = true, ask = full),
            actions = listOfNotNull(
                Action("copy", context.getString(R.string.action_copy), Effect.CopyText(said)),
                Action("replace", context.getString(R.string.action_replace), Effect.Replace(said), symbol = "again", done = context.getString(R.string.done_replaced)).takeIf { replaces() },
                Action("pin", context.getString(R.string.action_pin), Effect.Pin("text", said), symbol = "pin"),
                Action("gemini", context.getString(R.string.action_in_gemini), Effect.AskGemini(full.take(100_000)), symbol = "send"),
            ),
        )
    }

    override fun unanswered(r: Result): Result {
        val was = r.body as? Body.Stream ?: return r
        val full = was.ask ?: return r
        return r.copy(body = was.copy(caption = context.getString(R.string.prompt_failed), ask = null, busy = false),
            actions = listOf(Action("gemini", context.getString(R.string.gemini_title), Effect.AskGemini(full.take(100_000)), symbol = "send")))
    }
}
