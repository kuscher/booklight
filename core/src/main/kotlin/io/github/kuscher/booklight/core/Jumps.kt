package io.github.kuscher.booklight.core

/** Short forms that are an address: a GitHub repository, a port on this machine. */
object Jumps {
    /**
     * "owner/repo" is the repository, "owner/repo#12" issue 12 (GitHub sends a pull request's number on
     * to the pull request), "owner/repo/pull/3" and any other path as typed. A single word, spaces or
     * anything else: null, and the text is searched for instead.
     */
    fun github(arg: String): String? {
        val t = arg.trim()
        if (t.length > 2000 || t.any { it.isWhitespace() || it.isISOControl() }) return null
        val slash = t.indexOf('/')
        if (slash <= 0) return null
        var end = slash + 1
        while (end < t.length && named(t[end])) end++
        val owner = t.substring(0, slash)
        val repo = t.substring(slash + 1, end)
        if (!name(owner) || !name(repo)) return null
        val base = "https://github.com/$owner/$repo"
        val tail = t.substring(end)
        return when {
            tail.isEmpty() -> base
            tail[0] == '/' -> "https://github.com/$t"
            tail[0] == '#' && tail.length in 2..12 && tail.drop(1).all { it in '0'..'9' } -> "$base/issues/${tail.drop(1)}"
            else -> null
        }
    }

    /** ":3000" is http://localhost:3000, ":3000/path?x=1" keeps the path. Ports 1 to 65535; otherwise null. */
    fun port(text: String): String? {
        val t = text.trim()
        if (!t.startsWith(":")) return null
        var i = 1
        while (i < t.length && t[i] in '0'..'9') i++
        val digits = t.substring(1, i)
        if (digits.isEmpty() || digits.length > 5 || digits.toInt() !in 1..65535) return null
        val path = t.substring(i)
        if (path.isNotEmpty() && (path[0] != '/' || path.any { it.isWhitespace() || it.isISOControl() })) return null
        return "http://localhost:${digits.toInt()}$path"
    }

    private fun named(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '.' || c == '-'

    /** GitHub's letters for an owner or a repository; dots alone would be a path, not a name. */
    private fun name(s: String) = s.isNotEmpty() && s.all(::named) && s.any { it != '.' }
}
