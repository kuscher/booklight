package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/**
 * Spotify's replies, read. The files in `resources/spotify` are what the service answered on 2 October
 * 2026 to a key in development mode, with `market=DE` (see the README there; `token.json` alone is
 * written by hand, a real one being a token).
 */
class SpotifyTest {
    private fun body(name: String) = javaClass.getResource("/spotify/$name.json")!!.readText()
    private fun reply(name: String, status: Int = 200) = Spotify.Fetched(status, body(name))
    private fun ok(body: String) = Spotify.Fetched(200, body)
    private fun what(text: String) = Play.read(text)!!
    private val api = "https://api.spotify.com/v1/"

    /** The items of a saved reply, as they stand in it, without the brackets round them. */
    private fun items(name: String) = body(name).substringAfter("\"items\":[").substringBeforeLast("]")
    /** What a saved search holds under its one key (`"artists":{…}`), to put beside another's. */
    private fun inner(name: String) = body(name).trim().removePrefix("{").removeSuffix("}")
    /** "Starboy", on which Daft Punk are The Weeknd's guests, and Daft Punk's own "One More Time": the songs found for them, in that order. */
    private val daftPunkSongs = "\"tracks\":{\"items\":[" + items("search-artist-tracks") + "," + items("album-tracks") + "]}"

    /** One reply of these parts: the songs of one search beside the artists of another. */
    private fun both(vararg parts: String) = parts.joinToString(",", "{", "}")

    private val bohemian = Song(PlayMode.SONG, "Bohemian Rhapsody", "spotify:track:2JiDi0qAXsPwhPqA2qaKGt", listOf("Queen"), "A Night At The Opera")
    private val daftPunk = Song(PlayMode.ARTIST, "Daft Punk", "spotify:track:0DiWol3AO6WpXZgp0goxAV?context=spotify%3Aartist%3A4tZwfgrHOc3mvqYlEYSvVi", starts = "One More Time")
    private val nothing = """{"tracks":{"items":[]},"albums":{"items":[]},"artists":{"items":[]},"playlists":{"items":[]}}"""

    /** A lookup with these replies, in the order they are asked for; the addresses that were asked are kept. */
    private class Asked(vararg val replies: Spotify.Fetched) {
        val urls = ArrayList<String>()
        fun get(url: String): Spotify.Fetched { urls.add(url); return replies[urls.size - 1] }
    }
    private fun lookup(text: String, vararg replies: Spotify.Fetched): Pair<Spotify.Answer, List<String>> {
        val asked = Asked(*replies)
        return Spotify.lookup(what(text), "DE", asked::get) to asked.urls
    }

    @Test fun `the search for each kind`() {
        assertEquals(api + "search?q=lofi%20beats&type=track,artist&limit=10&market=DE", Spotify.search(what("lofi beats"), "DE"))
        assertEquals(api + "search?q=bohemian%20rhapsody&type=track&limit=5&market=DE", Spotify.search(what("song bohemian rhapsody"), "DE"))
        assertEquals(api + "search?q=discovery&type=album&limit=5&market=DE", Spotify.search(what("album discovery"), "DE"))
        assertEquals(api + "search?q=daft%20punk&type=artist&limit=5&market=DE", Spotify.search(what("artist daft punk"), "DE"))
        assertEquals(api + "search?q=deep%20focus&type=playlist&limit=10&market=DE", Spotify.search(what("playlist deep focus"), "DE"))
        assertEquals(api + "search?q=jazz&type=playlist&limit=10&market=DE", Spotify.search(what("genre jazz"), "DE"))
    }

    @Test fun `by names the fields where a kind word says what it is`() {
        assertEquals(api + "search?q=track%3A%22bohemian%20rhapsody%22%20artist%3A%22queen%22&type=track&limit=5&market=DE", Spotify.search(what("song bohemian rhapsody by queen"), "DE"))
        assertEquals(api + "search?q=track%3A%22haus%20am%20see%22%20artist%3A%22peter%20fox%22&type=track&limit=5&market=DE", Spotify.search(what("lied haus am see von peter fox"), "DE"))
        assertEquals(api + "search?q=album%3A%22discovery%22%20artist%3A%22daft%20punk%22&type=album&limit=5&market=DE", Spotify.search(what("album discovery by daft punk"), "DE"))
        // And the same words as free text, for when the fields found nothing.
        assertEquals(api + "search?q=bohemian%20rhapsody%20queen&type=track&limit=5&market=DE", Spotify.second(what("song bohemian rhapsody by queen"), "DE"))
        assertEquals(api + "search?q=discovery%20daft%20punk&type=album&limit=5&market=DE", Spotify.second(what("album discovery by daft punk"), "DE"))
        assertNull(Spotify.second(what("song bohemian rhapsody"), "DE"))
        assertNull(Spotify.second(what("lofi beats"), "DE"))
        assertNull(Spotify.second(what("artist daft punk"), "DE"))
    }

    @Test fun `free text is searched by its words as they stand, by and all`() {
        // "Stand by Me" is not "stand" by "me": Spotify's own ranking is asked first.
        assertEquals(api + "search?q=stand%20by%20me&type=track,artist&limit=10&market=DE", Spotify.search(what("stand by me"), "DE"))
        assertEquals(api + "search?q=bohemian%20rhapsody%20by%20queen&type=track,artist&limit=10&market=DE", Spotify.search(what("bohemian rhapsody by queen"), "DE"))
        // The reading as a title and who it is by is the second search, for when the words found nothing.
        assertEquals(api + "search?q=track%3A%22bohemian%20rhapsody%22%20artist%3A%22queen%22&type=track&limit=5&market=DE", Spotify.second(what("bohemian rhapsody by queen"), "DE"))
        assertEquals(api + "search?q=track%3A%22stand%22%20artist%3A%22me%22&type=track&limit=5&market=DE", Spotify.second(what("stand by me"), "DE"))
        // What "Use this" puts into the field is read without kind words: a title that begins with one is searched as it stands.
        assertEquals(api + "search?q=Song%202%20Blur&type=track,artist&limit=10&market=DE", Spotify.search(Play.read("Song 2 Blur", kinds = false)!!, "DE"))
    }

    @Test fun `free text with by is found by its words, and by its fields only when they find nothing`() {
        val komet = Song(PlayMode.SONG, "Komet", "spotify:track:7oQepKHmXDaPC3rgeLRvQu", listOf("Udo Lindenberg", "Apache 207"), "Komet")
        val (a, urls) = lookup("komet by udo lindenberg", reply("search-track-by"))
        assertEquals(komet, a.song)
        assertEquals(listOf(api + "search?q=komet%20by%20udo%20lindenberg&type=track,artist&limit=10&market=DE"), urls)
        val (b, more) = lookup("komet von udo lindenberg", reply("search-track-none"), reply("search-track-by"))
        assertEquals(komet, b.song)
        assertEquals(listOf(api + "search?q=komet%20von%20udo%20lindenberg&type=track,artist&limit=10&market=DE", api + "search?q=track%3A%22komet%22%20artist%3A%22udo%20lindenberg%22&type=track&limit=5&market=DE"), more)
    }

    @Test fun `what is typed cannot end a field or an address`() {
        assertEquals(api + "search?q=track%3A%22say%20%20hi%20%20now%22%20artist%3A%22a%26b%22&type=track&limit=5&market=DE", Spotify.search(what("song say \"hi\" now by a&b"), "DE"))
        assertEquals(api + "search?q=a%26b%3Dc%20%23d%3Fe%2Ff&type=track,artist&limit=10&market=DE", Spotify.search(what("a&b=c #d?e/f"), "DE"))
        assertEquals(api + "search?q=caf%C3%A9%20del%20mar%20%F0%9F%8E%89&type=track,artist&limit=10&market=DE", Spotify.search(what("café del mar 🎉"), "DE"))
    }

    @Test fun `the market is a country or the widest catalogue`() {
        assertTrue(Spotify.search(what("x"), "de").endsWith("&market=DE"))
        assertTrue(Spotify.search(what("x"), " gb ").endsWith("&market=GB"))
        for (odd in listOf("", "D", "DEU", "d1", "&x", "419")) assertTrue(odd, Spotify.search(what("x"), odd).endsWith("&market=US"))
        assertEquals(api + "albums/2noRn2Aes5aoNVsU6iWThc/tracks?limit=1&market=US", Spotify.albumTracks("2noRn2Aes5aoNVsU6iWThc", ""))
        assertEquals(api + "search?q=artist%3A%22Daft%20Punk%22&type=track&limit=10&market=DE", Spotify.artistTracks("Daft Punk", "DE"))
    }

    @Test fun `the links that play`() {
        assertEquals("spotify:track:T", Spotify.track("T"))
        assertEquals("spotify:track:T?context=spotify%3Aalbum%3AA", Spotify.inAlbum("T", "A"))
        assertEquals("spotify:track:T?context=spotify%3Aartist%3AA", Spotify.byArtist("T", "A"))
        assertEquals("spotify:playlist:P:play", Spotify.playlist("P"))
    }

    @Test fun `the token`() {
        assertEquals("Basic aWQ6c2VjcmV0", Spotify.basic("id", "secret"))
        assertEquals("Basic aWQ6c2VjcmV0", Spotify.basic(" id\n", " secret "))
        assertEquals("https://accounts.spotify.com/api/token", Spotify.TOKEN)
        assertEquals("grant_type=client_credentials", Spotify.TOKEN_FORM)
        val t = Spotify.token(reply("token")).value!!
        assertEquals("written-by-hand-not-a-token", t.value)
        assertEquals(3600, t.seconds)
        // It is used until a minute before it ends.
        assertEquals(Duration.ofMinutes(59), Spotify.lasts(t))
        assertEquals(Duration.ZERO, Spotify.lasts(Spotify.Token("x", 30)))
    }

    @Test fun `a key that is not accepted`() {
        assertEquals(SongFailure.REFUSED, Spotify.token(reply("token-invalid-client", 400)).failure)
        assertNull(Spotify.token(reply("token-invalid-client", 400)).value)
        assertEquals(SongFailure.REFUSED, Spotify.token(reply("error-401", 401)).failure)
        assertEquals(SongFailure.REFUSED, Spotify.token(reply("artist-top-tracks-403", 403)).failure)
    }

    @Test fun `a token reply nobody can use`() {
        for (body in listOf("", "{}", "[]", "null", "<html>", """{"access_token":""}""", """{"access_token":"x"}""", """{"access_token":"x","expires_in":0}""", """{"access_token":"x","expires_in":"soon"}""",
            """{"access_token":7,"expires_in":3600}""", """{"error":"invalid_request"}""", """{"error":"unsupported_grant_type"}"""))
            assertEquals(body, SongFailure.NO_ANSWER, Spotify.token(Spotify.Fetched(200, body)).failure)
        assertEquals(SongFailure.NO_ANSWER, Spotify.token(Spotify.Fetched(500, "")).failure)
        assertEquals(SongFailure.NO_ANSWER, Spotify.token(Spotify.Fetched(503, reply("token").body)).failure)
        assertEquals(SongFailure.TOO_MANY, Spotify.token(Spotify.Fetched(429, "")).failure)
    }

    @Test fun `a song`() {
        val (a, urls) = lookup("song bohemian rhapsody", reply("search-track"))
        assertEquals(bohemian, a.song)
        assertNull(a.failure)
        assertEquals(listOf(api + "search?q=bohemian%20rhapsody&type=track&limit=5&market=DE"), urls)
    }

    @Test fun `a song is the first that can be played`() {
        fun song(id: String, more: String) = """{"id":"$id","name":"x","artists":[{"id":"A1","name":"A"}]$more}"""
        val list = listOf(song("T1", ""","is_playable":false"""), song("T2", ""","is_local":true"""), song("T3", ""","is_playable":true,"is_local":false"""), song("T4", ""))
        assertEquals("spotify:track:T3", lookup("song x", ok("""{"tracks":{"items":[${list.joinToString(",")}]}}""")).first.song!!.address)
        // Where a reply does not say, the song is taken as one that can be played.
        assertEquals("spotify:track:T4", lookup("song x", ok("""{"tracks":{"items":[${list[0]},${list[3]}]}}""")).first.song!!.address)
        assertEquals(SongFailure.NOT_FOUND, lookup("song x", ok("""{"tracks":{"items":[${list[0]},${list[1]}]}}""")).first.failure)
    }

    @Test fun `a song by its fields, and by its words when the fields find nothing`() {
        val komet = Song(PlayMode.SONG, "Komet", "spotify:track:7oQepKHmXDaPC3rgeLRvQu", listOf("Udo Lindenberg", "Apache 207"), "Komet")
        val (a, urls) = lookup("song komet by udo lindenberg", reply("search-track-by"))
        assertEquals(komet, a.song)
        assertEquals(listOf(api + "search?q=track%3A%22komet%22%20artist%3A%22udo%20lindenberg%22&type=track&limit=5&market=DE"), urls)
        val (b, more) = lookup("lied komet von udo lindenberg", reply("search-track-none"), reply("search-track-by"))
        assertEquals(komet, b.song)
        assertEquals(listOf(Spotify.search(what("song komet by udo lindenberg"), "DE"), api + "search?q=komet%20udo%20lindenberg&type=track&limit=5&market=DE"), more)
    }

    @Test fun `nothing found`() {
        val (a, urls) = lookup("song zzqqxxyyvvkkjj", reply("search-track-none"))
        assertNull(a.song)
        assertEquals(SongFailure.NOT_FOUND, a.failure)
        assertEquals(1, urls.size)
        // With "by": both searches found nothing.
        val (b, both) = lookup("zzzz by yyyy", reply("search-track-none"), reply("search-track-none"))
        assertEquals(SongFailure.NOT_FOUND, b.failure)
        assertEquals(2, both.size)
        // A reply without the list that was asked for is nothing found, too.
        assertEquals(SongFailure.NOT_FOUND, lookup("album zzzz", reply("search-track-none")).first.failure)
        assertEquals(SongFailure.NOT_FOUND, lookup("playlist zzzz", ok("{}")).first.failure)
        assertEquals(SongFailure.NOT_FOUND, lookup("zzzz", ok("""{"tracks":{"items":null},"artists":{"items":[null,7,"x",{}]}}""")).first.failure)
    }

    @Test fun `an album is played from its first song`() {
        val (a, urls) = lookup("album discovery by daft punk", reply("search-album"), reply("album-tracks"))
        assertEquals(Song(PlayMode.ALBUM, "Discovery", "spotify:track:0DiWol3AO6WpXZgp0goxAV?context=spotify%3Aalbum%3A2noRn2Aes5aoNVsU6iWThc", listOf("Daft Punk"), starts = "One More Time"), a.song)
        assertEquals(listOf(Spotify.search(what("album discovery by daft punk"), "DE"), api + "albums/2noRn2Aes5aoNVsU6iWThc/tracks?limit=1&market=DE"), urls)
    }

    @Test fun `an album whose songs are not to be had`() {
        assertEquals(SongFailure.NOT_FOUND, lookup("album discovery", reply("search-album"), ok("""{"items":[]}""")).first.failure)
        assertEquals(SongFailure.TOO_MANY, lookup("album discovery", reply("search-album"), Spotify.Fetched(429, """{"error":{"status":429,"message":"API rate limit exceeded"}}""")).first.failure)
        assertEquals(SongFailure.NO_ANSWER, lookup("album discovery", reply("search-album"), Spotify.Fetched(404, """{"error":{"status":404,"message":"Resource not found"}}""")).first.failure)
        // An album that cannot be played here is passed over.
        assertEquals(SongFailure.NOT_FOUND, lookup("album x", ok("""{"albums":{"items":[{"id":"A1","name":"X","is_playable":false}]}}""")).first.failure)
        // Found by its words when its fields found nothing: three requests.
        val (a, urls) = lookup("album discovery by daft punk", ok(nothing), reply("search-album"), reply("album-tracks"))
        assertEquals("Discovery", a.song!!.name)
        assertEquals(listOf(api + "search?q=album%3A%22discovery%22%20artist%3A%22daft%20punk%22&type=album&limit=5&market=DE", api + "search?q=discovery%20daft%20punk&type=album&limit=5&market=DE",
            api + "albums/2noRn2Aes5aoNVsU6iWThc/tracks?limit=1&market=DE"), urls)
    }

    @Test fun `an artist is played from the first song of their own`() {
        // The first song found for them is The Weeknd's, with them as guests; the second is theirs.
        val (a, urls) = lookup("artist daft punk", reply("search-artist"), ok(both(daftPunkSongs)))
        assertEquals(daftPunk, a.song)
        assertEquals(listOf(api + "search?q=daft%20punk&type=artist&limit=5&market=DE", api + "search?q=artist%3A%22Daft%20Punk%22&type=track&limit=10&market=DE"), urls)
        // Only songs they are guests on: nothing of theirs to start from. (This is the saved reply: asked for two songs, Spotify gave this one.)
        assertEquals(SongFailure.NOT_FOUND, lookup("artist daft punk", reply("search-artist"), reply("search-artist-tracks")).first.failure)
        assertEquals(SongFailure.NOT_FOUND, lookup("artist daft punk", reply("search-artist"), reply("search-track-none")).first.failure)
        assertEquals(SongFailure.NOT_FOUND, lookup("artist zzzz", ok(nothing)).first.failure)
        assertEquals(SongFailure.OFFLINE, Spotify.lookup(what("artist daft punk"), "DE") { url -> if ("artist%3A" in url) throw Spotify.Unreachable() else reply("search-artist") }.failure)
        // Their top tracks are never asked for: a key of this kind is refused them.
        assertTrue(urls.none { "top-tracks" in it })
    }

    @Test fun `the artist is the one of that name, else the first`() {
        // "Giorgio Moroder" is the second artist found for "daft punk".
        val songs = """{"tracks":{"items":[{"id":"T1","name":"Chase","artists":[{"id":"6jU2Tt13MmXYk0ZBv1KmfO","name":"Giorgio Moroder"}]}]}}"""
        assertEquals(Song(PlayMode.ARTIST, "Giorgio Moroder", "spotify:track:T1?context=spotify%3Aartist%3A6jU2Tt13MmXYk0ZBv1KmfO", starts = "Chase"), lookup("artist giorgio moroder", reply("search-artist"), ok(songs)).first.song)
        assertEquals(daftPunk, lookup("artist daft", reply("search-artist"), ok(both(daftPunkSongs))).first.song)
    }

    @Test fun `free text is an artist only when a song of their own was found for it`() {
        // Songs and artists at once: an artist of exactly that name, and among the songs one of their own. One request.
        val (a, urls) = lookup("daft punk", ok(both(daftPunkSongs, inner("search-artist"))))
        assertEquals(daftPunk, a.song)
        assertEquals(listOf(api + "search?q=daft%20punk&type=track,artist&limit=10&market=DE"), urls)
        assertEquals(daftPunk, lookup("DAFT  Punk", ok(both(daftPunkSongs, inner("search-artist")))).first.song)
        // None of the songs is their own: the first song is what was found.
        val starboy = Song(PlayMode.SONG, "Starboy", "spotify:track:7MXVkk9YMctZqd1Srtv4MB", listOf("The Weeknd", "Daft Punk"), "Starboy")
        assertEquals(starboy, lookup("daft punk", ok(both(inner("search-artist-tracks"), inner("search-artist")))).first.song)
        // Not the artist's whole name: the first song.
        assertEquals(starboy, lookup("daft", ok(both(daftPunkSongs, inner("search-artist")))).first.song)
        // A song's name, and no artist of that name: the song.
        assertEquals(bohemian, lookup("bohemian rhapsody", ok(both(inner("search-track"), inner("search-artist")))).first.song)
        assertEquals(bohemian, lookup("bohemian rhapsody", reply("search-track")).first.song)
    }

    @Test fun `the name of an artist with or without its the`() {
        val reply = """{"tracks":{"items":[{"id":"T1","name":"Here Comes The Sun","artists":[{"id":"B1","name":"The Beatles"}]}]},"artists":{"items":[{"id":"B1","name":"The Beatles"}]}}"""
        assertEquals(Song(PlayMode.ARTIST, "The Beatles", "spotify:track:T1?context=spotify%3Aartist%3AB1", starts = "Here Comes The Sun"), lookup("beatles", ok(reply)).first.song)
        assertEquals(PlayMode.ARTIST, lookup("the beatles", ok(reply)).first.song!!.kind)
        assertEquals(PlayMode.SONG, lookup("beatles sun", ok(reply)).first.song!!.kind)
    }

    @Test fun `free text that gets no answer for songs and artists at once is asked for as songs`() {
        val (a, urls) = lookup("bohemian rhapsody", Spotify.Fetched(400, """{"error":{"status":400,"message":"Bad request"}}"""), reply("search-track"))
        assertEquals(bohemian, a.song)
        assertEquals(listOf(api + "search?q=bohemian%20rhapsody&type=track,artist&limit=10&market=DE", api + "search?q=bohemian%20rhapsody&type=track&limit=5&market=DE"), urls)
        // Not when the key is refused or there is no connection: that would only be the same again.
        assertEquals(1, lookup("bohemian rhapsody", reply("error-401", 401)).second.size)
        assertEquals(1, lookup("song bohemian rhapsody", Spotify.Fetched(400, "")).second.size)
    }

    @Test fun `a playlist, past the ones that are not to be had`() {
        // Spotify's own playlists come back as null: the first that is there is somebody's.
        val (a, urls) = lookup("playlist deep focus", reply("search-playlist"))
        assertEquals(Song(PlayMode.PLAYLIST, "Deep Focus Music", "spotify:playlist:14KtkIpsvzDSCXR24EqHCL:play", owner = "Example Owner"), a.song)
        assertEquals(1, urls.size)
        // A genre is a playlist of that name.
        assertEquals("spotify:playlist:14KtkIpsvzDSCXR24EqHCL:play", lookup("genre deep focus", reply("search-playlist")).first.song!!.address)
        assertEquals(SongFailure.NOT_FOUND, lookup("playlist x", ok("""{"playlists":{"items":[null,null]}}""")).first.failure)
        assertEquals(Song(PlayMode.PLAYLIST, "X", "spotify:playlist:P1:play"), lookup("playlist x", ok("""{"playlists":{"items":[{"id":"P1","name":"X","owner":{"id":"u"}}]}}""")).first.song)
    }

    @Test fun `why there is no answer`() {
        val tooMany = """{"error":{"status":429,"message":"API rate limit exceeded"}}"""
        assertEquals(SongFailure.REFUSED, lookup("song x", reply("error-401", 401)).first.failure)
        assertEquals(SongFailure.REFUSED, lookup("song x", reply("artist-top-tracks-403", 403)).first.failure)
        assertEquals(SongFailure.TOO_MANY, lookup("song x", Spotify.Fetched(429, tooMany)).first.failure)
        // The status alone says it where the text does not.
        assertEquals(SongFailure.TOO_MANY, lookup("song x", Spotify.Fetched(429, "")).first.failure)
        assertEquals(SongFailure.REFUSED, lookup("song x", Spotify.Fetched(401, "<html>")).first.failure)
        assertEquals(SongFailure.NO_ANSWER, lookup("song x", Spotify.Fetched(500, "")).first.failure)
        assertEquals(SongFailure.NO_ANSWER, lookup("song x", Spotify.Fetched(400, """{"error":{"status":400,"message":"No search query"}}""")).first.failure)
        // And the text says it where the status does not.
        assertEquals(SongFailure.TOO_MANY, lookup("song x", ok(tooMany)).first.failure)
        assertEquals(SongFailure.NO_ANSWER, lookup("song x", ok("not json")).first.failure)
        assertEquals(SongFailure.NO_ANSWER, lookup("song x", ok("[]")).first.failure)
        assertEquals(SongFailure.OFFLINE, Spotify.lookup(what("x"), "DE") { throw Spotify.Unreachable() }.failure)
        assertEquals(SongFailure.NO_ANSWER, Spotify.lookup(what("x"), "DE") { throw IllegalStateException("anything") }.failure)
    }

    @Test fun `an id that is not one never becomes an address`() {
        for (id in listOf("a b", "a:b", "a?b", "a%3Ab", "../x", "", "x".repeat(65), "tra\u0000ck")) {
            val body = """{"tracks":{"items":[{"id":${quote(id)},"name":"X","artists":[{"id":"A1","name":"A"}]}]}}"""
            assertEquals(id, SongFailure.NOT_FOUND, lookup("song x", Spotify.Fetched(200, body)).first.failure)
            val list = """{"playlists":{"items":[{"id":${quote(id)},"name":"X"}]}}"""
            assertEquals(id, SongFailure.NOT_FOUND, lookup("playlist x", Spotify.Fetched(200, list)).first.failure)
        }
        // A number is not an id, and a song needs a name.
        assertEquals(SongFailure.NOT_FOUND, lookup("song x", Spotify.Fetched(200, """{"tracks":{"items":[{"id":7,"name":"X"},{"id":"T1"},{"id":"T2","name":" "}]}}""")).first.failure)
    }

    private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\u0000", "\\u0000") + "\""

    @Test fun `a song with nobody named and no album is still a song`() {
        val a = lookup("song x", Spotify.Fetched(200, """{"tracks":{"items":[{"id":"T1","name":"X"}]}}""")).first
        assertEquals(Song(PlayMode.SONG, "X", "spotify:track:T1"), a.song)
        val b = lookup("song x", Spotify.Fetched(200, """{"tracks":{"items":[{"id":"T1","name":"X","artists":[null,{"name":"A"},{"id":"B1"},7],"album":{"name":" "}}]}}""")).first
        assertEquals(Song(PlayMode.SONG, "X", "spotify:track:T1", listOf("A")), b.song)
    }

    @Test fun `how long an answer is kept`() {
        assertEquals(Duration.ofMinutes(5), Spotify.keep(null))
        assertEquals(Duration.ofMinutes(5), Spotify.keep(SongFailure.NOT_FOUND))
        assertEquals(Duration.ofMinutes(5), Spotify.keep(SongFailure.REFUSED))
        assertEquals(Duration.ofSeconds(30), Spotify.keep(SongFailure.TOO_MANY))
        assertEquals(Duration.ofSeconds(10), Spotify.keep(SongFailure.OFFLINE))
        assertEquals(Duration.ofSeconds(10), Spotify.keep(SongFailure.NO_ANSWER))
    }

    @Test fun `odd replies do not throw`() {
        val odd = listOf("", " ", "null", "true", "7", "\"x\"", "[", "{", "{\"tracks\":", "{\"tracks\":7}", "{\"tracks\":{\"items\":7}}", "{\"tracks\":{\"items\":[[],{},null,\"x\",{\"id\":{}}]}}",
            "{\"error\":7}", "{\"error\":{}}", "{\"error\":{\"status\":\"x\"}}", "{\"error\":null}", "[".repeat(100), "{\"a\":".repeat(100), "\uD83D", "\u0000")
        for (body in odd) for (text in listOf("x", "song x", "album x by y", "artist x", "playlist x", "genre x")) {
            Spotify.lookup(what(text), "DE") { Spotify.Fetched(200, body) }
            Spotify.lookup(what(text), "DE") { Spotify.Fetched(403, body) }
            Spotify.token(Spotify.Fetched(200, body))
        }
    }

    /** Spotify always finds something. What it finds for a name it does not know has nothing to do with it, and is not played. */
    @Test fun whatHasNothingToDoWithTheTextIsNotFound() {
        // The reply for "bohemian rhapsody", given for other texts: as Spotify gives its popular songs for a text it does not know.
        assertEquals(SongFailure.NOT_FOUND, lookup("zzqqxxyyvvkk", reply("search-track")).first.failure)
        assertEquals(SongFailure.NOT_FOUND, lookup("that song about the poor boy", reply("search-track")).first.failure)
        // A slip of the fingers is still the song; so is a part of its name, or only who it is by.
        assertEquals("Bohemian Rhapsody", lookup("bohemian rapsody", reply("search-track")).first.song?.name)
        assertEquals("Bohemian Rhapsody", lookup("rhapsody", reply("search-track")).first.song?.name)
        assertEquals("Bohemian Rhapsody", lookup("that quen song", reply("search-track")).first.song?.name)
        assertEquals("Bohemian Rhapsody", lookup("song bohemian rhapsody by queen", reply("search-track")).first.song?.name)
    }

    @Test fun wordsThatAreAlike() {
        assertTrue(Spotify.alike("rhapsody", "rapsody"))
        assertTrue(Spotify.alike("queen", "quen"))
        assertTrue(Spotify.alike("luft", "luftballons"))
        assertFalse(Spotify.alike("boy", "bay"))
        assertFalse(Spotify.alike("poor", "pool"))
        assertFalse(Spotify.alike("heartbeat", "rhapsody"))
    }
}
