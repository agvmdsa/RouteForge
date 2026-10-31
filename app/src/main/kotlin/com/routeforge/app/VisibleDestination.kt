package com.routeforge.app

/**
 * The destination whose screen is showing. The search is drawn over the tab it was opened from, so while it is [current] the
 * screen the user sees (and the bottom bar must follow) is [previous]; anywhere else it is [current] itself.
 */
internal fun <T : Any> visibleDestination(
    current: T?,
    previous: T?,
    isSearch: (T) -> Boolean,
): T? = if (current != null && isSearch(current)) previous else current
