package com.vaultspec.authenticator.ui.component

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import java.util.concurrent.ConcurrentHashMap

/** A bundled brand glyph: a single path in a 24×24 viewport plus the brand color. */
class BrandIcon(val slug: String, val color: Color, val path: Path)

/**
 * Offline brand icon pack (Simple Icons, CC0) stored under assets/brand_icons:
 * `index.tsv` maps slug → brand color and alternative names, `p/<slug>` holds the path data.
 * Lookups never touch the network.
 */
object BrandIconPack {
    private const val DIR = "brand_icons"
    const val VIEWPORT = 24f

    private class Index(val colors: Map<String, Int>, val names: Map<String, String>)

    @Volatile
    private var index: Index? = null
    private val paths = LruCache<String, Path>(128)
    private val resolved = ConcurrentHashMap<String, String>()

    /** Already-resolved icon for [issuer], or null if it hasn't been looked up yet. */
    fun cached(issuer: String): BrandIcon? {
        val idx = index ?: return null
        val slug = resolved[issuer]?.takeIf { it.isNotEmpty() } ?: return null
        val path = paths.get(slug) ?: return null
        return BrandIcon(slug, Color(idx.colors.getValue(slug)), path)
    }

    /** True once [issuer] has been looked up and no bundled icon matched. */
    fun isKnownMiss(issuer: String): Boolean = resolved[issuer] == ""

    /** Resolves [issuer] to a bundled icon. Reads assets — call off the main thread. */
    fun find(context: Context, issuer: String): BrandIcon? {
        val idx = loadIndex(context)
        val slug = resolved.getOrPut(issuer) { match(idx, issuer) ?: "" }
        if (slug.isEmpty()) return null
        val path = paths.get(slug) ?: runCatching {
            val data = context.assets.open("$DIR/p/$slug").bufferedReader().use { it.readText() }
            PathParser().parsePathString(data).toPath()
        }.getOrNull()?.also { paths.put(slug, it) } ?: return null
        return BrandIcon(slug, Color(idx.colors.getValue(slug)), path)
    }

    private fun loadIndex(context: Context): Index {
        index?.let { return it }
        synchronized(this) {
            index?.let { return it }
            val colors = HashMap<String, Int>(4096)
            val names = HashMap<String, String>(1024)
            context.assets.open("$DIR/index.tsv").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split('\t')
                    if (parts.size < 2) return@forEach
                    val slug = parts[0]
                    colors[slug] = (0xFF000000 or parts[1].toLong(16)).toInt()
                    parts.getOrNull(2)?.split('|')?.forEach { name ->
                        if (name.isNotEmpty()) names.putIfAbsent(name, slug)
                    }
                }
            }
            return Index(colors, names).also { index = it }
        }
    }

    private fun match(idx: Index, issuer: String): String? {
        fun lookup(key: String): String? = when {
            key.isEmpty() -> null
            key in idx.colors -> key
            else -> idx.names[key]
        }

        val lower = issuer.lowercase().trim()

        // 1. Whole issuer: "Proton Mail" → protonmail
        lookup(normalize(lower))?.let { return it }

        // 2. A domain inside the issuer: "accounts.google.com" → google
        DOMAIN.findAll(lower).forEach { m ->
            val labels = m.value.split('.').dropLast(1).filter { it !in GENERIC_LABELS }
            labels.asReversed().forEach { label -> lookup(normalize(label))?.let { return it } }
        }

        // 3. Leading words, longest first: "GitHub Enterprise (work)" → githubenterprise → github
        val words = lower.split(NON_ALNUM).filter { it.isNotEmpty() }
        for (n in (words.size - 1) downTo 1) {
            val key = words.take(n).joinToString("")
            if (key.length >= 3) lookup(key)?.let { return it }
        }
        return null
    }

    private fun normalize(text: String) = text.replace(NON_ALNUM, "")

    private val NON_ALNUM = Regex("[^a-z0-9]+")
    private val DOMAIN = Regex("[a-z0-9-]+(\\.[a-z0-9-]+)+")
    private val GENERIC_LABELS = setOf("www", "my", "app", "accounts", "account", "login", "auth", "id", "co", "com")
}
