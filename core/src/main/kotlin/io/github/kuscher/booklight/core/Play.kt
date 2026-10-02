package io.github.kuscher.booklight.core

/**
 * What kind of thing is asked for: Android's "play from search" request has a mode for each, and
 * [ANY] is free text. Recipes save a mode by its number: new ones go at the end.
 */
enum class PlayMode { ANY, SONG, ALBUM, ARTIST, PLAYLIST, GENRE }

/**
 * What was typed after `play`, split. [query] is the music as it was typed, without the kind word
 * and without the player: the text a player that only searches looks for. The other fields are the
 * names the [mode] has; one that was not given is empty. Free text ([PlayMode.ANY]) is claimed to
 * be nothing: where it has "by" in it, [title] and [artist] are a second reading of it, for when its
 * words as they stand find nothing ("stand by me" is not "stand" by "me"). [player]: which of the
 * players' names the text ended with ("… on spotify"), as its place in the list that was passed in;
 * -1 for none.
 */
data class PlayRequest(
    val mode: PlayMode, val query: String,
    val title: String = "", val artist: String = "", val album: String = "", val playlist: String = "", val genre: String = "",
    val player: Int = -1,
) {
    /** What is to be played, without who it is by: the name a row shows. */
    val name: String get() = when (mode) {
        PlayMode.ANY -> query
        PlayMode.SONG -> title
        PlayMode.ALBUM -> album
        PlayMode.ARTIST -> artist
        PlayMode.PLAYLIST -> playlist
        PlayMode.GENRE -> genre
    }
}

/**
 * Reads the text after `play`: "album discovery by daft punk on spotify". It only splits the text.
 * Which apps are players is the caller's question: it passes their names in, and only one of those
 * after "on" is a player ("smoke on the water" stays a title). English and German words both count,
 * whatever the device's language; case, accents and extra spaces don't matter ([Matcher.fold]).
 */
object Play {
    /** A word at the start that says what kind of thing follows. Alone, it is the music's own name ("album"). */
    private val KINDS: Map<String, PlayMode> = buildMap {
        for (w in listOf("song", "track", "lied", "titel")) put(w, PlayMode.SONG)
        put("album", PlayMode.ALBUM)
        for (w in listOf("artist", "kunstler", "kuenstler", "kunstlerin", "kuenstlerin", "interpret", "interpretin")) put(w, PlayMode.ARTIST)
        put("playlist", PlayMode.PLAYLIST)
        put("genre", PlayMode.GENRE)
    }
    private val BY = setOf("by", "von")
    private val ON = setOf("on", "in", "auf")

    /**
     * Null when there is nothing to play: no text. [kinds] false: the text is a name as it stands (the
     * model's answer, put into the field), and its first word is not a kind word: "Song 2" is a title.
     */
    fun read(text: String, players: List<String> = emptyList(), kinds: Boolean = true): PlayRequest? {
        val words = Words.of(text).map { it.text }
        if (words.isEmpty()) return null
        val folded = words.map(Matcher::fold)
        fun cut(from: Int, to: Int) = words.subList(from, to).joinToString(" ")

        // "… on spotify": the first "on" that has music before it and a player's name after it, so a player whose own
        // name holds such a word ("plug in player") is still named whole. (What follows an "on" is only looked at if it
        // has no more letters than the longest name: a pasted page full of "in" costs nothing.)
        val names = players.map(Matcher::fold)
        val room = names.maxOfOrNull { it.length } ?: 0
        val after = IntArray(words.size + 1)
        for (i in words.indices.reversed()) after[i] = after[i + 1] + folded[i].count { it != ' ' }
        var end = words.size
        var player = -1
        for (i in 1 until words.size - 1) {
            if (folded[i] !in ON || after[i + 1] > room) continue
            val p = named(cut(i + 1, words.size), names)
            if (p >= 0) { end = i; player = p; break }
        }

        val mode = KINDS[folded[0]]?.takeIf { kinds && end > 1 } ?: PlayMode.ANY
        val start = if (mode == PlayMode.ANY) 0 else 1
        // "by" stays in the query: a player that only searches finds "stand by me" by those three words, not by two of them.
        val query = cut(start, end)
        // "X by Y": the last "by" with words on both sides. Not for an artist, a playlist or a genre: they are not by anybody.
        val by = if (mode == PlayMode.ARTIST || mode == PlayMode.PLAYLIST || mode == PlayMode.GENRE) null
            else (end - 2 downTo start + 1).firstOrNull { folded[it] in BY }
        val name = if (by != null) cut(start, by) else query
        val artist = if (by != null) cut(by + 1, end) else ""
        return when (mode) {
            // Free text stays free text. With "by" in it, the title and who it is by are kept as a second reading: only a
            // kind word makes them the first ("song stand by me by ben e king").
            PlayMode.ANY -> if (by == null) PlayRequest(PlayMode.ANY, query, player = player) else PlayRequest(PlayMode.ANY, query, title = name, artist = artist, player = player)
            PlayMode.SONG -> PlayRequest(mode, query, title = name, artist = artist, player = player)
            PlayMode.ALBUM -> PlayRequest(mode, query, album = name, artist = artist, player = player)
            PlayMode.ARTIST -> PlayRequest(mode, query, artist = query, player = player)
            PlayMode.PLAYLIST -> PlayRequest(mode, query, playlist = query, player = player)
            PlayMode.GENRE -> PlayRequest(mode, query, genre = query, player = player)
        }
    }

    /**
     * What the device's own model is asked when the text is a description or misspelt ("that queen
     * song about the poor boy"). Only ever on the user's Enter, and its answer is shown before
     * anything is made of it: the model also turns a right title into another song.
     */
    fun question(text: String): String = "Which song is meant? Reply with only the title, then a slash, then the artist.\n\n${text.trim()}"

    /**
     * The title and the artist in the model's reply, as it wrote them: "Bohemian Rhapsody / Queen" is
     * those two. Its first line counts, without quotation marks and without "Title:" before a part.
     * A slash with spaces round it divides them ("Back in Black / AC/DC"); where there is none, the
     * first slash does. One part where it wrote no slash, none where it said nothing.
     */
    fun song(reply: String): List<String> {
        val line = reply.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return emptyList()
        val parts = if (" / " in line) line.split(" / ") else line.split("/", limit = 2)
        return parts.map { part -> LABEL.replace(part.trim().trim(*MARKS), "").trim().trim(*MARKS).trim() }.filter { it.isNotEmpty() }.take(2)
    }

    /**
     * The model's reply as text to type after `play`: "Bohemian Rhapsody / Queen" is "Bohemian
     * Rhapsody Queen". Null when it said nothing that could be a name.
     */
    fun named(reply: String): String? {
        val name = song(reply).joinToString(" ").replace(SPACES, " ")
        return name.takeIf { it.isNotEmpty() && it.length <= 120 && it.any(Char::isLetterOrDigit) }
    }

    private val LABEL = Regex("^(title|song|artist|titel|lied|interpret)\\s*:\\s*", RegexOption.IGNORE_CASE)
    private val MARKS = charArrayOf('"', '\'', '“', '”', '„', '‘', '’', '«', '»', '*', '_', '.', ' ')
    private val SPACES = Regex("\\s+")

    /**
     * Which of the players' [names] (folded) the words [tail] name: a whole name, or the start of one from two letters
     * on ("spot"). Of several the shortest, so "youtube" is YouTube before it is YouTube Music. -1 for none.
     */
    private fun named(tail: String, names: List<String>): Int {
        val t = Matcher.fold(tail)
        if (t.count { it != ' ' } < 2) return -1
        return names.withIndex().filter { it.value.startsWith(t) }.minByOrNull { it.value.length }?.index ?: -1
    }
}
