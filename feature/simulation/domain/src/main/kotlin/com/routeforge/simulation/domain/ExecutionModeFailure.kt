package com.routeforge.simulation.domain

import com.routeforge.coredomain.Error

sealed interface ExecutionModeFailure : Error {
    data object NonPositiveCount : ExecutionModeFailure
}
