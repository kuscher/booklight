package io.github.kuscher.booklight.providers

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Installed apps, as the launcher sees them: every launchable activity of every profile
 * (LauncherApps; visibility from the manifest's MAIN/LAUNCHER <queries>, no permission).
 * The list is held in memory and refreshed when packages change.
 */
class AppsProvider(private val context: Context, private val scope: CoroutineScope) : Provider {
    override val id = "apps"

    private data class App(val label: String, val pkg: String, val cls: String, val user: Long)

    @Volatile private var index: List<App> = emptyList()
    @Volatile var loadedMs: Long = -1; private set
    val count: Int get() = index.size

    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)

    init {
        reload()
        launcher.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
            override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
            override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        }, Handler(Looper.getMainLooper()))
    }

    fun reload() {
        scope.launch(Dispatchers.IO) {
            val t0 = System.nanoTime()
            val own = context.packageName
            val out = ArrayList<App>()
            for (profile in launcher.profiles) {
                val serial = users.getSerialNumberForUser(profile)
                for (a in launcher.getActivityList(null, profile)) {
                    if (a.componentName.packageName == own) continue   // Booklight doesn't list itself
                    out.add(App(a.label.toString(), a.componentName.packageName, a.componentName.className, serial))
                }
            }
            index = out.sortedBy { it.label.lowercase() }
            loadedMs = (System.nanoTime() - t0) / 1_000_000
        }
    }

    override suspend fun query(q: Query): List<Result> = index.mapNotNull { a ->
        val s = Matcher.score(q.text, a.label)
        if (s > 0) result(a, s) else null
    }

    /** Every app with score 0: the engine keeps only the ones you have used. */
    override suspend fun zeroState(): List<Result> = index.map { result(it, 0.0) }

    private fun result(a: App, score: Double) = Result(
        id = "app:${a.pkg}/${a.cls}" + if (a.user != 0L) "#${a.user}" else "",
        provider = id, kind = Kind.APP, title = a.label,
        icon = Icon.App(a.pkg, a.cls, a.user), score = score,
        actions = listOf(
            Action("open", context.getString(R.string.action_open), Effect.LaunchApp(a.pkg, a.cls, a.user)),
            Action("info", context.getString(R.string.action_app_info), Effect.AppInfo(a.pkg, a.cls, a.user)),
        ),
    )
}
