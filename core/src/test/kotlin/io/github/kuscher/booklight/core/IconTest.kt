package io.github.kuscher.booklight.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconTest {
    @Test fun anAppsIconAsAnActionsSymbol() {
        val icon = Icon.App("com.example.player", "com.example.player.MainActivity", 10)
        assertEquals("app:com.example.player/com.example.player.MainActivity#10", icon.symbol)
        assertEquals(icon, Icon.App.of(icon.symbol))
        assertEquals(Icon.App("a", ".B"), Icon.App.of(Icon.App("a", ".B").symbol))
        // A class name may hold a dollar sign or a hash of its own: the user is what follows the last one.
        assertEquals(Icon.App("a", "b.C\$D#E", 3), Icon.App.of("app:a/b.C\$D#E#3"))
    }

    @Test fun aColourAsAnActionsSymbol() {
        // A calendar's own colour marks its line in the list of calendars.
        for (argb in listOf(0xFF0B8043.toInt(), 0xFFFFFFFF.toInt(), 0x00000000, 0xFF000000.toInt(), 0x7F123456)) assertEquals(Icon.Swatch(argb), Icon.Swatch.of(Icon.Swatch(argb).symbol))
        assertEquals("dot:ff0b8043", Icon.Swatch(0xFF0B8043.toInt()).symbol)
        for (s in listOf("", "dot", "dot:", "dot:xyz", "dot:1ffffffff", "dot:-1", "dots:ff0b8043", "copy", "t:HEX", "app:a/b#1")) assertNull(s, Icon.Swatch.of(s))
        // And an app's icon is no colour.
        assertNull(Icon.Swatch.of(Icon.App("a", ".B").symbol))
        assertNull(Icon.App.of(Icon.Swatch(1).symbol))
    }

    @Test fun booklightsOwnSymbolsNameNoApp() {
        for (s in listOf("", "play", "open", "t:HEX", "app", "app:", "app:/", "app:a", "app:a/", "app:a/b", "app:a/b#", "app:a/b#x", "app:/b#1", "app:a/#1", "apps:a/b#1"))
            assertNull(s, Icon.App.of(s))
    }
}
