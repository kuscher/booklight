package io.github.kuscher.booklight.core

/** What an app command of the user's own asks its app for. */
enum class What { LINK, SEND, SEARCH, PLAY, CUSTOM }

enum class ExtraKind { TEXT, NUMBER, YES_NO }

/** A named value that goes with a request. [value] may hold `{argument}`; a yes or no is `true` or `false`. */
data class Extra(val name: String, val kind: ExtraKind, val value: String)

/**
 * What one app is asked to do, as the editor of an app command holds it: the form's fields. Which of
 * them count depends on [what]; [Requests.tidy] empties the others.
 */
data class Request(
    /** The app's package. */
    val app: String = "",
    val what: What = What.LINK,
    /** [What.CUSTOM]: the action's name, as the app declares it. */
    val action: String = "",
    /** [What.LINK], [What.CUSTOM]: the link the app is handed. */
    val address: String = "",
    /** [What.SEND]: the text. [What.SEARCH], [What.PLAY]: what to look for. */
    val text: String = "",
    /** [What.CUSTOM]: one activity of the app by its class name, when the request is for that one alone. */
    val activity: String = "",
    /** [What.CUSTOM]: what kind of thing is handed over (`text/plain`). */
    val type: String = "",
    val extras: List<Extra> = emptyList(),
)

/** What is wrong with a request, or with a line that was to be read into one. [detail]: the flag, the name or the value it is about. */
data class Problem(val kind: Kind, val detail: String = "") {
    enum class Kind {
        // The form: what keeps it from being saved or run.
        APP, ADDRESS, REFUSED, NO_ADDRESS, TEXT, ACTION, EXTRA_NAME, EXTRA_TWICE, NUMBER, YES_NO, TOO_MANY,
        // A line that was pasted: what was not read.
        NOT_A_LINE, QUOTE, FLAG, VALUE, WORD, COMPONENT, TWO_APPS, FIELD, EXTRA_TYPE, UNFINISHED,
    }
}

sealed interface Read {
    data class Ok(val request: Request) : Read
    data class Bad(val problem: Problem) : Read
}

/**
 * App commands of the user's own, as they are kept and run: an `intent:` address (the form Android itself
 * reads, `Intent.parseUri`) with `{argument}` still in it where the typed text goes. [write] makes it from
 * the form, [read] the form from it, [fill] puts the text in. [am] reads an `am start …` line into the
 * form. Nothing here starts anything: the app's `Executor` does, and only an activity of the named app
 * that is open to other apps.
 *
 * Only what the form holds is read. What a line says beyond that (a category, flags, another kind of
 * extra) is named as a [Problem], never guessed and never dropped in silence.
 */
object Requests {
    const val ARGUMENT = "{argument}"
    const val MAX_EXTRAS = 8

    // Android's own names for what the form's kinds ask.
    const val VIEW = "android.intent.action.VIEW"
    const val MAIN = "android.intent.action.MAIN"
    const val SEND = "android.intent.action.SEND"
    const val SEARCH = "android.intent.action.SEARCH"
    const val PLAY = "android.media.action.MEDIA_PLAY_FROM_SEARCH"
    const val TEXT = "android.intent.extra.TEXT"
    const val QUERY = "query"
    /** What kind of thing is to be played, and the value that says "whatever the words mean". */
    const val FOCUS = "android.intent.extra.focus"
    const val ANYTHING = "vnd.android.cursor.item/*"
    const val PLAIN = "text/plain"

    /** The request with only what its kind uses: what the editor still holds of another kind is not part of it. */
    fun tidy(r: Request): Request = when (r.what) {
        What.LINK -> r.copy(action = "", address = r.address.trim(), text = "", activity = "", type = "")
        What.SEND, What.SEARCH, What.PLAY -> r.copy(action = "", address = "", activity = "", type = "")
        What.CUSTOM -> r.copy(action = r.action.trim(), address = r.address.trim(), text = "", activity = r.activity.trim(), type = r.type.trim())
    }

    /** Whether the request has a place for typed text: then its command is a keyword that takes text; else a row that runs on Enter. */
    fun takesArgument(r: Request): Boolean = tidy(r).let { t -> ARGUMENT in t.address || ARGUMENT in t.text || t.extras.any { ARGUMENT in it.value } }
    fun takesArgument(template: String): Boolean = (read(template) as? Read.Ok)?.request?.let(::takesArgument) == true

    /** What keeps [r] from being saved or run, in the order of the form's fields; null if nothing does. */
    fun why(r: Request): Problem? {
        val t = tidy(r)
        if (t.app.isBlank()) return Problem(Problem.Kind.APP)
        when (t.what) {
            What.LINK -> address(t.address)?.let { return it }
            What.SEND, What.SEARCH, What.PLAY -> if (t.text.isBlank()) return Problem(Problem.Kind.TEXT)
            What.CUSTOM -> {
                if (t.action.isEmpty() && t.activity.isEmpty()) return Problem(Problem.Kind.ACTION)
                if (t.address.isNotEmpty()) address(t.address)?.let { return it }
            }
        }
        if (t.extras.size > MAX_EXTRAS) return Problem(Problem.Kind.TOO_MANY)
        val names = HashSet(own(t).map { it.name })
        for (e in t.extras) {
            if (e.name.isBlank()) return Problem(Problem.Kind.EXTRA_NAME)
            if (!names.add(e.name)) return Problem(Problem.Kind.EXTRA_TWICE, e.name)
            when (e.kind) {
                ExtraKind.TEXT -> {}
                ExtraKind.NUMBER -> if (e.value.replace(ARGUMENT, "1").trim().toLongOrNull() == null) return Problem(Problem.Kind.NUMBER, e.value)
                ExtraKind.YES_NO -> if (e.value != "true" && e.value != "false") return Problem(Problem.Kind.YES_NO, e.value)
            }
        }
        return null
    }

    /** A link an app is handed follows the rule for the user's own links (`Schemes`). */
    private fun address(address: String): Problem? = when (Schemes.refused(address)) {
        Schemes.Why.NONE -> Problem(Problem.Kind.ADDRESS)
        Schemes.Why.REFUSED -> Problem(Problem.Kind.REFUSED, Schemes.of(address).orEmpty())
        Schemes.Why.EMPTY -> Problem(Problem.Kind.NO_ADDRESS)
        null -> null
    }

    /**
     * The extras a kind of request brings by itself: the text to send, what to look for. A player is also told
     * what kind of thing to play ("whatever the words mean"), unless the user says that themselves.
     */
    private fun own(t: Request): List<Extra> = when (t.what) {
        What.SEND -> listOf(Extra(TEXT, ExtraKind.TEXT, t.text))
        What.SEARCH -> listOf(Extra(QUERY, ExtraKind.TEXT, t.text))
        What.PLAY -> listOfNotNull(Extra(FOCUS, ExtraKind.TEXT, ANYTHING).takeIf { t.extras.none { e -> e.name == FOCUS } }, Extra(QUERY, ExtraKind.TEXT, t.text))
        What.LINK, What.CUSTOM -> emptyList()
    }

    // ---- as an address

    /**
     * [r] as an `intent:` address, the way `Intent.toUri` writes one: the link without its scheme, then
     * `#Intent;`, the fields each closed by a semicolon, `end`. `{argument}` stays readable in it. It always
     * names the app's package, so it is a request to that app and to nobody else.
     */
    fun write(r: Request): String {
        val t = tidy(r)
        val scheme = Schemes.of(t.address)
        val out = StringBuilder("intent:").append(if (scheme == null) t.address else t.address.substring(scheme.length + 1)).append("#Intent;")
        fun field(name: String, value: String, allow: String = "") { out.append(name).append('=').append(encode(value, allow)).append(';') }
        if (scheme != null) field("scheme", scheme)
        val action = when (t.what) {
            What.LINK -> VIEW; What.SEND -> SEND; What.SEARCH -> SEARCH; What.PLAY -> PLAY
            What.CUSTOM -> t.action.ifEmpty { if (t.address.isEmpty()) MAIN else VIEW }
        }
        // An address without an action is one to view: Android reads it so.
        if (action != VIEW) field("action", action)
        val type = if (t.what == What.SEND) PLAIN else t.type
        if (type.isNotEmpty()) field("type", type, "/")
        field("package", t.app)
        if (t.activity.isNotEmpty()) field("component", t.app + "/" + t.activity, "/")
        for (e in own(t) + t.extras) {
            val value = if (e.kind == ExtraKind.NUMBER) e.value.trim() else e.value
            // A number is a whole one; one too large for Android's plain number is handed over as a long one.
            val letter = when (e.kind) { ExtraKind.TEXT -> "S"; ExtraKind.YES_NO -> "B"; ExtraKind.NUMBER -> if (value.toLongOrNull().let { it != null && (it < Int.MIN_VALUE || it > Int.MAX_VALUE) }) "l" else "i" }
            field(letter + "." + encode(e.name), value, "{}")
        }
        return out.append("end").toString()
    }

    /** What an address or a line says, before it is sorted into the form's kinds. */
    private class Parts(val app: String, val action: String, val address: String, val activity: String, val type: String, val extras: List<Extra>)

    /** The form for an `intent:` address: one of the user's own as it was saved, or one that was pasted. */
    fun read(address: String): Read = when (val p = parts(address.trim())) { is Parts -> shape(p); else -> Read.Bad(p as Problem) }

    /** As `Intent.parseUri` reads it: the fields are after the last `#`, each `name=value;`, values percent-encoded. */
    private fun parts(uri: String): Any {
        val hash = uri.lastIndexOf('#')
        if (!uri.startsWith("intent:") || hash < 0 || !uri.startsWith("#Intent;", hash)) return Problem(Problem.Kind.NOT_A_LINE)
        val data = uri.substring(7, hash)
        var scheme = ""; var action = ""; var type = ""; var pkg = ""; var component = ""
        val extras = ArrayList<Extra>()
        var i = hash + 8
        while (!uri.startsWith("end", i)) {
            val semi = uri.indexOf(';', i)
            if (semi < 0) return Problem(Problem.Kind.UNFINISHED)
            val eq = uri.indexOf('=', i).let { if (it < 0 || it > semi) semi else it }
            val name = uri.substring(i, eq)
            val value = if (eq < semi) decode(uri.substring(eq + 1, semi)) else ""
            when {
                name == "scheme" -> scheme = value
                name == "action" -> action = value
                name == "type" -> type = value
                name == "package" -> pkg = value
                name == "component" -> component = value
                name.length > 2 && name[1] == '.' -> extras += when (name[0]) {
                    'S' -> Extra(decode(name.substring(2)), ExtraKind.TEXT, value)
                    'i' -> Extra(decode(name.substring(2)), ExtraKind.NUMBER, value)
                    // The form's number is Android's plain one, and a long one only where it is too large for that ([write]):
                    // a long one that would be written back as a plain one is not taken.
                    'l' -> if (value.toLongOrNull().let { it != null && (it < Int.MIN_VALUE || it > Int.MAX_VALUE) }) Extra(decode(name.substring(2)), ExtraKind.NUMBER, value)
                        else return Problem(Problem.Kind.EXTRA_TYPE, "l.")
                    // (Android reads anything but "true" as no. Here anything but the two words is named.)
                    'B' -> Extra(decode(name.substring(2)), ExtraKind.YES_NO, value.lowercase().takeIf { it == "true" || it == "false" } ?: return Problem(Problem.Kind.YES_NO, value))
                    else -> return Problem(Problem.Kind.EXTRA_TYPE, name.substring(0, 2))
                }
                // A category, flags, a selector, bounds: the form has no place for them.
                else -> return Problem(Problem.Kind.FIELD, name)
            }
            i = semi + 1
        }
        val (owner, activity) = if (component.isEmpty()) "" to "" else component(component) ?: return Problem(Problem.Kind.COMPONENT, component)
        if (pkg.isNotEmpty() && owner.isNotEmpty() && pkg != owner) return Problem(Problem.Kind.TWO_APPS)
        return Parts(pkg.ifEmpty { owner }, action.ifEmpty { VIEW }, if (scheme.isEmpty()) data else "$scheme:$data", activity, type, extras)
    }

    /** `app/activity`, the activity's name in full or starting with a dot after the app's. */
    private fun component(value: String): Pair<String, String>? {
        val slash = value.indexOf('/')
        if (slash <= 0 || slash == value.lastIndex) return null
        val pkg = value.substring(0, slash)
        val cls = value.substring(slash + 1)
        return pkg to if (cls.startsWith(".")) pkg + cls else cls
    }

    /** A yes or no as `am` takes it after `--ez`. */
    private fun yesNo(value: String): String? = when (value.lowercase()) { "true", "t", "1" -> "true"; "false", "f", "0" -> "false"; else -> null }

    /** Sorts what was read into the form: one of the four plain kinds if it is exactly that, else a custom action with everything shown. */
    private fun shape(p: Parts): Read {
        val action = p.action.ifEmpty { if (p.address.isEmpty()) MAIN else VIEW }
        fun text(name: String) = p.extras.singleOrNull { it.name == name }?.takeIf { it.kind == ExtraKind.TEXT }
        val bare = p.address.isEmpty() && p.activity.isEmpty()
        val sent = text(TEXT)
        val query = text(QUERY)
        val r = when {
            action == VIEW && p.address.isNotEmpty() && p.activity.isEmpty() && p.type.isEmpty() -> Request(p.app, What.LINK, address = p.address, extras = p.extras)
            action == SEND && bare && (p.type.isEmpty() || p.type == PLAIN) && sent != null -> Request(p.app, What.SEND, text = sent.value, extras = p.extras - sent)
            action == SEARCH && bare && p.type.isEmpty() && query != null -> Request(p.app, What.SEARCH, text = query.value, extras = p.extras - query)
            action == PLAY && bare && p.type.isEmpty() && query != null -> Request(p.app, What.PLAY, text = query.value, extras = (p.extras - query).filterNot { it.name == FOCUS && it.value == ANYTHING })
            // (A link with no action named is one to view, and the form says so; an activity alone needs no action.)
            else -> Request(p.app, What.CUSTOM, action = if (p.address.isEmpty()) p.action else action, address = p.address, activity = p.activity, type = p.type, extras = p.extras)
        }
        return if (r.extras.size > MAX_EXTRAS) Read.Bad(Problem(Problem.Kind.TOO_MANY)) else Read.Ok(r)
    }

    /**
     * The address to start for a saved command, with [argument] where it says `{argument}`: encoded for an
     * address inside the link, as it was typed inside a text or an extra. The command is read once: a
     * placeholder that arrives inside the text stays text. Null if the command cannot be read, or the text
     * does not fit it (a number is wanted and it is none).
     */
    fun fill(template: String, argument: String): String? {
        val r = (read(template) as? Read.Ok)?.request ?: return null
        val filled = r.copy(
            address = r.address.replace(ARGUMENT, Templates.encode(argument)), text = r.text.replace(ARGUMENT, argument),
            extras = r.extras.map { it.copy(value = it.value.replace(ARGUMENT, argument)) },
        )
        return if (why(filled) != null) null else write(filled)
    }

    // ---- a line from a terminal

    /** What was pasted into the editor: an `intent:` address, or an `am start …` line. */
    fun paste(text: String): Read = text.trim().let { if (it.startsWith("intent:")) read(it) else am(it) }

    private val STARTS = setOf("start", "start-activity")

    /**
     * The form for an `am start …` line (with `adb shell` before it or without): `-a` the action, `-d` the
     * link, `-t` the type, `-n` the activity, `-p` the app, `--es` or `-e` a text, `--ei` a number, `--ez` a
     * yes or no; and the one bare word `am` takes at the end: a link, `app/activity`, or the app. `--user` and
     * its value are passed over: they say who runs the line, not what is asked. Any other flag is named as a
     * [Problem].
     */
    fun am(line: String): Read {
        var w = words(line) ?: return Read.Bad(Problem(Problem.Kind.QUOTE))
        if (w.firstOrNull() == "$") w = w.drop(1)
        if (w.firstOrNull() == "adb") {
            val shell = w.indexOf("shell")
            if (shell < 0) return Read.Bad(Problem(Problem.Kind.NOT_A_LINE))
            w = w.drop(shell + 1)
            // adb shell "am start …": the line is one quoted word.
            if (w.size == 1) w = words(w[0]) ?: return Read.Bad(Problem(Problem.Kind.QUOTE))
        }
        var i = when {
            w.size >= 2 && w[0] == "am" && w[1] in STARTS -> 2
            w.size >= 3 && w[0] == "cmd" && w[1] == "activity" && w[2] in STARTS -> 3
            else -> return Read.Bad(Problem(Problem.Kind.NOT_A_LINE))
        }
        var action = ""; var data = ""; var type = ""; var pkg = ""; var component = ""
        var word: String? = null
        val extras = LinkedHashMap<String, Extra>()
        while (i < w.size) {
            val flag = w[i]
            fun value(): String? = w.getOrNull(++i)
            when (flag) {
                "-a" -> action = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                "-d" -> data = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                "-t" -> type = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                "-n" -> component = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                "-p" -> pkg = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                // Which of the device's users runs it: it says nothing of what is asked, and a line from a terminal very often has it.
                "--user" -> value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                "-e", "--es", "--ei", "--ez" -> {
                    val name = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                    val v = value() ?: return Read.Bad(Problem(Problem.Kind.VALUE, flag))
                    extras[name] = when (flag) {
                        "--ei" -> Extra(name, ExtraKind.NUMBER, v.takeIf { it.replace(ARGUMENT, "1").toLongOrNull() != null } ?: return Read.Bad(Problem(Problem.Kind.NUMBER, v)))
                        "--ez" -> Extra(name, ExtraKind.YES_NO, yesNo(v) ?: return Read.Bad(Problem(Problem.Kind.YES_NO, v)))
                        else -> Extra(name, ExtraKind.TEXT, v)
                    }
                }
                else -> if (flag.startsWith("-")) return Read.Bad(Problem(Problem.Kind.FLAG, flag)) else if (word != null) return Read.Bad(Problem(Problem.Kind.WORD, flag)) else word = flag
            }
            i++
        }
        // The bare word fills in what the flags left open, as `am` has it: an address, an activity, or the app.
        var base: Parts? = null
        when {
            word == null -> {}
            word.startsWith("intent:") -> base = parts(word).let { it as? Parts ?: return Read.Bad(it as Problem) }
            // (Where a flag has said it already, `am` would take the flag and say nothing of the word: here it is named.)
            ':' in word -> if (data.isEmpty()) data = word else return Read.Bad(Problem(Problem.Kind.WORD, word))
            '/' in word -> if (component.isEmpty()) component = word else return Read.Bad(Problem(Problem.Kind.WORD, word))
            else -> if (pkg.isEmpty()) pkg = word else return Read.Bad(Problem(Problem.Kind.WORD, word))
        }
        val (owner, activity) = if (component.isEmpty()) "" to "" else component(component) ?: return Read.Bad(Problem(Problem.Kind.COMPONENT, component))
        val apps = listOf(pkg, owner, base?.app.orEmpty()).filter { it.isNotEmpty() }.distinct()
        if (apps.size > 1) return Read.Bad(Problem(Problem.Kind.TWO_APPS))
        val all = base?.extras.orEmpty().filter { it.name !in extras } + extras.values
        return shape(Parts(
            apps.firstOrNull().orEmpty(), action.ifEmpty { base?.action.orEmpty() }, data.ifEmpty { base?.address.orEmpty() },
            activity.ifEmpty { base?.activity.orEmpty() }, type.ifEmpty { base?.type.orEmpty() }, all,
        ))
    }

    /**
     * A command line in its words, as a shell parts them: spaces part them, quotes of either kind keep them
     * together, a backslash takes the next character as it is. Null if a quote is left open.
     */
    internal fun words(line: String): List<String>? {
        val out = ArrayList<String>()
        val word = StringBuilder()
        var begun = false
        var quote = ' '
        var i = 0
        while (i < line.length) {
            val c = line[i]
            val next = line.getOrNull(i + 1)
            when {
                quote == '\'' -> if (c == '\'') quote = ' ' else word.append(c)
                quote == '"' -> when {
                    c == '"' -> quote = ' '
                    c == '\\' && next != null && next in "\"\\\$`" -> { word.append(next); i++ }
                    else -> word.append(c)
                }
                c == '\'' || c == '"' -> { quote = c; begun = true }
                // A backslash at the end of a line only says that the line goes on.
                c == '\\' && next == '\n' -> i++
                c == '\\' && next != null -> { word.append(next); begun = true; i++ }
                c.isWhitespace() -> if (begun) { out += word.toString(); word.clear(); begun = false }
                else -> { word.append(c); begun = true }
            }
            i++
        }
        if (quote != ' ') return null
        if (begun) out += word.toString()
        return out
    }

    // ---- the letters of an address

    /** As Android's `Uri.encode`: everything but letters, digits and a few marks as `%XX` of its UTF-8 bytes; [allow] stays as it is. */
    internal fun encode(value: String, allow: String = ""): String {
        val out = StringBuilder(value.length + 8)
        for (b in value.toByteArray(Charsets.UTF_8)) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "_-!.~'()*" || c in allow) out.append(c)
            else out.append('%').append(HEX[b.toInt() shr 4 and 0xF]).append(HEX[b.toInt() and 0xF])
        }
        return out.toString()
    }

    /** The other way. A `%` that starts no byte stays a `%`. */
    internal fun decode(value: String): String {
        if ('%' !in value) return value
        val out = StringBuilder(value.length)
        val bytes = java.io.ByteArrayOutputStream()
        fun flush() { if (bytes.size() > 0) { out.append(bytes.toString(Charsets.UTF_8.name())); bytes.reset() } }
        var i = 0
        while (i < value.length) {
            val hi = if (value[i] == '%') value.getOrNull(i + 1)?.digitToIntOrNull(16) else null
            val lo = if (hi != null) value.getOrNull(i + 2)?.digitToIntOrNull(16) else null
            if (hi != null && lo != null) { bytes.write(hi * 16 + lo); i += 3 } else { flush(); out.append(value[i]); i++ }
        }
        flush()
        return out.toString()
    }

    private const val HEX = "0123456789ABCDEF"
}
