package com.routeforge.coredomain.handoff

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

/** The value of a one-shot hand-off: the latest value sent waits until a receiver takes it, and taking it is what
 *  removes it. An equal value sent twice before it is taken is delivered once, because the slot is a `StateFlow`. */
internal class OneShotSlot<T : Any> {
    private val slot = MutableStateFlow<T?>(null)

    val pending: T? get() = slot.value

    val incoming: Flow<T> = slot.filterNotNull().onEach { slot.value = null }

    fun send(value: T) {
        slot.value = value
    }
}
