package io.github.kuscher.booklight.providers

import android.content.Context
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Action
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.Colors
import io.github.kuscher.booklight.core.Effect
import io.github.kuscher.booklight.core.Icon
import io.github.kuscher.booklight.core.Kind
import io.github.kuscher.booklight.core.Matcher
import io.github.kuscher.booklight.core.Provider
import io.github.kuscher.booklight.core.Query
import io.github.kuscher.booklight.core.Result
import io.github.kuscher.booklight.core.Rgb
import io.github.kuscher.booklight.core.Secrets
import java.security.SecureRandom

/**
 * Things answered in place, like sums: a colour value typed as it is written in code, a fresh
 * password, a UUID. Enter copies.
 */
class Answers(private val context: Context) : Provider {
    override val id = "answers"
    private val random = SecureRandom()

    override suspend fun query(q: Query): List<Result> {
        val t = q.text
        Colors.parse(t)?.let { return listOf(color(context, it)) }
        val words = t.lowercase().split(' ').filter { it.isNotEmpty() }
        val first = words.firstOrNull() ?: return emptyList()
        if (words.size <= 2 && context.getString(R.string.password_keys).split(',').any { it == first }) {
            val length = words.getOrNull(1)?.toIntOrNull() ?: if (words.size == 1) 16 else return emptyList()
            return listOf(secret(Secrets.password(length, random), context.getString(R.string.password_sub, length.coerceIn(8, 64))))
        }
        if (words.size == 1 && Matcher.fold(first) == "uuid") return listOf(secret(Secrets.uuid(random), "UUID"))
        return emptyList()
    }

    /** A new one each time it is asked for; what Enter copies is the one on screen. */
    private fun secret(value: String, kind: String) = Result(
        id = "answer:secret", provider = id, kind = Kind.ANSWER, title = value, subtitle = kind, icon = Icon.Symbol("key"), score = 1.0, learnable = false,
        body = Body.Mono(value),
        actions = listOf(
            Action("copy", context.getString(R.string.action_copy), Effect.CopyText(value, sensitive = true)),
            Action("again", context.getString(R.string.action_new_one), Effect.Internal("again"), keepOpen = true),
        ),
    )

    companion object {
        /** A colour: its swatch, and one action per way of writing it. The armed one is shown large and is what Enter copies. */
        fun color(context: Context, c: Rgb): Result {
            val copy = context.getString(R.string.action_copy)
            return Result(
                id = "answer:color", provider = "answers", kind = Kind.ANSWER, title = c.hex(), icon = Icon.Swatch(c.argb), score = 1.0, learnable = false,
                answer = c.hex(),
                actions = listOf(
                    Action("hex", "$copy HEX", Effect.CopyText(c.hex()), symbol = "t:#"),
                    Action("rgb", "$copy RGB", Effect.CopyText(c.rgb()), symbol = "t:RGB"),
                    Action("hsl", "$copy HSL", Effect.CopyText(c.hsl()), symbol = "t:HSL"),
                    Action("oklch", "$copy OKLCH", Effect.CopyText(c.oklch()), symbol = "t:LCH"),
                ),
            )
        }
    }
}
