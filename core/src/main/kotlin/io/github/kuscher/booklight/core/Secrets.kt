package io.github.kuscher.booklight.core

import java.util.Random

/**
 * Passwords and UUIDs. The randomness is the caller's: the app passes a `SecureRandom`, tests a
 * seeded `Random` so that they can say what comes out.
 */
object Secrets {
    // No l, I, O, 0 or 1: a password is sometimes read off one screen and typed on another.
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!#$%&*+-=?@"
    private const val ALL = LOWER + UPPER + DIGITS + SYMBOLS

    /** [length] characters (8 at least, 64 at most), with a small letter, a capital, a digit and a symbol among them. */
    fun password(length: Int, random: Random): String {
        val n = length.coerceIn(8, 64)
        val chars = CharArray(n) { i ->
            val from = when (i) { 0 -> LOWER; 1 -> UPPER; 2 -> DIGITS; 3 -> SYMBOLS; else -> ALL }
            from[random.nextInt(from.length)]
        }
        // The four that were promised must not always come first.
        for (i in n - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val c = chars[i]; chars[i] = chars[j]; chars[j] = c
        }
        return String(chars)
    }

    /** A version 4 (random) UUID, lower case: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx, y one of 8, 9, a, b. */
    fun uuid(random: Random): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val out = StringBuilder(36)
        for ((i, b) in bytes.withIndex()) {
            if (i == 4 || i == 6 || i == 8 || i == 10) out.append('-')
            val v = b.toInt() and 0xFF
            out.append(HEX[v shr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private const val HEX = "0123456789abcdef"
}
