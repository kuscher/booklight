package io.github.kuscher.booklight.scopes

import android.content.ClipboardManager
import android.content.Context
import io.github.kuscher.booklight.R
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
import io.github.kuscher.booklight.data.PromptEntry
import io.github.kuscher.booklight.overlay.qr
import io.github.kuscher.booklight.providers.Answers

/** What the clipboard holds as plain text, or null. Readable only while one of Booklight's windows has focus, which the panel has. */
fun clipboardText(context: Context): String? = runCatching {
    context.getSystemService(ClipboardManager::class.java).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}.getOrNull()

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

/**
 * A piece of text and what can be done with it: the clipboard (`clip`), or text another app
 * handed over (its selection menu, its share sheet), which then is the chip. Typing narrows the
 * rows by name. The text goes nowhere unless a row is run.
 *
 * The user's prompts are rows too: for text another app sent, after the two ways out; for the
 * clipboard, when their name is typed.
 */
class TextScope(private val context: Context, private val fixed: String?, private val search: (String) -> Result, private val prompts: () -> List<PromptEntry> = { emptyList() }) : Scope {
    override val key = if (fixed == null) "clip" else "text"
    override val keywords: List<String> = if (fixed == null) context.getString(R.string.clip_keys).split(',') else emptyList()
    override val name: String = fixed?.let { it.trim().replace('\n', ' ').let { t -> if (t.length > 28) t.take(27) + "…" else t } } ?: context.getString(R.string.clip_name)
    override val symbol = if (fixed == null) "clip" else "text"
    override val hint: String = context.getString(R.string.clip_hint)
    override val about: String = context.getString(R.string.clip_about)
    override val listed = fixed == null

    override suspend fun rows(arg: String): List<Result> {
        val text = fixed ?: clipboardText(context) ?: return listOf(Result(
            id = "text:none", provider = key, kind = Kind.OTHER, title = context.getString(R.string.clip_empty), icon = Icon.Symbol("clip"), score = 1.0, learnable = false, actions = emptyList(),
        ))
        val one = text.trim().replace('\n', ' ')
        fun row(id: String, title: String, symbol: String, sub: String?, vararg actions: Action) = Result(
            id = "text:$id", provider = key, kind = Kind.OTHER, title = title, subtitle = sub, icon = Icon.Symbol(symbol), score = 1.0, learnable = false, actions = actions.toList(),
        )
        val names = mapOf("upper" to R.string.clip_upper, "lower" to R.string.clip_lower, "title" to R.string.clip_title, "line" to R.string.clip_line, "count" to R.string.clip_count, "url" to R.string.clip_url)
        val copy = context.getString(R.string.action_copy)
        val rows = ArrayList<Result>()
        // Text another app sent is something to look up: the ways out come first. The clipboard may hold anything
        // (a password): there the rows that stay on the device come first and the ways out last.
        val out = listOf(search(one), gemini(context, text, 1.0))
        if (fixed != null) rows += out
        val filter = arg.trim()
        val asked = prompts().filter { fixed != null || (filter.isNotEmpty() && (Matcher.score(filter, it.name) > 0 || it.keyword.equals(filter, ignoreCase = true))) }.map { p ->
            row("prompt:${p.id}", p.name, "spark", one, Action("prompt", context.getString(R.string.scope_type), Effect.EnterScope(PromptScope.key(p), one), keepOpen = true, symbol = "spark"))
        }
        // Three of them while nothing is typed, so that Note, Mail and the code stay in sight; a name finds the rest.
        if (fixed != null) rows += if (filter.isEmpty()) asked.take(3) else asked
        rows += row("note", context.getString(R.string.note_name), "note", one, Action("note", context.getString(R.string.scope_type), Effect.EnterScope("note", one), keepOpen = true, symbol = "edit"))
        rows += row("mail", context.getString(R.string.mail_name), "mail", one, Action("compose", context.getString(R.string.action_compose), Effect.Compose(emptyList(), "", text), symbol = "edit"))
        if (one.length <= 1200) rows += row("qr", context.getString(R.string.qr_name), "qr", one, Action("qr", context.getString(R.string.scope_type), Effect.EnterScope("qr", one), keepOpen = true, symbol = "qr"))
        for (t in Clip.transforms(text)) {
            val label = context.getString(names[t.id] ?: continue)
            if (t.id == "count") {
                val (words, chars) = t.value.split(' ').let { it[0] to it.getOrElse(1) { "0" } }
                rows += row("count", context.getString(R.string.clip_counted, words, chars), "text", label, Action("copy", copy, Effect.CopyText(t.value.replace(' ', '\t'))))
            } else rows += row(t.id, t.value.trim().replace('\n', ' '), "text", label, Action("copy", copy, Effect.CopyText(t.value)))
        }
        if (fixed == null) rows += out
        if (filter.isEmpty()) return rows
        // In the clipboard's list a prompt found by its name comes first: it was asked for.
        return (if (fixed == null) asked else emptyList()) + rows.filter { r -> Matcher.score(filter, r.subtitle ?: "") > 0 || Matcher.score(filter, r.title) > 0 }
    }
}
