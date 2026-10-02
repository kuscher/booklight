package io.github.kuscher.booklight.core

import java.time.Duration
import java.util.Base64

/** Why Spotify gave nothing to play, for the row to say in plain words. */
enum class SongFailure {
    /** Spotify knows nothing by that name that can be played. */
    NOT_FOUND,
    /** The key was not accepted: mistyped, taken back, or of an account that may not use it. */
    REFUSED,
    /** Too many requests for now. */
    TOO_MANY,
    /** The device reached nobody. */
    OFFLINE,
    /** Anything else: a reply that could not be read, an error of the service's own. */
    NO_ANSWER,
}

/**
 * What Spotify found for a request, and the [address] that plays it when it is sent to the Spotify
 * app. [kind] is SONG, ALBUM, ARTIST or PLAYLIST. [artists]: who a song or an album is by. [album]:
 * the album a song is on. [owner]: whose playlist it is. [starts]: the song an album or an artist
 * begins with (an album is played from its first song, an artist from one of theirs).
 */
data class Song(
    val kind: PlayMode, val name: String, val address: String,
    val artists: List<String> = emptyList(), val album: String = "", val owner: String = "", val starts: String = "",
)

/**
 * Spotify's Web API, read: the addresses to ask, what its replies say, and the link that plays what
 * was found. The key is the user's own (a client id and its secret); with it a token is got that
 * lasts an hour, and with the token the catalogue is searched. Nothing here is about a user's
 * account: the token is the app's, not a person's.
 *
 * What is written here was seen in its replies on 2 October 2026, asked with a key in development
 * mode (every key of a person's own is one), and the files the tests read are those replies: a
 * search gives fewer items than it is asked for (five where ten were asked), an artist's top tracks
 * are refused (403), Spotify's own playlists come back as `null`, and a track no longer says how
 * popular it is. So an artist is played from the first song of their own that a search for their
 * songs finds. Which links play was tried on a Googlebook (docs/research/intents.md): all four of
 * the ones made here. A reply from the network is never trusted to be well formed.
 */
object Spotify {
    const val NAME = "Spotify"
    /** The Spotify app. */
    const val PACKAGE = "com.spotify.music"
    /** Where a key is got. */
    const val DASHBOARD = "https://developer.spotify.com/dashboard"
    /** Where a token is got: a POST of [TOKEN_FORM], with [basic] as its authorization. */
    const val TOKEN = "https://accounts.spotify.com/api/token"
    const val TOKEN_FORM = "grant_type=client_credentials"
    private const val API = "https://api.spotify.com/v1/"

    /** What a request came back with. */
    class Fetched(val status: Int, val body: String)

    /** Thrown by whoever fetches when the device reached nobody: the row then says "No connection". */
    class Unreachable : Exception()

    /** What a reply held: [value], or why not. */
    class Said<out T>(val value: T?, val failure: SongFailure? = null)

    /** A token and how many seconds it lasts. */
    class Token(val value: String, val seconds: Int)

    /** What a lookup came to: what to play, or why there is nothing. */
    class Answer(val song: Song?, val failure: SongFailure?)

    /** The authorization a token is asked with: the key's two halves. */
    fun basic(id: String, secret: String): String = "Basic " + Base64.getEncoder().encodeToString("${id.trim()}:${secret.trim()}".toByteArray(Charsets.UTF_8))

    fun token(reply: Fetched): Said<Token> {
        val all = read(reply)
        val m = all.value ?: return Said(null, all.failure)
        val value = m.text("access_token") ?: return Said(null, SongFailure.NO_ANSWER)
        val seconds = (m["expires_in"] as? Double)?.toInt()?.takeIf { it > 0 } ?: return Said(null, SongFailure.NO_ANSWER)
        return Said(Token(value, seconds))
    }

    /** How long a token is used: until a minute before it ends. */
    fun lasts(token: Token): Duration = Duration.ofSeconds((token.seconds - 60L).coerceAtLeast(0))

    /** How long an answer is kept before Spotify is asked again for the same request. */
    fun keep(failure: SongFailure?): Duration = when (failure) {
        // What may be right again in a moment is asked again soon.
        SongFailure.OFFLINE, SongFailure.NO_ANSWER -> Duration.ofSeconds(10)
        SongFailure.TOO_MANY -> Duration.ofSeconds(30)
        // What was found, that nothing was, or a refused key (a new key is asked at once): five minutes.
        else -> Duration.ofMinutes(5)
    }

    // ---- the addresses to ask

    /**
     * The search for [what]. The kind word chooses what is looked for; free text looks for songs and
     * artists at once, by its words as they stand, "by" and all: Spotify's own ranking finds "Stand
     * by Me". A song or an album that a kind word names, with "by", is asked for by its fields. [market] is the
     * country whose catalogue is searched: with a token that is nobody's, Spotify needs to be told.
     */
    fun search(what: PlayRequest, market: String): String = when (what.mode) {
        PlayMode.ANY -> search(what.query, "track,artist", WIDE, market)
        PlayMode.SONG -> search(if (what.artist.isEmpty()) what.title else "track:${phrase(what.title)} artist:${phrase(what.artist)}", "track", FEW, market)
        PlayMode.ALBUM -> search(if (what.artist.isEmpty()) what.album else "album:${phrase(what.album)} artist:${phrase(what.artist)}", "album", FEW, market)
        PlayMode.ARTIST -> search(what.artist, "artist", FEW, market)
        PlayMode.PLAYLIST -> search(what.playlist, "playlist", WIDE, market)
        // Spotify has no genre to play: a playlist of that name is the nearest thing that goes on playing.
        PlayMode.GENRE -> search(what.genre, "playlist", WIDE, market)
    }

    /**
     * The second search, for when the first found nothing. After a search by fields: the same words as
     * free text. After free text with "by" in it: its reading as a title and who it is by. Null
     * where there is no other way to ask.
     */
    fun second(what: PlayRequest, market: String): String? = when {
        what.artist.isEmpty() -> null
        what.mode == PlayMode.ANY -> if (what.title.isEmpty()) null else search("track:${phrase(what.title)} artist:${phrase(what.artist)}", "track", FEW, market)
        what.mode == PlayMode.SONG -> search("${what.title} ${what.artist}", "track", FEW, market)
        what.mode == PlayMode.ALBUM -> search("${what.album} ${what.artist}", "album", FEW, market)
        else -> null
    }

    /** An album's first song. */
    fun albumTracks(id: String, market: String): String = API + "albums/$id/tracks?limit=1&market=" + market(market)

    /**
     * Songs by an artist, by name: the first of them that is their own is where the artist is played
     * from. (Their top tracks cannot be asked for with a key of this kind. And the first song found
     * may be somebody else's on which they are guests: for Daft Punk it was.)
     */
    fun artistTracks(name: String, market: String): String = search("artist:${phrase(name)}", "track", WIDE, market)

    private fun search(q: String, types: String, limit: Int, market: String) =
        API + "search?q=" + java.net.URLEncoder.encode(q, "UTF-8").replace("+", "%20") + "&type=" + types + "&limit=" + limit + "&market=" + market(market)

    /** A field's value as one phrase. A quotation mark of the user's own would end it early. */
    private fun phrase(s: String) = "\"" + s.replace('"', ' ').trim() + "\""

    /** Two letters, as Spotify names a country; anything else is the United States, where the catalogue is widest. */
    private fun market(m: String) = m.trim().uppercase().takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } } ?: "US"

    // ---- the links that play

    fun track(id: String) = "spotify:track:$id"
    /** A song with its album as what goes on playing after it. */
    fun inAlbum(track: String, album: String) = "spotify:track:$track?context=spotify%3Aalbum%3A$album"
    /** A song with its artist as what goes on playing after it. */
    fun byArtist(track: String, artist: String) = "spotify:track:$track?context=spotify%3Aartist%3A$artist"
    fun playlist(id: String) = "spotify:playlist:$id:play"

    // ---- the lookup

    /**
     * What to play for [what], asked for through [get] (the address in, the reply out; it may
     * throw). One request for a song or a playlist; two for an album (its first song) and for an
     * artist (a song of their own); one more when the first search found nothing and there is another
     * way to ask ([second]). Three at most.
     */
    fun lookup(what: PlayRequest, market: String, get: (String) -> Fetched): Answer {
        fun ask(url: String): Said<Map<*, *>> = try { read(get(url)) } catch (_: Unreachable) { Said(null, SongFailure.OFFLINE) } catch (_: Exception) { Said(null, SongFailure.NO_ANSWER) }
        var first = ask(search(what, market))
        // Free text asks for songs and artists at once. Should that ever get no answer, songs alone are asked for: the
        // search every reply here was seen for.
        if (what.mode == PlayMode.ANY && first.failure == SongFailure.NO_ANSWER) first = ask(search(what.query, "track", FEW, market))
        var reply = first.value ?: return Answer(null, first.failure ?: SongFailure.NO_ANSWER)
        var found = pick(what, reply)
        if (found == null) second(what, market)?.let { url ->
            val again = ask(url)
            reply = again.value ?: return Answer(null, again.failure ?: SongFailure.NO_ANSWER)
            found = pick(what, reply)
        }
        return when (val f = found) {
            null -> Answer(null, SongFailure.NOT_FOUND)
            is Track -> Answer(Song(PlayMode.SONG, f.name, track(f.id), f.artists.map { it.name }, f.album), null)
            is Named -> when (f.kind) {
                PlayMode.PLAYLIST -> Answer(Song(PlayMode.PLAYLIST, f.name, playlist(f.id), owner = f.by.firstOrNull().orEmpty()), null)
                PlayMode.ALBUM -> {
                    val tracks = ask(albumTracks(f.id, market))
                    val start = tracks.value?.let { tracks(it["items"]).firstOrNull() } ?: return Answer(null, tracks.failure ?: SongFailure.NOT_FOUND)
                    Answer(Song(PlayMode.ALBUM, f.name, inAlbum(start.id, f.id), f.by, starts = start.name), null)
                }
                else -> {
                    // An artist: a song of their own from the same reply (free text that turned out to be their name), or
                    // else from a search for their songs.
                    var start = own(f.id, tracks(reply.items("tracks")))
                    if (start == null) {
                        val more = ask(artistTracks(f.name, market))
                        start = own(f.id, tracks((more.value ?: return Answer(null, more.failure ?: SongFailure.NO_ANSWER)).items("tracks")))
                    }
                    start?.let { Answer(Song(PlayMode.ARTIST, f.name, byArtist(it.id, f.id), starts = it.name), null) } ?: Answer(null, SongFailure.NOT_FOUND)
                }
            }
        }
    }

    /** Somebody a song is by. */
    private class Who(val id: String, val name: String)
    private sealed interface Found
    /** A song that can be played. */
    private class Track(val id: String, val name: String, val artists: List<Who>, val album: String) : Found
    /** An album, an artist or a playlist: [by] is the album's artists or the playlist's owner. */
    private class Named(val kind: PlayMode, val id: String, val name: String, val by: List<String>) : Found

    /**
     * What a search's reply holds for [what]: the first thing of the kind that was asked for which has to do with what
     * was typed ([near]). Spotify's search always finds something: for a name it does not know it answers with
     * whatever is popular, and Enter must not play that.
     */
    private fun pick(what: PlayRequest, reply: Map<*, *>): Found? = when (what.mode) {
        PlayMode.SONG -> tracks(reply.items("tracks")).firstOrNull { near(what, it) }
        PlayMode.ALBUM -> named(reply.items("albums"), PlayMode.ALBUM).firstOrNull { near(what, it) }
        PlayMode.PLAYLIST, PlayMode.GENRE -> named(reply.items("playlists"), PlayMode.PLAYLIST).firstOrNull { near(what, it) }
        PlayMode.ARTIST -> named(reply.items("artists"), PlayMode.ARTIST).let { all -> all.firstOrNull { same(it.name, what.artist) } ?: all.firstOrNull { near(what, it) } }
        PlayMode.ANY -> {
            val songs = tracks(reply.items("tracks"))
            // Free text is an artist only if it is an artist's whole name and one of the songs found for it is their own:
            // a song's name is also the name of somebody ("Bohemian Rhapsody" has tribute bands), and the songs found are not theirs.
            named(reply.items("artists"), PlayMode.ARTIST).firstOrNull { same(it.name, what.query) && own(it.id, songs) != null } ?: songs.firstOrNull { near(what, it) }
        }
    }

    /** Whether what was found has to do with what was typed: one word of the one is, or nearly is, a word of the other. */
    private fun near(what: PlayRequest, found: Found): Boolean {
        val typed = words(listOf(what.query, what.title, what.artist, what.album, what.playlist, what.genre).joinToString(" "))
        val theirs = words(when (found) {
            is Track -> (listOf(found.name, found.album) + found.artists.map { it.name }).joinToString(" ")
            is Named -> (listOf(found.name) + found.by).joinToString(" ")
        })
        return typed.any { t -> theirs.any { alike(t, it) } }
    }

    /** The words of a text that say something: folded, three letters or more, without the words every title has. If none is left, all of them. */
    private fun words(s: String): List<String> = Matcher.fold(s).split(' ').filter { it.isNotEmpty() }.let { all -> all.filter { it.length >= 3 && it !in EMPTY_WORDS }.ifEmpty { all } }

    /** The same word, one that begins the other (four letters or more), or the same but for a slip: one letter in a word of five or more, two in one of nine or more. */
    internal fun alike(a: String, b: String): Boolean {
        if (a == b) return true
        if (minOf(a.length, b.length) >= 4 && (a.startsWith(b) || b.startsWith(a))) return true
        val long = maxOf(a.length, b.length)
        val slips = if (long >= 9) 2 else if (long >= 5) 1 else 0
        return slips > 0 && kotlin.math.abs(a.length - b.length) <= slips && distance(a, b, slips) <= slips
    }

    /** How many letters must change, come or go to make [a] into [b]; anything over [most] is just "more". */
    private fun distance(a: String, b: String, most: Int): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val row = IntArray(b.length + 1); row[0] = i
            var least = row[0]
            for (j in 1..b.length) {
                row[j] = minOf(prev[j] + 1, row[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
                if (row[j] < least) least = row[j]
            }
            if (least > most) return most + 1
            prev = row
        }
        return prev[b.length]
    }

    /** Words that are in too many titles and requests to mean that two of them belong together. */
    private val EMPTY_WORDS = setOf("the", "and", "von", "der", "die", "das", "und", "for", "from", "with", "feat", "that", "this", "song", "lied", "music", "musik", "about", "version", "remastered", "remaster", "edit", "live")

    /** The first of [songs] that is the artist's own: they are named first on it. A song they are guests on is somebody else's. */
    private fun own(artist: String, songs: List<Track>): Track? = songs.firstOrNull { it.artists.firstOrNull()?.id == artist }

    /** The same name, whatever its case and accents, with or without "the" before it. */
    private fun same(a: String, b: String): Boolean = key(a).let { it.isNotEmpty() && it == key(b) }
    private fun key(s: String) = Matcher.fold(s).removePrefix("the ")

    /** The songs of a list that can be played here: with an id, not marked as unplayable in this market, not a file of somebody's own. */
    private fun tracks(items: Any?): List<Track> = (items as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let(::track) }

    private fun track(m: Map<*, *>): Track? {
        val id = m.id() ?: return null
        if (m["is_playable"] == false || m["is_local"] == true) return null
        val artists = (m["artists"] as? List<*>).orEmpty().mapNotNull { a -> (a as? Map<*, *>)?.let { Who(it.id().orEmpty(), it.text("name") ?: return@let null) } }
        return Track(id, m.text("name") ?: return null, artists, (m["album"] as? Map<*, *>)?.text("name").orEmpty())
    }

    /** The albums, artists or playlists of a list. A list of playlists holds `null` where one is not to be had (Spotify's own are not). */
    private fun named(items: Any?, kind: PlayMode): List<Named> = (items as? List<*>).orEmpty().mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        if (m["is_playable"] == false) return@mapNotNull null
        val by = when (kind) {
            PlayMode.ALBUM -> (m["artists"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.text("name") }
            PlayMode.PLAYLIST -> listOfNotNull((m["owner"] as? Map<*, *>)?.text("display_name"))
            else -> emptyList()
        }
        Named(kind, m.id() ?: return@mapNotNull null, m.text("name") ?: return@mapNotNull null, by)
    }

    /** The reply as an object, or why not. An error says so in its text; where it does not, the status does. */
    private fun read(f: Fetched): Said<Map<*, *>> {
        val all = try { Json.parse(f.body) as? Map<*, *> } catch (_: Exception) { null }
        val error = all?.get("error")
        if (error != null || f.status != 200) {
            // The token's address says `"error": "invalid_client"`; every other, an object with the status in it.
            if (error is String) return Said(null, if (error == "invalid_client") SongFailure.REFUSED else SongFailure.NO_ANSWER)
            return Said(null, when (((error as? Map<*, *>)?.get("status") as? Double)?.toInt() ?: f.status) {
                401, 403 -> SongFailure.REFUSED
                429 -> SongFailure.TOO_MANY
                else -> SongFailure.NO_ANSWER
            })
        }
        return if (all == null) Said(null, SongFailure.NO_ANSWER) else Said(all)
    }

    private val ID = Regex("[A-Za-z0-9]{1,64}")
    /** A Spotify id: letters and digits. Anything else never goes into an address. */
    private fun Map<*, *>.id(): String? = (this["id"] as? String)?.takeIf { ID.matches(it) }
    private fun Map<*, *>.text(key: String): String? = (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    /** The items under one of a search reply's keys (`tracks`, `albums`…). */
    private fun Map<*, *>.items(key: String): Any? = (this[key] as? Map<*, *>)?.get("items")

    /** How many items a search asks for: a few where the first is taken, the most there is where one of them must fit another (five came back for ten). */
    private const val FEW = 5
    private const val WIDE = 10
}
