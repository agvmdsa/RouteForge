package com.routeforge.simulation.domain.model

/** FR-020/FR-021: governs whether/how playback automatically restarts after completing a route. */
sealed class ExecutionMode {
    data object Once : ExecutionMode()

    data class Times(
        val count: Int,
    ) : ExecutionMode()

    data object Loop : ExecutionMode()

    /** Reaching the end reverses direction and keeps moving back along the same path instead of
     *  teleporting back to the start — repeats indefinitely, same as [Loop]. */
    data object PingPong : ExecutionMode()

    companion object {
        val DEFAULT: ExecutionMode = Once
    }
}
