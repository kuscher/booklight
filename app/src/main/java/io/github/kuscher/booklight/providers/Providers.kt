package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.Intent
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Calc
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.SearchEngine
import io.github.kuscher.booklight.core.Web

/** A sum typed into the panel is answered in place; Enter copies the answer. */
class CalcProvider(private val context: Context) : Provider {
    override val id = "calc"
    override suspend fun query(q: Query): List<Result> {
        val a = Calc.answer(q.text) ?: return emptyList()
        return listOf(Result(
            id = "calc", provider = id, kind = Kind.ANSWER, title = a, subtitle = q.text.removePrefix("=").trim(),
            icon = Icon.Symbol("calc"), score = 1.0, answer = a, learnable = false,
            actions = listOf(Action("copy", context.getString(R.string.action_copy), Effect.CopyText(a.replace(",", "")))),
        ))
    }
}

/**
 * System settings pages, from a hand-kept list of public `android.settings.…` actions (the
 * Settings app's own search index isn't open to apps). Pages this device doesn't have are dropped.
 */
class SettingsProvider(private val context: Context) : Provider {
    override val id = "settings"

    private class Page(val key: String, val title: String, val action: String, val words: String = "")

    private val pages: List<Page> by lazy {
        val pm = context.packageManager
        ALL.filter { Intent(it.action).resolveActivity(pm) != null }
    }

    override suspend fun query(q: Query): List<Result> = pages.mapNotNull { p ->
        // Its name counts in full; a keyword ("dark" for Display) counts a little less.
        val s = maxOf(Matcher.score(q.text, p.title), p.words.split(',').maxOfOrNull { Matcher.keyword(q.text, it) } ?: 0.0)
        if (s <= 0) null else Result(
            id = "setting:${p.key}", provider = id, kind = Kind.SETTING, title = p.title,
            icon = Icon.Symbol("settings"), score = s,
            actions = listOf(Action("open", context.getString(R.string.action_open), Effect.OpenSettings(p.action))),
        )
    }

    private companion object {
        val ALL = listOf(
            Page("home", "Settings", "android.settings.SETTINGS", "preferences,options"),
            Page("wifi", "Wi-Fi", "android.settings.WIFI_SETTINGS", "wifi,wlan,network,internet"),
            Page("bluetooth", "Bluetooth", "android.settings.BLUETOOTH_SETTINGS", "pair,headphones,devices"),
            Page("display", "Display", "android.settings.DISPLAY_SETTINGS", "brightness,screen,resolution,scaling"),
            Page("dark", "Dark theme", "android.settings.DARK_THEME_SETTINGS", "dark mode,night,appearance"),
            Page("nightlight", "Night Light", "android.settings.NIGHT_DISPLAY_SETTINGS", "blue light,warm"),
            Page("sound", "Sound", "android.settings.SOUND_SETTINGS", "volume,audio,speaker,ringtone"),
            Page("battery", "Battery", "android.intent.action.POWER_USAGE_SUMMARY", "power,charge,saver"),
            Page("storage", "Storage", "android.settings.INTERNAL_STORAGE_SETTINGS", "disk,space,free up"),
            Page("apps", "Apps", "android.settings.MANAGE_APPLICATIONS_SETTINGS", "installed,uninstall,applications"),
            Page("defaults", "Default apps", "android.settings.MANAGE_DEFAULT_APPS_SETTINGS", "browser,assistant,default"),
            Page("notifications", "Notifications", "android.settings.NOTIFICATION_SETTINGS", "alerts,do not disturb"),
            Page("modes", "Modes and Do Not Disturb", "android.settings.ZEN_MODE_SETTINGS", "dnd,focus,quiet"),
            Page("keyboard", "Physical keyboard", "android.settings.HARD_KEYBOARD_SETTINGS", "keyboard,keys,shortcuts,layout,modifier"),
            Page("language", "Languages", "android.settings.LOCALE_SETTINGS", "locale,region,language"),
            Page("ime", "On-screen keyboard", "android.settings.INPUT_METHOD_SETTINGS", "gboard,input method,typing"),
            Page("date", "Date and time", "android.settings.DATE_SETTINGS", "clock,time zone,timezone"),
            Page("accessibility", "Accessibility", "android.settings.ACCESSIBILITY_SETTINGS", "talkback,magnification,captions"),
            Page("privacy", "Privacy", "android.settings.PRIVACY_SETTINGS", "permissions,camera,microphone"),
            Page("security", "Security", "android.settings.SECURITY_SETTINGS", "lock,password,fingerprint,pin"),
            Page("location", "Location", "android.settings.LOCATION_SOURCE_SETTINGS", "gps"),
            Page("network", "Network and internet", "android.settings.WIRELESS_SETTINGS", "ethernet,mobile,connection"),
            Page("vpn", "VPN", "android.settings.VPN_SETTINGS"),
            Page("airplane", "Airplane mode", "android.settings.AIRPLANE_MODE_SETTINGS", "flight mode,offline"),
            Page("hotspot", "Hotspot and tethering", "android.settings.TETHER_SETTINGS", "share internet"),
            Page("data", "Data usage", "android.settings.DATA_USAGE_SETTINGS", "traffic"),
            Page("cast", "Cast", "android.settings.CAST_SETTINGS", "screen mirroring,chromecast,wireless display"),
            Page("print", "Printing", "android.settings.ACTION_PRINT_SETTINGS", "printer"),
            Page("accounts", "Accounts", "android.settings.SYNC_SETTINGS", "google account,sync,passwords"),
            Page("wallpaper", "Wallpaper", "android.intent.action.SET_WALLPAPER", "background,style,theme"),
            Page("update", "System update", "android.settings.SYSTEM_UPDATE_SETTINGS", "software update,os version"),
            Page("about", "About this device", "android.settings.DEVICE_INFO_SETTINGS", "model,serial,build number,version"),
            Page("developer", "Developer options", "android.settings.APPLICATION_DEVELOPMENT_SETTINGS", "adb,debugging,usb debugging"),
        )
    }
}

/** Booklight's own pages, findable like anything else. */
class CommandsProvider(private val context: Context) : Provider {
    override val id = "commands"

    private class Command(val key: String, val title: Int, val subtitle: Int?, val words: String, val effect: Effect)

    private val all = listOf(
        Command("settings", R.string.cmd_settings, null, "booklight,preferences,options,about", Effect.Internal("settings")),
        Command("shortcut", R.string.cmd_shortcut, R.string.cmd_shortcut_sub, "booklight,hotkey,key,shortcut", Effect.Internal("shortcuts")),
    )

    override suspend fun query(q: Query): List<Result> = all.mapNotNull { c ->
        val title = context.getString(c.title)
        val s = maxOf(Matcher.score(q.text, title), c.words.split(',').maxOf { Matcher.keyword(q.text, it) })
        if (s <= 0) null else Result(
            id = "command:${c.key}", provider = id, kind = Kind.COMMAND, title = title, subtitle = c.subtitle?.let(context::getString),
            icon = Icon.Symbol("booklight"), score = s,
            actions = listOf(Action("open", context.getString(R.string.action_open), c.effect)),
        )
    }
}

/**
 * The way out to the web: a typed address opens in the browser, and anything typed can be
 * searched for. Nothing is sent anywhere until you choose the row (Booklight has no internet
 * permission): the browser does the searching.
 */
class WebProvider(private val context: Context) : Provider {
    override val id = "web"

    override suspend fun query(q: Query): List<Result> {
        val t = q.text
        val out = ArrayList<Result>(2)
        Web.url(t)?.let { u ->
            out.add(Result(
                id = "web:url", provider = id, kind = Kind.WEB, title = t, subtitle = context.getString(R.string.action_open_link),
                icon = Icon.Symbol("globe"), score = SearchEngine.URL_SCORE + 0.05, learnable = false,
                actions = listOf(
                    Action("open", context.getString(R.string.action_open_link), Effect.OpenUrl(u)),
                    Action("copy", context.getString(R.string.action_copy), Effect.CopyText(u)),
                ),
            ))
        }
        out.add(Result(
            id = "web:search", provider = id, kind = Kind.WEB, title = context.getString(R.string.web_search_title, t),
            icon = Icon.Symbol("search"), score = 0.1, learnable = false,
            actions = listOf(Action("search", context.getString(R.string.action_search_web), Effect.WebSearch(t))),
        ))
        return out
    }
}
