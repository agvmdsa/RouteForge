package com.routeforge.simulation.domain

import com.routeforge.coredomain.DomainError

sealed interface ExecutionModeFailure : DomainError {
    data object NonPositiveCount : ExecutionModeFailure
}
