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

    @Test fun booklightsOwnSymbolsNameNoApp() {
        for (s in listOf("", "play", "open", "t:HEX", "app", "app:", "app:/", "app:a", "app:a/", "app:a/b", "app:a/b#", "app:a/b#x", "app:/b#1", "app:a/#1", "apps:a/b#1"))
            assertNull(s, Icon.App.of(s))
    }
}
