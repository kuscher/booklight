package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroTest {
    private val now = 1_800_000_000_000L
    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private fun e(count: Double, ago: Long = 0) = History.Entry(count, now - ago)
    private fun seats(items: Map<String, History.Entry>, hidden: Set<String> = emptySet(), held: List<String> = emptyList(), live: (String) -> Boolean = { true }) =
        Zero.pick(items, now, live, hidden, held)
    private fun ids(items: Map<String, History.Entry>, hidden: Set<String> = emptySet(), held: List<String> = emptyList(), live: (String) -> Boolean = { true }) =
        seats(items, hidden, held, live).picks.map { it.id }

    @Test fun noHistoryNoRows() = assertEquals(emptyList<String>(), ids(emptyMap()))

    @Test fun twoEarnedAndNoThirdAreTwoRows() =
        assertEquals(listOf("app:a", "app:b"), ids(mapOf("app:a" to e(5.0, day), "app:b" to e(4.0, day), "app:c" to e(1.0, day))))

    @Test fun neverOneRow() =
        assertEquals(emptyList<String>(), ids(mapOf("app:a" to e(5.0, day), "app:b" to e(1.0, hour))))

    @Test fun threeEarnedStandHeaviestFirst() {
        val picks = seats(mapOf("app:c" to e(3.0, day), "app:a" to e(5.0, day), "app:b" to e(4.0, day))).picks
        assertEquals(listOf("app:a", "app:b", "app:c"), picks.map { it.id })
        assertTrue(picks.all { it.why == Zero.Why.OFTEN })
    }

    @Test fun theThirdSeatIsWhatWasRunLast() {
        val picks = seats(mapOf("app:a" to e(5.0, day), "app:b" to e(4.0, day), "app:c" to e(3.0, day), "setting:wifi" to e(1.0, hour))).picks
        assertEquals(listOf("app:a", "app:b", "setting:wifi"), picks.map { it.id })
        assertEquals(Zero.Why.RECENT, picks[2].why)
    }

    @Test fun recentIsEightHours() {
        val base = mapOf("app:a" to e(5.0, day), "app:b" to e(4.0, day))
        assertEquals(listOf("app:a", "app:b", "app:x"), ids(base + ("app:x" to e(1.0, 8 * hour))))
        assertEquals(listOf("app:a", "app:b"), ids(base + ("app:x" to e(1.0, 8 * hour + 1))))
    }

    @Test fun theMostRecentOfAllMayAlreadyHaveASeat() =
        // a was run last and has seat one: seat three is the newest of the rest.
        assertEquals(listOf("app:a", "app:b", "app:d"), ids(mapOf("app:a" to e(5.0, hour), "app:b" to e(4.0, day), "app:c" to e(1.0, 5 * hour), "app:d" to e(1.0, 2 * hour))))

    @Test fun runTwiceIsEarnedForElevenDays() {
        val others = mapOf("app:a" to e(5.0, day), "app:b" to e(4.0, day))
        assertEquals(listOf("app:a", "app:b", "app:x"), ids(others + ("app:x" to e(2.0, 11 * day))))     // 1.52
        assertEquals(listOf("app:a", "app:b"), ids(others + ("app:x" to e(2.0, 12 * day))))              // 1.49
    }

    @Test fun onlyThingsThatAreOpenedAreSuggested() {
        val noise = mapOf("dial:volume" to e(50.0), "key:snap" to e(40.0), "scope:mail" to e(30.0), "web:url" to e(20.0), "calc" to e(10.0), "text:ask:tr" to e(9.0))
        assertEquals(listOf("app:a", "cmd:x/new", "link:yt"), ids(noise + mapOf("app:a" to e(5.0, day), "cmd:x/new" to e(4.0, day), "link:yt" to e(3.0, day))))
    }

    @Test fun aHiddenOneIsPassedOver() =
        assertEquals(listOf("app:b", "app:c", "app:d"), ids(mapOf("app:a" to e(9.0, day), "app:b" to e(5.0, day), "app:c" to e(4.0, day), "app:d" to e(3.0, day)), hidden = setOf("app:a")))

    @Test fun aHolderThatIsAwayIsPassedOverAndNothingIsKept() {
        val asked = ArrayList<String>()
        val got = seats(mapOf("app:a" to e(9.0, day), "app:b" to e(5.0, day), "app:c" to e(4.0, day), "app:d" to e(3.0, day), "app:never" to e(0.1, 40 * day)), held = listOf("app:a", "app:b")) { asked += it; it != "app:a" }
        assertEquals(listOf("app:b", "app:c", "app:d"), got.picks.map { it.id })
        assertNull("its seat is not given away for good", got.held)
        assertTrue("only candidates are asked about", "app:never" !in asked)
    }

    @Test fun aSeatIsKeptUntilANewcomerWeighsAFifthMore() {
        fun with(c: Double) = seats(mapOf("app:a" to e(6.0, day), "app:b" to e(3.0, day), "app:c" to e(c, day), "app:d" to e(2.0, day)), held = listOf("app:a", "app:b"))
        assertEquals(listOf("app:a", "app:b", "app:c"), with(3.0 * 1.19).picks.map { it.id })
        assertEquals(listOf("app:a", "app:c", "app:b"), with(3.0 * 1.2).picks.map { it.id })
        assertEquals(listOf("app:a", "app:c"), with(3.0 * 1.2).held)
    }

    @Test fun aNewcomerStandsWhereTheOneItPushedOutStood() =
        // A is pushed out; B keeps seat two: "Down, Down, Enter" still means B.
        assertEquals(listOf("app:c", "app:b", "app:a"), ids(mapOf("app:a" to e(3.0, day), "app:b" to e(3.5, day), "app:c" to e(3.7, day)), held = listOf("app:a", "app:b")))

    @Test fun twoNewcomersStandHeaviestFirst() =
        assertEquals(listOf("app:d", "app:c", "app:b"), ids(mapOf("app:a" to e(2.0, day), "app:b" to e(2.5, day), "app:c" to e(4.0, day), "app:d" to e(5.0, day)), held = listOf("app:a", "app:b")))

    @Test fun theTwoSwapOnlyWhenTheSecondWeighsAFifthMore() {
        fun with(b: Double) = ids(mapOf("app:a" to e(3.0, day), "app:b" to e(b, day), "app:c" to e(2.0, day)), held = listOf("app:a", "app:b"))
        assertEquals(listOf("app:a", "app:b", "app:c"), with(3.5))
        assertEquals(listOf("app:b", "app:a", "app:c"), with(3.6))
    }

    @Test fun aHolderThatIsNoLongerEarnedOrIsHiddenGivesUpItsSeat() {
        val items = mapOf("app:a" to e(1.0, day), "app:b" to e(5.0, day), "app:c" to e(4.0, day), "app:d" to e(3.0, day))
        val got = seats(items, held = listOf("app:a", "app:b"))
        assertEquals(listOf("app:b", "app:c", "app:d"), got.picks.map { it.id })
        assertEquals(listOf("app:b", "app:c"), got.held)
        assertEquals(listOf("app:c", "app:d"), seats(items, hidden = setOf("app:b"), held = listOf("app:a", "app:b")).held)
    }

    @Test fun equalWeightsStandInOneOrder() {
        val items = mapOf("app:b" to e(3.0, day), "app:a" to e(3.0, day), "app:c" to e(3.0, day))
        assertEquals(ids(items), ids(items))
        assertEquals(listOf("app:a", "app:b", "app:c"), ids(mapOf("app:c" to e(3.0, day), "app:b" to e(3.0, day), "app:a" to e(3.0, day))))
    }

    @Test fun aClockSetBackMakesNothingRecentAndBreaksNothing() {
        val base = mapOf("app:a" to e(5.0, day), "app:b" to e(4.0, day))
        assertEquals(listOf("app:a", "app:b", "app:x"), ids(base + ("app:x" to History.Entry(1.0, now + 3_000))))
        assertEquals(listOf("app:a", "app:b"), ids(base + ("app:x" to History.Entry(1.0, now + 6_000))))
        // It keeps its count, unfaded.
        assertEquals(3.0, seats(mapOf("app:a" to History.Entry(3.0, now + day), "app:b" to e(3.0, day))).picks.first { it.id == "app:a" }.weight, 0.0)
    }

    @Test fun neverMoreThanThreeAndNoneTwice() {
        val many = (1..40).associate { "app:$it" to e(it.toDouble(), it * hour) }
        val got = seats(many, held = listOf("app:40", "app:40"))
        assertEquals(3, got.picks.size)
        assertEquals(got.picks.map { it.id }.distinct(), got.picks.map { it.id })
        assertTrue(got.held!!.size <= 2)
    }

    // "Don't suggest" among a row's actions.

    private fun row(vararg actions: Action, armed: Int = 0) = Result(id = "app:x", provider = "apps", kind = Kind.APP, title = "X", icon = Icon.Symbol("app"), score = 1.0, actions = actions.toList(), armed = armed)
    private fun a(id: String, more: Boolean = false, danger: Boolean = false) = Action(id, id, Effect.Internal(id), more = more, danger = danger)

    @Test fun onAnAppItIsBehindTheArrowAndLastBeforeUninstall() {
        val r = Zero.offer(row(a("open"), a("info"), a("left", more = true), a("uninstall", more = true, danger = true), armed = 3), "Don't suggest")
        assertEquals(listOf("open", "info", "left", "unsuggest", "uninstall"), r.actions.map { it.id })
        assertTrue(r.actions[3].more)
        // An app that cannot be uninstalled: the last line of its list.
        assertEquals(listOf("open", "left", "unsuggest"), Zero.offer(row(a("open"), a("left", more = true)), "Don't suggest").actions.map { it.id })
        assertEquals("an arming that pointed at Uninstall still does", "uninstall", r.actions[r.armed].id)
        assertEquals(Effect.Unsuggest("app:x"), r.actions[3].effect)
    }

    @Test fun onARowWithOneActionItIsTheLast() =
        assertEquals(listOf("open", "unsuggest"), Zero.offer(row(a("open")), "Don't suggest").actions.map { it.id })

    @Test fun itStandsBeforeWhatRemoves() {
        val r = Zero.offer(row(a("open"), a("link"), a("edit"), a("delete", danger = true)), "Don't suggest")
        assertEquals(listOf("open", "link", "edit", "unsuggest", "delete"), r.actions.map { it.id })
        assertEquals(0, r.armed)
    }

    @Test fun offeredTwiceIsOfferedOnce() {
        val once = Zero.offer(row(a("open"), a("delete", danger = true)), "Don't suggest")
        assertEquals(once, Zero.offer(once, "Don't suggest"))
    }
}
