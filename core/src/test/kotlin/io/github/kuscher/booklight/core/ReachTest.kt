package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReachTest {
    // A way to go

    @Test fun aPlaceIsWhereToGoFromHere() {
        assertEquals(Trip("", "hamburg hbf", null), Reach.trip("hamburg hbf"))
        assertEquals(Trip("", "Hamburg Hbf", null), Reach.trip("  Hamburg Hbf "))
        assertEquals(Trip("", "hamburg", null), Reach.trip("to hamburg"))
        assertEquals(Trip("", "hamburg", null), Reach.trip("nach hamburg"))
    }

    @Test fun bothEnds() {
        assertEquals(Trip("berlin", "hamburg", null), Reach.trip("from berlin to hamburg"))
        assertEquals(Trip("berlin", "hamburg", null), Reach.trip("berlin to hamburg"))
        assertEquals(Trip("berlin", "hamburg", null), Reach.trip("to hamburg from berlin"))
        assertEquals(Trip("berlin", "hamburg", null), Reach.trip("hamburg from berlin"))
        assertEquals(Trip("Berlin Hbf", "Hamburg Altona", null), Reach.trip("From Berlin Hbf To Hamburg Altona"))
        assertEquals(Trip("berlin", "hamburg", null), Reach.trip("von berlin nach hamburg"))
        assertEquals(Trip("berlin", "potsdam", null), Reach.trip("berlin nach potsdam"))
    }

    @Test fun theWayOfTravelling() {
        assertEquals(Trip("berlin", "hamburg", Travel.TRANSIT), Reach.trip("berlin to hamburg by train"))
        assertEquals(Trip("", "hamburg hbf", Travel.TRANSIT), Reach.trip("hamburg hbf by bus"))
        assertEquals(Trip("", "hamburg hbf", Travel.TRANSIT), Reach.trip("hamburg hbf by public transport"))
        assertEquals(Trip("", "hamburg hbf", Travel.BIKE), Reach.trip("hamburg hbf by bike"))
        assertEquals(Trip("", "hamburg hbf", Travel.WALK), Reach.trip("hamburg hbf by foot"))
        assertEquals(Trip("", "hamburg hbf", Travel.WALK), Reach.trip("hamburg hbf on foot"))
        assertEquals(Trip("", "hamburg hbf", Travel.WALK), Reach.trip("hamburg hbf walking"))
        assertEquals(Trip("", "hamburg hbf", Travel.CAR), Reach.trip("hamburg hbf By Car"))
        assertEquals(Trip("", "hamburg", Travel.TRANSIT), Reach.trip("by train to hamburg"))
    }

    @Test fun inGerman() {
        assertEquals(Trip("berlin", "hamburg", Travel.TRANSIT), Reach.trip("von berlin nach hamburg mit dem zug"))
        assertEquals(Trip("", "hamburg", Travel.TRANSIT), Reach.trip("nach hamburg mit der bahn"))
        assertEquals(Trip("", "alexanderplatz", Travel.TRANSIT), Reach.trip("alexanderplatz mit der S-Bahn"))
        assertEquals(Trip("", "alexanderplatz", Travel.TRANSIT), Reach.trip("alexanderplatz mit den Öffis"))
        assertEquals(Trip("", "bäcker", Travel.WALK), Reach.trip("bäcker zu fuß"))
        assertEquals(Trip("", "bäcker", Travel.WALK), Reach.trip("bäcker zu Fuss"))
        assertEquals(Trip("berlin", "potsdam", Travel.BIKE), Reach.trip("berlin nach potsdam mit dem rad"))
        assertEquals(Trip("", "potsdam", Travel.BIKE), Reach.trip("potsdam per Fahrrad"))
        assertEquals(Trip("", "hamburg", Travel.CAR), Reach.trip("mit dem auto nach hamburg"))
    }

    @Test fun whileItIsBeingTyped() {
        assertEquals(Trip("", "", null), Reach.trip(""))
        assertEquals(Trip("", "", null), Reach.trip("   "))
        assertEquals(Trip("", "", null), Reach.trip("to"))
        assertEquals(Trip("berlin", "", null), Reach.trip("from berlin"))
        assertEquals(Trip("berlin", "", null), Reach.trip("berlin to"))
        assertEquals(Trip("", "", Travel.TRANSIT), Reach.trip("by train"))
        assertEquals(Trip("", "hamburg by", null), Reach.trip("hamburg by"))
    }

    @Test fun aPlaceStaysAPlace() {
        assertEquals(Trip("", "Toronto", null), Reach.trip("Toronto"))                       // "to" inside a word
        assertEquals(Trip("", "stratford on avon", null), Reach.trip("stratford on avon"))
        assertEquals(Trip("", "henley on bus", null), Reach.trip("henley on bus"))           // on leads only to feet
        assertEquals(Trip("", "walking street", null), Reach.trip("walking street"))         // one word says it only at the end
        assertEquals(Trip("", "stand by me", null), Reach.trip("stand by me"))
        assertEquals(Trip("", "Carl-von-Ossietzky-Straße", null), Reach.trip("Carl-von-Ossietzky-Straße"))
        // "von" inside a place is part of its name; "from" after a place is where to start.
        assertEquals(Trip("", "Otto von Guericke Straße Magdeburg", null), Reach.trip("Otto von Guericke Straße Magdeburg"))
        assertEquals(Trip("", "Otto von Guericke Straße", Travel.BIKE), Reach.trip("Otto von Guericke Straße mit dem Rad"))
    }

    @Test fun aLeadingToMakesTheRestThePlace() {
        assertEquals(Trip("", "Welcome to Las Vegas sign", null), Reach.trip("to Welcome to Las Vegas sign"))
        assertEquals(Trip("", "von der Heydt Museum", null), Reach.trip("nach von der Heydt Museum"))
        assertEquals(Trip("berlin", "hamburg", null), Reach.trip("nach hamburg von berlin"))
        assertEquals(Trip("Bonn", "von der Heydt Museum", null), Reach.trip("nach von der Heydt Museum von Bonn"))
    }

    @Test fun fromHereIsFromWhereTheUserIs() {
        assertEquals(Trip("", "hamburg", null), Reach.trip("from here to hamburg"))
        assertEquals(Trip("", "hamburg", Travel.TRANSIT), Reach.trip("von Hier nach hamburg mit dem zug"))
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=hamburg", Reach.directions(Reach.trip("from here to hamburg")))
    }

    @Test fun directions() {
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=hamburg%20hbf", Reach.directions(Reach.trip("hamburg hbf")))
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=hamburg&origin=berlin&travelmode=transit", Reach.directions(Reach.trip("berlin to hamburg by train")))
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=hamburg&travelmode=walking", Reach.directions(Trip("", "hamburg", Travel.WALK)))
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=hamburg&travelmode=bicycling", Reach.directions(Trip("", "hamburg", Travel.BIKE)))
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=hamburg&travelmode=driving", Reach.directions(Trip("", "hamburg", Travel.CAR)))
        // A place is text in an address: nothing typed can add to the link.
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=K%C3%B6ln%2FBonn%20%26%20x%3D1&origin=a%23b", Reach.directions(Trip("a#b", "Köln/Bonn & x=1", null)))
    }

    // A number, and the text after it

    @Test fun aNumberTheWaysPeopleTypeOne() {
        assertEquals(Dial("+49 30 5550 1234", "+493055501234", ""), Reach.phone("+49 30 5550 1234"))
        assertEquals(Dial("+49-30-5550-1234", "+493055501234", ""), Reach.phone("+49-30-5550-1234"))
        assertEquals(Dial("0049 30 55501234", "00493055501234", ""), Reach.phone("0049 30 55501234"))
        assertEquals(Dial("(030) 5550-1234", "03055501234", ""), Reach.phone("(030) 5550-1234"))
        assertEquals(Dial("030/55501234", "03055501234", ""), Reach.phone("030/55501234"))
        assertEquals(Dial("030 / 5550 1234", "03055501234", ""), Reach.phone("030 / 5550 1234"))
        assertEquals(Dial("030.5550.1234", "03055501234", ""), Reach.phone(" 030.5550.1234 "))
        assertEquals(Dial("112", "112", ""), Reach.phone("112"))
        // The dashes of a number copied from a page: a non-breaking hyphen, an en dash.
        assertEquals("+493055501234", Reach.phone("+49 30 5550\u20111234")?.number)
        assertEquals("03055501234", Reach.phone("030 5550\u20131234")?.number)
    }

    @Test fun theZeroInBracketsIsNotDialledAfterACountryCode() {
        assertEquals("+493055501234", Reach.phone("+49 (0)30 5550 1234")?.number)
        assertEquals("+493055501234", Reach.phone("+49 (0) 30 5550 1234")?.number)
        assertEquals("00493055501234", Reach.phone("0049 (0)30 5550 1234")?.number)
        assertEquals("03055501234", Reach.phone("(0)30 5550 1234")?.number)
    }

    @Test fun theTextAfterTheNumber() {
        assertEquals(Dial("+49 171 5550123", "+491715550123", "running late"), Reach.phone("+49 171 5550123 running late"))
        assertEquals(Dial("0171 5550123", "01715550123", "Running  late!"), Reach.phone("0171 5550123   Running  late!"))
        assertEquals(Dial("0171 5550123", "01715550123", "late"), Reach.phone("0171 5550123 - late"))
        // A text that starts with a number: the phone number takes it, unless a colon or a comma ends the number.
        assertEquals(Dial("0171 5550123 10", "0171555012310", "min late"), Reach.phone("0171 5550123 10 min late"))
        assertEquals(Dial("0171 5550123", "01715550123", "10 min late"), Reach.phone("0171 5550123: 10 min late"))
        assertEquals(Dial("+49 171 5550123", "+491715550123", "10 min late"), Reach.phone("+49 171 5550123, 10 min late"))
        assertEquals(Dial("+49 171 5550123", "+491715550123", "late"), Reach.phone("+49 171 5550123; late"))
        // A group of digits with letters in it is text.
        assertEquals(Dial("0171 5550123", "01715550123", "5pm ok?"), Reach.phone("0171 5550123 5pm ok?"))
        // More digits than a number has are text.
        assertEquals(Dial("030 5550 1234", "03055501234", "12345678901"), Reach.phone("030 5550 1234 12345678901"))
    }

    @Test fun notANumber() {
        for (text in listOf("", "   ", "anna", "12", "+", "++49 30 5550", "030+5550", "running late 030 5550 1234", "3pm", "1,5", ":", "(0)(0)(0)", "+(0)(0)(0)", "- / -")) assertNull(text, Reach.phone(text))
    }

    @Test fun whatCanStillBecomeANumber() {
        for (text in listOf("", "  ", "+", "+4", "+49", "0", "03", "(0", "+49 (", "0 0", "- ")) assertTrue(text, Reach.startsNumber(text))
        // A line that was not meant for a number: its row goes, and the search for all of it is row one.
        for (text in listOf("of duty", "o", "anna", "+49 anna", "3pm", "1,5", "me maybe")) assertFalse(text, Reach.startsNumber(text))
        // A number that is there is not one that is still coming.
        for (text in listOf("112", "+49 30 5550 1234", "030 5550 1234 running late")) assertFalse(text, Reach.startsNumber(text))
    }

    @Test fun theNumberWithItsCountryCode() {
        assertEquals("493055501234", Reach.phone("+49 30 5550 1234")?.full)
        assertEquals("493055501234", Reach.phone("0049 30 5550 1234")?.full)
        assertNull(Reach.phone("030 5550 1234")?.full)
        assertNull(Reach.phone("+49 30")?.full)                // not a whole number yet
        assertNull(Reach.phone("+0 30 5550 1234")?.full)       // no country code starts with a zero
    }

    @Test fun aChatInWhatsApp() {
        assertEquals("https://wa.me/493055501234?text=running%20late", Reach.whatsapp("493055501234", "running late"))
        assertEquals("https://wa.me/493055501234", Reach.whatsapp("493055501234", ""))
        assertEquals("https://wa.me/493055501234?text=5%20%2B%205%20%26%20more%3F", Reach.whatsapp("493055501234", "5 + 5 & more?"))
    }

    // Telegram

    @Test fun aTelegramName() {
        assertEquals(Handle("anna", "on my way"), Reach.handle("anna on my way"))
        assertEquals(Handle("anna_b2", ""), Reach.handle("@anna_b2"))
        assertEquals(Handle("anna", "hi"), Reach.handle("t.me/anna hi"))
        assertEquals(Handle("anna", ""), Reach.handle("https://t.me/anna"))
        assertEquals(Handle("anna", "on my way"), Reach.handle("anna: on my way"))
        for (text in listOf("", "  ", "bob hi", "4nna hi", "anna-b hi", "+49 171 5550123 hi", "a".repeat(33))) assertNull(text, Reach.handle(text))
    }

    @Test fun whatCanStillBecomeATelegramName() {
        for (text in listOf("", " ", "a", "an", "ann", "@", "@an", "t", "t.me", "t.me/", "t.me/an", "https://t.me/a", "HTTPS://T.ME/")) assertTrue(text, Reach.startsName(text))
        // A second word ends the first: "bob" had its chance to become a name. And what no name may hold cannot become one.
        for (text in listOf("bob hi", "me a river", "4nna", "anna-b", "@@", "t.me//", "ä", "a".repeat(33))) assertFalse(text, Reach.startsName(text))
        // With a name in front the rest is the text, whatever it is.
        assertEquals(Handle("anna", "of duty"), Reach.handle("anna of duty"))
    }

    @Test fun aChatInTelegram() {
        assertEquals("https://t.me/anna?text=on%20my%20way", Reach.telegram("anna", "on my way"))
        assertEquals("https://t.me/anna", Reach.telegram("anna", " "))
        assertEquals("https://t.me/+491715550123?text=hi", Reach.telegram("+491715550123", "hi"))
    }

    // Meet

    @Test fun aMeetingsCode() {
        assertEquals("abc-defg-hij", Reach.meetCode("abc-defg-hij"))
        assertEquals("abc-defg-hij", Reach.meetCode(" ABC-Defg-hij "))
        assertEquals("abc-defg-hij", Reach.meetCode("https://meet.google.com/abc-defg-hij"))
        assertEquals("abc-defg-hij", Reach.meetCode("meet.google.com/abc-defg-hij?authuser=0"))
        assertEquals("abc-defg-hij", Reach.meetCode("HTTPS://Meet.Google.com/abc-defg-hij/"))
        for (text in listOf("", "abcdefghij", "abc-defg", "abc-defg-hijk", "ab1-defg-hij", "with anna tomorrow", "the-team-now please", "abc-def\u017F-hij", "äbc-defg-hij")) assertNull(text, Reach.meetCode(text))
    }

    @Test fun whatCanStillBecomeACode() {
        for (text in listOf("", " ", "a", "ab", "abc", "abc-", "abc-de", "abc-defg", "abc-defg-", "abc-defg-hi", "ABC-d", "h", "https://", "meet.google.com/", "https://meet.google.com/abc-", "the")) assertTrue(text, Reach.startsCode(text))
        for (text in listOf("the parents", "anna", "abcd", "abc-defgh", "ab-", "abc--", "abc-defg-hijk", "ab1", "me at 5", "https://example.com", "abc-def\u017F")) assertFalse(text, Reach.startsCode(text))
    }

    @Test fun aMeeting() {
        assertEquals("https://meet.google.com/new", Reach.meet(null))
        assertEquals("https://meet.google.com/abc-defg-hij", Reach.meet("abc-defg-hij"))
    }
}
