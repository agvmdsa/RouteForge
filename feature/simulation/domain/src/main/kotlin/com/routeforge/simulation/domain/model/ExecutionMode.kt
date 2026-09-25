package com.routeforge.simulation.domain.model

/** FR-020/FR-021: governs whether/how playback automatically restarts after completing a route. */
sealed class ExecutionMode {
    data object Once : ExecutionMode()

    data class Times(
        val count: Int,
    ) : ExecutionMode()

    data object Loop : ExecutionMode()

    companion object {
        val DEFAULT: ExecutionMode = Once
    }
}
