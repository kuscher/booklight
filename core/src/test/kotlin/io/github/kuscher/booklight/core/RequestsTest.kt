package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestsTest {
    private val spotify = "com.spotify.music"
    private fun ok(read: Read): Request = (read as? Read.Ok)?.request ?: throw AssertionError("not read: $read")
    private fun bad(read: Read): Problem = (read as? Read.Bad)?.problem ?: throw AssertionError("read: $read")
    private fun text(name: String, value: String) = Extra(name, ExtraKind.TEXT, value)
    private fun number(name: String, value: String) = Extra(name, ExtraKind.NUMBER, value)
    private fun yes(name: String, value: Boolean) = Extra(name, ExtraKind.YES_NO, value.toString())

    // ---- as an address: what Android's Intent.toUri writes for the same request

    @Test fun aLinkToAnApp() {
        assertEquals("intent:search:{argument}#Intent;scheme=spotify;package=com.spotify.music;end", Requests.write(Request(spotify, What.LINK, address = "spotify:search:{argument}")))
        assertEquals("intent://play.google.com/store/search?q={argument}&c=apps#Intent;scheme=https;package=com.android.vending;end",
            Requests.write(Request("com.android.vending", What.LINK, address = "https://play.google.com/store/search?q={argument}&c=apps")))
        assertEquals("intent:track:1#Intent;scheme=spotify;package=com.spotify.music;end", Requests.write(Request(spotify, What.LINK, address = " Spotify:track:1 ")))
    }

    @Test fun textToSend() {
        assertEquals("intent:#Intent;action=android.intent.action.SEND;type=text/plain;package=com.example.notes;S.android.intent.extra.TEXT={argument};end",
            Requests.write(Request("com.example.notes", What.SEND, text = "{argument}")))
        assertEquals("intent:#Intent;action=android.intent.action.SEND;type=text/plain;package=com.example.notes;S.android.intent.extra.TEXT=On%20my%20way%3B%20100%25%20%23late;end",
            Requests.write(Request("com.example.notes", What.SEND, text = "On my way; 100% #late")))
    }

    @Test fun aSearchAndPlayFromSearch() {
        assertEquals("intent:#Intent;action=android.intent.action.SEARCH;package=com.spotify.music;S.query={argument};end", Requests.write(Request(spotify, What.SEARCH, text = "{argument}")))
        assertEquals("intent:#Intent;action=android.media.action.MEDIA_PLAY_FROM_SEARCH;package=com.example.pod;S.android.intent.extra.focus=vnd.android.cursor.item%2F*;S.query={argument};end",
            Requests.write(Request("com.example.pod", What.PLAY, text = "{argument}")))
        // The user says what kind of thing to play: theirs is the only one.
        assertEquals("intent:#Intent;action=android.media.action.MEDIA_PLAY_FROM_SEARCH;package=com.example.pod;S.query=the%20daily;S.android.intent.extra.focus=vnd.android.cursor.item%2Fartist;end",
            Requests.write(Request("com.example.pod", What.PLAY, text = "the daily", extras = listOf(text(Requests.FOCUS, "vnd.android.cursor.item/artist")))))
    }

    @Test fun aCustomAction() {
        assertEquals("intent:#Intent;action=com.example.SHOW;package=com.example.app;end", Requests.write(Request("com.example.app", What.CUSTOM, action = "com.example.SHOW")))
        assertEquals("intent://item/7#Intent;scheme=example;action=com.example.SHOW;type=image/png;package=com.example.app;component=com.example.app/com.example.app.ui.Show%24Inner;i.count=3;l.at=1790000000000;B.fresh=true;S.note=a%20b;end",
            Requests.write(Request("com.example.app", What.CUSTOM, action = "com.example.SHOW", address = "example://item/7", activity = "com.example.app.ui.Show\$Inner", type = "image/png",
                extras = listOf(number("count", "3"), number("at", "1790000000000"), yes("fresh", true), text("note", "a b")))))
        // Only an activity: the action is the one every app's entrance has. An address without an action is one to view.
        assertEquals("intent:#Intent;action=android.intent.action.MAIN;package=com.example.app;component=com.example.app/com.example.app.Main;end",
            Requests.write(Request("com.example.app", What.CUSTOM, activity = "com.example.app.Main")))
        assertEquals("intent:thing#Intent;scheme=example;package=com.example.app;component=com.example.app/com.example.app.Main;end",
            Requests.write(Request("com.example.app", What.CUSTOM, address = "example:thing", activity = "com.example.app.Main")))
    }

    @Test fun onlyWhatTheKindUsesIsWritten() {
        val stale = Request(spotify, What.SEARCH, action = "x.Y", address = "spotify:track:1", text = "lofi", activity = "a.B", type = "text/plain")
        assertEquals("intent:#Intent;action=android.intent.action.SEARCH;package=com.spotify.music;S.query=lofi;end", Requests.write(stale))
        assertFalse(Requests.takesArgument(Request(spotify, What.SEARCH, address = "spotify:search:{argument}", text = "lofi")))
        assertTrue(Requests.takesArgument(Request(spotify, What.LINK, address = "spotify:search:{argument}", text = "lofi")))
        assertTrue(Requests.takesArgument(Request(spotify, What.CUSTOM, action = "x.Y", extras = listOf(text("q", "say {argument}")))))
        assertFalse(Requests.takesArgument(Request(spotify, What.LINK, address = "spotify:track:1")))
    }

    @Test fun whatWasWrittenIsReadBack() {
        val all = listOf(
            Request(spotify, What.LINK, address = "spotify:search:{argument}"),
            Request(spotify, What.LINK, address = "https://open.spotify.com/search/{argument}?a=1&b=2#top", extras = listOf(yes("android.intent.extra.START_PLAYBACK", true))),
            Request("com.example.notes", What.SEND, text = "Say “{argument}” ; = % # é 🎉", extras = listOf(text("android.intent.extra.SUBJECT", "Note"))),
            Request(spotify, What.SEARCH, text = "{argument}"),
            Request("com.example.pod", What.PLAY, text = "{argument}"),
            Request("com.example.pod", What.PLAY, text = "x", extras = listOf(text(Requests.FOCUS, "vnd.android.cursor.item/artist"), text("android.intent.extra.artist", "{argument}"))),
            Request("com.example.app", What.CUSTOM, action = "com.example.SHOW", address = "example://item/{argument}", activity = "com.example.app.ui.Show", type = "image/png",
                extras = listOf(number("count", "{argument}"), number("big", "1790000000000"), number("minus", "-4"), yes("fresh", false), text("S.odd;name=", "v"))),
            Request("com.example.app", What.CUSTOM, action = "android.intent.action.VIEW"),
            Request("com.example.app", What.CUSTOM, action = "android.intent.action.SEND", type = "text/plain"),
        )
        for (r in all) {
            assertNull("$r", Requests.why(r))
            assertEquals(r, ok(Requests.read(Requests.write(r))))
        }
    }

    @Test fun aCustomActionThatIsAPlainKindIsShownAsThatKind() {
        assertEquals(Request(spotify, What.LINK, address = "spotify:track:1"), ok(Requests.read(Requests.write(Request(spotify, What.CUSTOM, action = Requests.VIEW, address = "spotify:track:1")))))
        assertEquals(Request(spotify, What.SEARCH, text = "lofi"), ok(Requests.read(Requests.write(Request(spotify, What.CUSTOM, action = Requests.SEARCH, extras = listOf(text("query", "lofi")))))))
        // With a number where the words go it is no search.
        assertEquals(What.CUSTOM, ok(Requests.read("intent:#Intent;action=android.intent.action.SEARCH;package=a.b;i.query=3;end")).what)
    }

    // ---- an address read as Android reads it

    @Test fun anAddressIsReadAsAndroidReadsIt() {
        assertEquals(Request(spotify, What.LINK, address = "spotify:track:1"), ok(Requests.read("intent:track:1#Intent;scheme=spotify;package=com.spotify.music;end")))
        // The fields come after the last #; one in the link belongs to the link.
        assertEquals(Request("a.b", What.LINK, address = "https://example.com/a#Intent;component=x.y/z;end"), ok(Requests.read("intent://example.com/a#Intent;component=x.y/z;end#Intent;scheme=https;package=a.b;end")))
        assertEquals(Request("a.b", What.CUSTOM, action = "x.Y", activity = "a.b.Main"), ok(Requests.read("intent:#Intent;action=x.Y;component=a.b/.Main;end")))
        assertEquals(Request("a.b", What.CUSTOM, action = "x.Y", extras = listOf(text("k", "a;b=c"), number("n", "7"), number("m", "3000000000"), yes("on", true))),
            ok(Requests.read("  intent:#Intent;action=x.Y;package=a.b;S.k=a%3Bb%3Dc;i.n=7;l.m=3000000000;B.on=TRUE;end ")))
        // Without an action it is one to view.
        assertEquals(Request("a.b", What.CUSTOM, action = Requests.VIEW), ok(Requests.read("intent:#Intent;package=a.b;end")))
        assertEquals(Request("", What.LINK, address = "spotify:track:1"), ok(Requests.read("intent:track:1#Intent;scheme=spotify;end")))
        // A scheme with nothing after it is still the link Android would make of it: the form then says what it lacks.
        assertEquals(Request("a.b", What.LINK, address = "tel:"), ok(Requests.read("intent:#Intent;scheme=tel;package=a.b;end")))
        assertEquals(Problem.Kind.NO_ADDRESS, Requests.why(ok(Requests.read("intent:#Intent;scheme=tel;package=a.b;end")))?.kind)
    }

    @Test fun whatTheFormCannotHoldIsNamed() {
        assertEquals(Problem(Problem.Kind.FIELD, "category"), bad(Requests.read("intent:#Intent;action=x.Y;category=android.intent.category.DEFAULT;package=a.b;end")))
        assertEquals(Problem(Problem.Kind.FIELD, "launchFlags"), bad(Requests.read("intent:#Intent;launchFlags=0x10000000;package=a.b;end")))
        assertEquals(Problem(Problem.Kind.FIELD, "SEL"), bad(Requests.read("intent:#Intent;action=x.Y;SEL;package=a.b;end")))
        assertEquals(Problem(Problem.Kind.EXTRA_TYPE, "d."), bad(Requests.read("intent:#Intent;package=a.b;d.ratio=1.5;end")))
        assertEquals(Problem(Problem.Kind.EXTRA_TYPE, "f."), bad(Requests.read("intent:#Intent;package=a.b;f.ratio=1.5;end")))
        assertEquals(Problem(Problem.Kind.YES_NO, "maybe"), bad(Requests.read("intent:#Intent;package=a.b;B.on=maybe;end")))
        // Android would read these two as no; `am` reads them as yes. In an address they are named.
        assertEquals(Problem(Problem.Kind.YES_NO, "1"), bad(Requests.read("intent:#Intent;package=a.b;B.on=1;end")))
        assertEquals(Problem(Problem.Kind.YES_NO, "t"), bad(Requests.read("intent:#Intent;package=a.b;B.on=t;end")))
        // A long number the form would write back as a plain one: the app that asks for a long one would not get it.
        assertEquals(Problem(Problem.Kind.EXTRA_TYPE, "l."), bad(Requests.read("intent:#Intent;package=a.b;l.id=5;end")))
        assertEquals(Problem(Problem.Kind.EXTRA_TYPE, "l."), bad(Requests.read("intent:#Intent;package=a.b;l.id={argument};end")))
        assertEquals(Problem(Problem.Kind.TWO_APPS), bad(Requests.read("intent:#Intent;package=a.b;component=c.d/.Main;end")))
        assertEquals(Problem(Problem.Kind.COMPONENT, "Main"), bad(Requests.read("intent:#Intent;component=Main;end")))
        assertEquals(Problem(Problem.Kind.UNFINISHED), bad(Requests.read("intent:#Intent;package=a.b")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.read("intent:track:1")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.read("spotify:track:1#Intent;package=a.b;end")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.read("")))
        assertEquals(Problem(Problem.Kind.TOO_MANY), bad(Requests.read("intent:#Intent;action=x.Y;package=a.b;" + (1..9).joinToString("") { "S.k$it=v;" } + "end")))
        assertEquals(8, ok(Requests.read("intent:#Intent;action=x.Y;package=a.b;" + (1..8).joinToString("") { "S.k$it=v;" } + "end")).extras.size)
    }

    // ---- the typed text

    @Test fun theTypedTextGoesWhereTheCommandSaysArgument() {
        assertEquals("intent:search:daft%20punk#Intent;scheme=spotify;package=com.spotify.music;end", Requests.fill("intent:search:{argument}#Intent;scheme=spotify;package=com.spotify.music;end", "daft punk"))
        assertEquals("intent:#Intent;action=android.intent.action.SEARCH;package=com.spotify.music;S.query=daft%20punk;end",
            Requests.fill(Requests.write(Request(spotify, What.SEARCH, text = "{argument}")), "daft punk"))
        // In the link it is encoded for an address; in a text and an extra it is what was typed.
        val both = Requests.write(Request("a.b", What.LINK, address = "example://find?q={argument}", extras = listOf(text("said", "“{argument}”"), number("n", "{argument}"))))
        assertEquals(Request("a.b", What.LINK, address = "example://find?q=7", extras = listOf(text("said", "“7”"), number("n", "7"))), ok(Requests.read(Requests.fill(both, "7")!!)))
        val send = Requests.write(Request("a.b", What.SEND, text = "{argument}"))
        assertEquals(Request("a.b", What.SEND, text = "a/b?c=d#e;S.x=1;end & {argument} 100% é"), ok(Requests.read(Requests.fill(send, "a/b?c=d#e;S.x=1;end & {argument} 100% é")!!)))
        assertEquals("example://find?q=a%2Fb%3Fc%23d%3Be", ok(Requests.read(Requests.fill(Requests.write(Request("a.b", What.LINK, address = "example://find?q={argument}")), "a/b?c#d;e")!!)).address)
    }

    @Test fun textThatDoesNotFitFillsNothing() {
        val counted = Requests.write(Request("a.b", What.CUSTOM, action = "x.Y", extras = listOf(number("n", "{argument}"))))
        assertEquals("intent:#Intent;action=x.Y;package=a.b;i.n=12;end", Requests.fill(counted, " 12 "))
        assertEquals("intent:#Intent;action=x.Y;package=a.b;l.n=3000000000;end", Requests.fill(counted, "3000000000"))
        assertNull(Requests.fill(counted, "twelve"))
        assertNull(Requests.fill(counted, "1.5"))
        assertNull(Requests.fill("not an address", "x"))
        assertNull(Requests.fill("intent:#Intent;category=x;package=a.b;end", "x"))
    }

    @Test fun whichCommandsTakeText() {
        assertTrue(Requests.takesArgument("intent:search:{argument}#Intent;scheme=spotify;package=com.spotify.music;end"))
        assertTrue(Requests.takesArgument("intent:#Intent;action=x.Y;package=a.b;S.q={argument};end"))
        assertFalse(Requests.takesArgument("intent:track:1#Intent;scheme=spotify;package=com.spotify.music;end"))
        assertFalse(Requests.takesArgument("intent:#Intent;action=x.Y;package=a.b;S.{argument}=q;end"))
        assertFalse(Requests.takesArgument("{argument}"))
    }

    // ---- what keeps a form from being saved

    @Test fun whatKeepsAFormFromBeingSaved() {
        fun why(r: Request) = Requests.why(r)?.kind
        assertEquals(Problem.Kind.APP, why(Request("", What.LINK, address = "spotify:track:1")))
        assertEquals(Problem.Kind.NO_ADDRESS, why(Request(spotify, What.LINK, address = "spotify:")))
        assertEquals(Problem.Kind.ADDRESS, why(Request(spotify, What.LINK, address = "")))
        assertEquals(Problem.Kind.ADDRESS, why(Request(spotify, What.LINK, address = "open.spotify.com/track/1")))
        assertEquals(Problem(Problem.Kind.REFUSED, "content"), Requests.why(Request(spotify, What.LINK, address = "content://media/1")))
        assertEquals(Problem.Kind.REFUSED, why(Request(spotify, What.CUSTOM, action = "x.Y", address = "file:///sdcard/a")))
        assertEquals(Problem.Kind.REFUSED, why(Request(spotify, What.LINK, address = "intent:#Intent;end")))
        assertEquals(Problem.Kind.TEXT, why(Request(spotify, What.SEND, text = " ")))
        assertEquals(Problem.Kind.TEXT, why(Request(spotify, What.SEARCH)))
        assertEquals(Problem.Kind.ACTION, why(Request(spotify, What.CUSTOM)))
        assertNull(why(Request(spotify, What.CUSTOM, activity = "a.B")))
        assertNull(why(Request(spotify, What.CUSTOM, action = "x.Y")))
        assertEquals(Problem.Kind.EXTRA_NAME, why(Request(spotify, What.SEARCH, text = "x", extras = listOf(text(" ", "v")))))
        assertEquals(Problem(Problem.Kind.EXTRA_TWICE, "a"), Requests.why(Request(spotify, What.SEARCH, text = "x", extras = listOf(text("a", "1"), number("a", "2")))))
        assertEquals(Problem(Problem.Kind.EXTRA_TWICE, "query"), Requests.why(Request(spotify, What.SEARCH, text = "x", extras = listOf(text("query", "y")))))
        assertEquals(Problem(Problem.Kind.EXTRA_TWICE, Requests.FOCUS), Requests.why(Request(spotify, What.PLAY, text = "x", extras = listOf(text(Requests.FOCUS, "a"), text(Requests.FOCUS, "b")))))
        assertEquals(Problem(Problem.Kind.NUMBER, "1.5"), Requests.why(Request(spotify, What.SEARCH, text = "x", extras = listOf(number("n", "1.5")))))
        assertEquals(Problem.Kind.NUMBER, why(Request(spotify, What.SEARCH, text = "x", extras = listOf(number("n", "")))))
        assertNull(why(Request(spotify, What.SEARCH, text = "x", extras = listOf(number("n", "{argument}"), number("m", "-12")))))
        assertEquals(Problem.Kind.YES_NO, why(Request(spotify, What.SEARCH, text = "x", extras = listOf(Extra("b", ExtraKind.YES_NO, "yes")))))
        assertEquals(Problem.Kind.TOO_MANY, why(Request(spotify, What.SEARCH, text = "x", extras = (1..9).map { text("k$it", "v") })))
        assertNull(why(Request(spotify, What.SEARCH, text = "x", extras = (1..8).map { text("k$it", "v") })))
        // What another kind left in the form does not count.
        assertNull(why(Request(spotify, What.SEARCH, address = "nonsense", text = "x")))
    }

    // ---- an am start line

    @Test fun anAmStartLine() {
        assertEquals(Request(spotify, What.LINK, address = "spotify:track:4uLU6hMCjMI75M1A2tKUQC"), ok(Requests.am("am start -a android.intent.action.VIEW -d spotify:track:4uLU6hMCjMI75M1A2tKUQC -p com.spotify.music")))
        assertEquals(Request(spotify, What.LINK, address = "spotify:search:daft%20punk"), ok(Requests.am("am start -d \"spotify:search:daft%20punk\" com.spotify.music")))
        assertEquals(Request(spotify, What.SEARCH, text = "daft punk"), ok(Requests.am("am start -a android.intent.action.SEARCH -p com.spotify.music --es query 'daft punk'")))
        assertEquals(Request("com.example.pod", What.PLAY, text = "the daily"),
            ok(Requests.am("am start-activity -a android.media.action.MEDIA_PLAY_FROM_SEARCH -p com.example.pod -e query \"the daily\" --es android.intent.extra.focus 'vnd.android.cursor.item/*'")))
        assertEquals(Request("com.example.notes", What.SEND, text = "It's \"fine\"", extras = listOf(text("android.intent.extra.SUBJECT", "Note"))),
            ok(Requests.am("am start -a android.intent.action.SEND -t text/plain -p com.example.notes --es android.intent.extra.TEXT \"It's \\\"fine\\\"\" --es android.intent.extra.SUBJECT Note")))
        assertEquals(Request("com.example.app", What.CUSTOM, activity = "com.example.app.Main"), ok(Requests.am("am start -n com.example.app/.Main")))
        assertEquals(Request("com.example.app", What.CUSTOM, activity = "com.example.app.Main"), ok(Requests.am("am start com.example.app/.Main")))
        assertEquals(Request("com.example.app", What.CUSTOM, action = "com.example.SHOW", activity = "org.other.Show", extras = listOf(number("count", "3"), yes("fresh", true), yes("old", false), text("note", "-x"))),
            ok(Requests.am("am start -a com.example.SHOW -n com.example.app/org.other.Show --ei count 3 --ez fresh true --ez old f --es note -x")))
        // No app is named: the form asks for one.
        assertEquals(Request("", What.LINK, address = "spotify:track:1"), ok(Requests.am("am start -a android.intent.action.VIEW -d spotify:track:1")))
        assertEquals(Request("", What.LINK, address = "spotify:track:1"), ok(Requests.am("am start spotify:track:1")))
        assertEquals(Request("com.example.app", What.CUSTOM), ok(Requests.am("am start com.example.app")))
    }

    @Test fun theLineAsItIsSaidFromAnotherMachine() {
        val want = Request(spotify, What.SEARCH, text = "daft punk")
        assertEquals(want, ok(Requests.am("adb shell am start -a android.intent.action.SEARCH -p com.spotify.music --es query 'daft punk'")))
        assertEquals(want, ok(Requests.am("adb -s SERIAL shell \"am start -a android.intent.action.SEARCH -p com.spotify.music --es query 'daft punk'\"")))
        assertEquals(want, ok(Requests.am("$ adb shell cmd activity start-activity -a android.intent.action.SEARCH \\\n  -p com.spotify.music --es query daft\\ punk")))
        assertEquals(want, ok(Requests.paste("\n  am start -a android.intent.action.SEARCH -p com.spotify.music --es query 'daft punk'\n")))
        assertEquals(want, ok(Requests.paste(" intent:#Intent;action=android.intent.action.SEARCH;package=com.spotify.music;S.query=daft%20punk;end")))
    }

    @Test fun anAddressAsTheLinesLastWord() {
        assertEquals(Request(spotify, What.SEARCH, text = "lofi", extras = listOf(yes("shuffle", true))),
            ok(Requests.am("am start --ez shuffle true 'intent:#Intent;action=android.intent.action.SEARCH;package=com.spotify.music;S.query=lofi;end'")))
        // A flag wins over what the address says.
        assertEquals(Request(spotify, What.SEARCH, text = "jazz"), ok(Requests.am("am start --es query jazz 'intent:#Intent;action=android.intent.action.SEARCH;package=com.spotify.music;S.query=lofi;end'")))
        assertEquals(Problem(Problem.Kind.FIELD, "category"), bad(Requests.am("am start 'intent:#Intent;category=x;end'")))
        assertEquals(Problem(Problem.Kind.TWO_APPS), bad(Requests.am("am start -p a.b 'intent:#Intent;component=c.d/.X;end'")))
        assertEquals(Problem(Problem.Kind.TWO_APPS), bad(Requests.am("am start -p a.b 'intent:#Intent;action=x.Y;package=c.d;end'")))
        assertEquals(Request("a.b", What.CUSTOM, action = "x.Y", activity = "a.b.X"), ok(Requests.am("am start -p a.b 'intent:#Intent;action=x.Y;component=a.b/.X;end'")))
    }

    @Test fun whatWasReadCanBeSaved() {
        // A link and a type, with no action: one to view, said in the form, and nothing keeps it from being saved.
        val r = ok(Requests.am("am start -d example://x -t text/plain -p a.b"))
        assertEquals(Request("a.b", What.CUSTOM, action = Requests.VIEW, address = "example://x", type = "text/plain"), r)
        assertNull(Requests.why(r))
        assertEquals(r, ok(Requests.read(Requests.write(r))))
        for (line in listOf("am start -n a.b/.Main", "am start a.b/.Main", "am start -a x.Y -p a.b --ei n 3", "am start -d spotify:track:1 -p a.b", "am start -a android.intent.action.SEND -t text/plain -p a.b --es android.intent.extra.TEXT hi")) {
            val read = ok(Requests.am(line))
            assertNull(line, Requests.why(read))
            assertEquals(line, read, ok(Requests.read(Requests.write(read))).let { back -> if (read.action.isEmpty()) back.copy(action = "") else back })
        }
    }

    @Test fun whoRunsTheLineIsPassedOver() {
        val want = Request("a.b", What.CUSTOM, action = "x.Y", extras = listOf(text("k", "v")))
        assertEquals(want, ok(Requests.am("am start --user current -a x.Y -p a.b --es k v")))
        assertEquals(want, ok(Requests.am("am start -a x.Y --user 10 -p a.b --es k v")))
        assertEquals(want, ok(Requests.am("adb shell am start-activity -a x.Y -p a.b --es k v --user 0")))
        assertEquals(Problem(Problem.Kind.VALUE, "--user"), bad(Requests.am("am start -a x.Y -p a.b --user")))
    }

    @Test fun flagsThatAreNotReadAreNamed() {
        assertEquals(Problem(Problem.Kind.FLAG, "--el"), bad(Requests.am("am start -a x.Y -p a.b --el at 1790000000000")))
        assertEquals(Problem(Problem.Kind.FLAG, "-c"), bad(Requests.am("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n a.b/.Main")))
        assertEquals(Problem(Problem.Kind.FLAG, "--display"), bad(Requests.am("am start --user current --display 1 -a x.Y -p a.b")))
        assertEquals(Problem(Problem.Kind.FLAG, "-f"), bad(Requests.am("am start -f 0x10000000 -a x.Y -p a.b")))
        assertEquals(Problem(Problem.Kind.FLAG, "--grant-read-uri-permission"), bad(Requests.am("am start -a x.Y -p a.b --grant-read-uri-permission")))
        assertEquals(Problem(Problem.Kind.FLAG, "-W"), bad(Requests.am("am start -W -n a.b/.Main")))
        assertEquals(Problem(Problem.Kind.FLAG, "--eu"), bad(Requests.am("am start -n a.b/.Main --eu android.intent.extra.STREAM content://x/1")))
    }

    @Test fun aLineThatCannotBeRead() {
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.am("play some music")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.am("")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.am("am broadcast -a x.Y")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.am("am startservice -n a.b/.S")))
        assertEquals(Problem(Problem.Kind.NOT_A_LINE), bad(Requests.am("adb devices")))
        assertEquals(Problem(Problem.Kind.QUOTE), bad(Requests.am("am start -a x.Y --es note 'not closed")))
        assertEquals(Problem(Problem.Kind.VALUE, "-a"), bad(Requests.am("am start -p a.b -a")))
        assertEquals(Problem(Problem.Kind.VALUE, "--es"), bad(Requests.am("am start -p a.b --es name")))
        assertEquals(Problem(Problem.Kind.NUMBER, "three"), bad(Requests.am("am start -a x.Y -p a.b --ei count three")))
        assertEquals(Problem(Problem.Kind.YES_NO, "maybe"), bad(Requests.am("am start -a x.Y -p a.b --ez on maybe")))
        assertEquals(Problem(Problem.Kind.WORD, "again"), bad(Requests.am("am start -a x.Y a.b again")))
        assertEquals(Problem(Problem.Kind.WORD, "c.d"), bad(Requests.am("am start -a x.Y -p a.b c.d")))
        assertEquals(Problem(Problem.Kind.COMPONENT, "a.b/"), bad(Requests.am("am start -n a.b/")))
        assertEquals(Problem(Problem.Kind.COMPONENT, "Main"), bad(Requests.am("am start -n Main")))
        assertEquals(Problem(Problem.Kind.TWO_APPS), bad(Requests.am("am start -n a.b/.Main -p c.d")))
        assertEquals(Problem(Problem.Kind.TOO_MANY), bad(Requests.am("am start -a x.Y -p a.b " + (1..9).joinToString(" ") { "--es k$it v" })))
    }

    @Test fun aLineInItsWords() {
        assertEquals(listOf("a", "b c", "d e", "f g", ""), Requests.words("a 'b c'  \"d e\" f\\ g ''"))
        assertEquals(listOf("it's", "say \"hi\"", "\$HOME", "a\\b"), Requests.words("\"it's\" \"say \\\"hi\\\"\" '\$HOME' 'a\\b'"))
        assertEquals(listOf("ab", "c"), Requests.words("a\\\nb\tc\n"))
        assertEquals(emptyList<String>(), Requests.words("   "))
        assertNull(Requests.words("a \"b"))
    }

    @Test fun theLettersOfAnAddress() {
        assertEquals("a%20b%3Bc%3Dd%25e%23f%2Fg", Requests.encode("a b;c=d%e#f/g"))
        assertEquals("text/plain", Requests.encode("text/plain", "/"))
        assertEquals("%C3%A9%F0%9F%8E%89", Requests.encode("é🎉"))
        assertEquals("{argument}", Requests.encode("{argument}", "{}"))
        assertEquals("%7Bargument%7D", Requests.encode("{argument}"))
        assertEquals("a b;c=d%e#f/g é🎉", Requests.decode("a%20b%3Bc%3Dd%25e%23f%2Fg%20%C3%A9%F0%9F%8E%89"))
        assertEquals("100% é🎉 %zz", Requests.decode("100% é🎉 %zz"))
        assertEquals("a+b", Requests.decode("a+b"))
    }
}
