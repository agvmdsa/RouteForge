package com.routeforge.coredomain.link

private const val MAX_CHARACTERS_READ = 10_000
private const val MAX_SEARCH_LINES = 3
private const val MAX_LINE_CHARACTERS = 200
private const val TRAILING_PUNCTUATION = ".,;:!?'\""

// A web link runs to the next white space; a geo: link must not be glued to the letter before it.
private val ADDRESS = Regex("""https?://\S+|(?<![\p{L}\p{N}])geo:\S+""", RegexOption.IGNORE_CASE)

/**
 * The pieces of a text another app shared: the addresses in it, the first line that is not an address (what the place is
 * called) and the first lines that are not addresses (what to search for when no address leads anywhere).
 */
data class SharedText(
    val addresses: List<String>,
    val name: String?,
    val searchText: String?,
) {
    companion object {
        /** Reads at most the first 10,000 characters of [text], however long it is. */
        fun parse(text: String): SharedText {
            val read = text.take(MAX_CHARACTERS_READ)
            val lines =
                read
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !ADDRESS.containsMatchIn(it) }
                    .toList()
            return SharedText(
                addresses = ADDRESS.findAll(read).map { withoutTrailingPunctuation(it.value) }.toList(),
                name = lines.firstOrNull()?.take(MAX_LINE_CHARACTERS),
                searchText =
                    lines
                        .take(MAX_SEARCH_LINES)
                        .joinToString(" ")
                        .take(MAX_LINE_CHARACTERS)
                        .trimEnd()
                        .ifEmpty { null },
            )
        }

        /** A closing bracket stays when it closes one inside the address, as in `geo:0,0?q=52.5,13.4(Label)`. */
        private fun withoutTrailingPunctuation(token: String): String {
            var end = token
            while (end.isNotEmpty()) {
                val last = end.last()
                val closesNothing = last == ')' && end.count { it == ')' } > end.count { it == '(' }
                if (last in TRAILING_PUNCTUATION || closesNothing) end = end.dropLast(1) else break
            }
            return end
        }
    }
}
