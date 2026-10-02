package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayTest {
    private val players = listOf("Spotify", "YouTube Music", "YouTube")
    private fun r(text: String, names: List<String> = players) = Play.read(text, names)!!

    @Test fun noTextIsNothingToPlay() {
        assertNull(Play.read(""))
        assertNull(Play.read("   "))
        assertNull(Play.read("\t\n", players))
    }

    @Test fun freeText() {
        assertEquals(PlayRequest(PlayMode.ANY, "lofi beats"), r("lofi beats"))
        assertEquals("lofi beats", r("lofi beats").name)
        assertEquals(PlayRequest(PlayMode.ANY, "x"), r("x"))
    }

    @Test fun aKindWordAtTheStart() {
        assertEquals(PlayRequest(PlayMode.SONG, "bohemian rhapsody", title = "bohemian rhapsody"), r("song bohemian rhapsody"))
        assertEquals(PlayRequest(PlayMode.SONG, "one more time", title = "one more time"), r("track one more time"))
        assertEquals(PlayRequest(PlayMode.ALBUM, "discovery", album = "discovery"), r("album discovery"))
        assertEquals(PlayRequest(PlayMode.ARTIST, "daft punk", artist = "daft punk"), r("artist daft punk"))
        assertEquals(PlayRequest(PlayMode.PLAYLIST, "focus", playlist = "focus"), r("playlist focus"))
        assertEquals(PlayRequest(PlayMode.GENRE, "jazz", genre = "jazz"), r("genre jazz"))
    }

    @Test fun theGermanKindWords() {
        assertEquals(PlayRequest(PlayMode.SONG, "haus am see", title = "haus am see"), r("lied haus am see"))
        assertEquals(PlayRequest(PlayMode.SONG, "haus am see", title = "haus am see"), r("titel haus am see"))
        assertEquals(PlayRequest(PlayMode.ARTIST, "peter fox", artist = "peter fox"), r("künstler peter fox"))
        assertEquals(PlayRequest(PlayMode.ARTIST, "peter fox", artist = "peter fox"), r("kuenstler peter fox"))
        assertEquals(PlayRequest(PlayMode.ARTIST, "nina hagen", artist = "nina hagen"), r("Künstlerin nina hagen"))
        assertEquals(PlayRequest(PlayMode.ARTIST, "peter fox", artist = "peter fox"), r("interpret peter fox"))
        assertEquals(PlayRequest(PlayMode.ALBUM, "stadtaffe", album = "stadtaffe"), r("album stadtaffe"))
        assertEquals(PlayRequest(PlayMode.PLAYLIST, "fokus", playlist = "fokus"), r("playlist fokus"))
        assertEquals(PlayRequest(PlayMode.GENRE, "schlager", genre = "schlager"), r("genre schlager"))
    }

    @Test fun aKindWordAloneIsTheMusicItself() {
        assertEquals(PlayRequest(PlayMode.ANY, "album"), r("album"))
        assertEquals(PlayRequest(PlayMode.ANY, "song"), r(" song "))
        assertEquals(PlayRequest(PlayMode.ANY, "genre"), r("genre"))
        // Before a player, too: nothing of that kind is named.
        assertEquals(PlayRequest(PlayMode.ANY, "album", player = 0), r("album on spotify"))
    }

    @Test fun aKindWordOnlyCountsAtTheStart() {
        assertEquals(PlayRequest(PlayMode.ANY, "the album leaf"), r("the album leaf"))
        assertEquals(PlayRequest(PlayMode.ANY, "my playlist"), r("my playlist"))
        // At the start it always counts: a title that begins with one ("Song 2") is typed after its artist, "blur song 2".
        assertEquals(PlayRequest(PlayMode.SONG, "2", title = "2"), r("song 2"))
        assertEquals(PlayRequest(PlayMode.ANY, "blur song 2"), r("blur song 2"))
    }

    @Test fun byNamesTheArtist() {
        assertEquals(PlayRequest(PlayMode.SONG, "bohemian rhapsody by queen", title = "bohemian rhapsody", artist = "queen"), r("song bohemian rhapsody by queen"))
        assertEquals(PlayRequest(PlayMode.ALBUM, "discovery by daft punk", album = "discovery", artist = "daft punk"), r("album discovery by daft punk"))
        assertEquals(PlayRequest(PlayMode.SONG, "haus am see von peter fox", title = "haus am see", artist = "peter fox"), r("lied haus am see von peter fox"))
        assertEquals(PlayRequest(PlayMode.ALBUM, "stadtaffe von peter fox", album = "stadtaffe", artist = "peter fox"), r("album stadtaffe von peter fox"))
        assertEquals("discovery", r("album discovery by daft punk").name)
    }

    @Test fun freeTextWithByStaysFreeText() {
        // Nothing is claimed for it: the title and who it is by are a second reading, and the name is all of it.
        assertEquals(PlayRequest(PlayMode.ANY, "bohemian rhapsody by queen", title = "bohemian rhapsody", artist = "queen"), r("bohemian rhapsody by queen"))
        assertEquals(PlayRequest(PlayMode.ANY, "haus am see von peter fox", title = "haus am see", artist = "peter fox"), r("haus am see von peter fox"))
        assertEquals(PlayRequest(PlayMode.ANY, "stand by me", title = "stand", artist = "me"), r("stand by me"))
        assertEquals("stand by me", r("stand by me").name)
        assertEquals(PlayRequest(PlayMode.ANY, "stand by me", title = "stand", artist = "me", player = 0), r("stand by me on spotify"))
    }

    @Test fun theLastByIsTheOne() {
        assertEquals(PlayRequest(PlayMode.ANY, "stand by me by ben e. king", title = "stand by me", artist = "ben e. king"), r("stand by me by ben e. king"))
        assertEquals(PlayRequest(PlayMode.SONG, "stand by me by ben e. king", title = "stand by me", artist = "ben e. king"), r("song stand by me by ben e. king"))
    }

    @Test fun aNameAsItStandsHasNoKindWord() {
        // What the model named and "Use this" put into the field: its first word is part of the name.
        assertEquals(PlayRequest(PlayMode.ANY, "Song 2 Blur"), Play.read("Song 2 Blur", players, kinds = false))
        assertEquals(PlayRequest(PlayMode.ANY, "Album of the Year Bruno Major", player = 0), Play.read("Album of the Year Bruno Major auf Spotify", players, kinds = false))
        assertEquals(PlayRequest(PlayMode.ANY, "Stand by Me Ben E. King", title = "Stand", artist = "Me Ben E. King"), Play.read("Stand by Me Ben E. King", players, kinds = false))
        // The same text typed by hand is read as ever.
        assertEquals(PlayRequest(PlayMode.SONG, "2 Blur", title = "2 Blur"), r("Song 2 Blur"))
    }

    @Test fun byWithNoArtistAfterItIsPartOfTheTitle() {
        assertEquals(PlayRequest(PlayMode.ANY, "stand by"), r("stand by"))
        assertEquals(PlayRequest(PlayMode.SONG, "stand by", title = "stand by"), r("song stand by"))
        assertEquals(PlayRequest(PlayMode.ALBUM, "passing by", album = "passing by", player = 0), r("album passing by on spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "stand by", player = 0), r("stand by on spotify"))
    }

    @Test fun byWithNothingBeforeItIsPartOfTheTitle() {
        assertEquals(PlayRequest(PlayMode.ANY, "by the way"), r("by the way"))
        assertEquals(PlayRequest(PlayMode.SONG, "by the way", title = "by the way"), r("song by the way"))
        assertEquals(PlayRequest(PlayMode.ANY, "by"), r("by"))
        assertEquals(PlayRequest(PlayMode.ANY, "von"), r("von"))
    }

    @Test fun theQueryKeepsWhatWasTyped() {
        // A player that only searches is given the words as they were typed: "stand by me" is found by all three.
        assertEquals("stand by me", r("stand by me").query)
        assertEquals("stand by me", r("song stand by me on spotify").query)
    }

    @Test fun anArtistAPlaylistAndAGenreAreNotByAnybody() {
        assertEquals(PlayRequest(PlayMode.ARTIST, "herbert von karajan", artist = "herbert von karajan"), r("artist herbert von karajan"))
        assertEquals(PlayRequest(PlayMode.PLAYLIST, "songs by the sea", playlist = "songs by the sea"), r("playlist songs by the sea"))
        assertEquals(PlayRequest(PlayMode.GENRE, "drum by bass", genre = "drum by bass"), r("genre drum by bass"))
    }

    @Test fun aPlayerAtTheEnd() {
        assertEquals(PlayRequest(PlayMode.ANY, "bohemian rhapsody", player = 0), r("bohemian rhapsody on spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "bohemian rhapsody", player = 0), r("bohemian rhapsody in spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "bohemian rhapsody", player = 0), r("bohemian rhapsody auf spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "lofi", player = 1), r("lofi on youtube music"))
        assertEquals(PlayRequest(PlayMode.ALBUM, "discovery by daft punk", album = "discovery", artist = "daft punk", player = 0), r("album discovery by daft punk on spotify"))
        assertEquals(PlayRequest(PlayMode.SONG, "haus am see von peter fox", title = "haus am see", artist = "peter fox", player = 0), r("lied haus am see von peter fox auf spotify"))
        assertEquals(PlayRequest(PlayMode.PLAYLIST, "focus", playlist = "focus", player = 1), r("playlist focus in youtube music"))
    }

    @Test fun thePlayerIsNamedFromItsStart() {
        assertEquals(0, r("lofi on spot").player)
        assertEquals(0, r("lofi on sp").player)
        // One letter is not a name yet.
        assertEquals(PlayRequest(PlayMode.ANY, "lofi on s"), r("lofi on s"))
        // Of two that begin that way, the shorter name; with more of the longer one typed, that one.
        assertEquals(2, r("lofi on you").player)
        assertEquals(2, r("lofi on youtube").player)
        assertEquals(1, r("lofi on youtube m").player)
        // Not from the middle of a name: "Lost in Music" is a title.
        assertEquals(PlayRequest(PlayMode.ANY, "lost in music"), r("lost in music"))
    }

    @Test fun onlyAPlayersNameAfterOnCounts() {
        assertEquals(PlayRequest(PlayMode.ANY, "smoke on the water"), r("smoke on the water"))
        assertEquals(PlayRequest(PlayMode.ANY, "smoke on the water", player = 0), r("smoke on the water on spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "walking on sunshine"), r("walking on sunshine"))
        assertEquals(PlayRequest(PlayMode.ANY, "man in the mirror", player = 0), r("man in the mirror in spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "auf uns"), r("auf uns"))
        // A player that is called that is a player.
        assertEquals(PlayRequest(PlayMode.ANY, "smoke", player = 1), r("smoke on the water", listOf("Spotify", "The Water")))
    }

    @Test fun withNoPlayersNothingIsAPlayer() {
        assertEquals(PlayRequest(PlayMode.ANY, "bohemian rhapsody on spotify"), r("bohemian rhapsody on spotify", emptyList()))
        assertEquals(PlayRequest(PlayMode.ANY, "bohemian rhapsody on spotify"), Play.read("bohemian rhapsody on spotify"))
    }

    @Test fun aPlayerWithNothingBeforeItIsText() {
        assertEquals(PlayRequest(PlayMode.ANY, "on spotify"), r("on spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "spotify"), r("spotify"))
        assertEquals(PlayRequest(PlayMode.ANY, "lofi on"), r("lofi on"))
    }

    @Test fun aPlayerWhoseNameHoldsSuchAWord() {
        val names = listOf("Player", "Plug in Player")
        assertEquals(PlayRequest(PlayMode.ANY, "lofi", player = 1), r("lofi on plug in player", names))
        assertEquals(PlayRequest(PlayMode.ANY, "lofi", player = 0), r("lofi on player", names))
    }

    @Test fun upperAndLowerCase() {
        assertEquals(PlayRequest(PlayMode.ALBUM, "Discovery BY Daft Punk", album = "Discovery", artist = "Daft Punk", player = 0), r("ALBUM Discovery BY Daft Punk ON Spotify"))
        assertEquals(PlayRequest(PlayMode.ARTIST, "Daft Punk", artist = "Daft Punk", player = 1), r("Artist Daft Punk In YOUTUBE MUSIC"))
        assertEquals(PlayRequest(PlayMode.SONG, "Haus am See Von Peter Fox", title = "Haus am See", artist = "Peter Fox", player = 0), r("Lied Haus am See Von Peter Fox Auf SPOTIFY"))
    }

    @Test fun extraSpaces() {
        assertEquals(PlayRequest(PlayMode.ALBUM, "dark side by pink floyd", album = "dark side", artist = "pink floyd", player = 0), r("  album   dark  side   by  pink floyd   on   spotify  "))
        assertEquals(PlayRequest(PlayMode.ANY, "lofi beats"), r("\tlofi \t beats\n"))
    }

    @Test fun punctuationAroundAWord() {
        // Folded like every name: "Spotify!" is Spotify, and a hyphen alone is nobody.
        assertEquals(0, r("lofi on spotify!").player)
        assertEquals(PlayRequest(PlayMode.ANY, "lofi on -"), r("lofi on -"))
    }

    @Test fun theQuestionForTheModel() {
        assertEquals("Which song is meant? Reply with only the title, then a slash, then the artist.\n\nthat queen song about the poor boy", Play.question("  that queen song about the poor boy "))
    }

    @Test fun theModelsReplyAsTextToPlay() {
        assertEquals("Bohemian Rhapsody Queen", Play.named("Bohemian Rhapsody / Queen"))
        assertEquals("Bohemian Rhapsody Queen", Play.named("Bohemian Rhapsody/Queen"))
        assertEquals("Bohemian Rhapsody Queen", Play.named("\n  \"Bohemian Rhapsody\" / Queen.\n\nIt is from 1975."))
        assertEquals("Bohemian Rhapsody Queen", Play.named("“Bohemian Rhapsody” / Queen"))
        assertEquals("Bohemian Rhapsody Queen", Play.named("**Bohemian Rhapsody** / *Queen*"))
        assertEquals("Bohemian Rhapsody Queen", Play.named("Title: Bohemian Rhapsody / Artist: Queen"))
        assertEquals("Haus am See Peter Fox", Play.named("Titel: Haus am See / Interpret: Peter Fox"))
        // No slash: the line as it is. More than one: the title and the artist, no more.
        assertEquals("Bohemian Rhapsody by Queen", Play.named("Bohemian Rhapsody by Queen"))
        assertEquals("Bohemian Rhapsody Queen", Play.named("Bohemian Rhapsody / Queen / 1975"))
        assertEquals("Queen", Play.named(" / Queen"))
        assertEquals("AC DC", Play.named("AC/DC"))
        // A slash with spaces round it divides the two; one inside a name stays there.
        assertEquals("Back in Black AC/DC", Play.named("Back in Black / AC/DC"))
    }

    @Test fun theTitleAndTheArtistOfAReply() {
        assertEquals(listOf("Bohemian Rhapsody", "Queen"), Play.song("\"Bohemian Rhapsody\" / Queen.\nFrom 1975."))
        assertEquals(listOf("Bohemian Rhapsody", "Queen"), Play.song("Title: Bohemian Rhapsody/Artist: Queen"))
        assertEquals(listOf("Back in Black", "AC/DC"), Play.song("Back in Black / AC/DC"))
        assertEquals(listOf("Bohemian Rhapsody", "Queen"), Play.song("Bohemian Rhapsody / Queen / 1975"))
        assertEquals(listOf("Bohemian Rhapsody by Queen"), Play.song("Bohemian Rhapsody by Queen"))
        assertEquals(emptyList<String>(), Play.song(" \n"))
        assertEquals(emptyList<String>(), Play.song("\"\" / ..."))
    }

    @Test fun aReplyThatNamesNothing() {
        assertNull(Play.named(""))
        assertNull(Play.named(" \n\t"))
        assertNull(Play.named("/"))
        assertNull(Play.named("\"\" / ..."))
        assertNull(Play.named("***"))
        assertNull(Play.named("x".repeat(121)))
        for (s in listOf("\u0000", "\uD83D", "🎉 / 🎉", "/".repeat(10_000), "a/".repeat(10_000), "x ".repeat(50_000))) Play.named(s)
    }

    @Test fun oddInputDoesNotThrow() {
        val odd = listOf("\u0000", "\uD83D", "🎉", "👨‍👩‍👧‍👦 family", "​‍﻿", "مرحبا بالعالم", "日本語 のテキスト", "by", "on", "in", "on on on", "by by by", "album by", "album on",
            "x".repeat(100_000), "x ".repeat(50_000), "in ".repeat(5_000), "by ".repeat(5_000), "on spotify ".repeat(2_000))
        for (s in odd) { Play.read(s); Play.read(s, players); Play.read("album $s on spotify", players); Play.read(s, listOf("", " ", "🎉", s)) }
    }

    // ---- which music app `play` goes to

    @Test fun playGoesToTheAppPlayedInLast() {
        val both = listOf("music" to true, "spotify" to true)
        assertEquals(1, Play.door(both, "spotify"))
        assertEquals(0, Play.door(both, "music"))
    }

    @Test fun theFirstByNameTheFirstTime() {
        assertEquals(0, Play.door(listOf("music" to true, "spotify" to true), null))
        // And where the one played in last is gone.
        assertEquals(0, Play.door(listOf("music" to true, "spotify" to true), "deezer"))
        assertEquals(-1, Play.door(emptyList(), "spotify"))
    }

    @Test fun anAppThatOnlySearchesGivesWayToOneThatPlays() {
        // Spotify without a key only shows its results: another player that plays what is named is the one.
        assertEquals(0, Play.door(listOf("music" to true, "spotify" to false), "spotify"))
        assertEquals(1, Play.door(listOf("a" to false, "b" to true), null))
        // Where none plays, the one asked last, else the first: its chip then searches.
        assertEquals(1, Play.door(listOf("a" to false, "spotify" to false), "spotify"))
        assertEquals(0, Play.door(listOf("spotify" to false), null))
    }
}
