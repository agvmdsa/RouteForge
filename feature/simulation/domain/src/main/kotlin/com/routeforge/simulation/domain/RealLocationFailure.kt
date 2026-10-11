package com.routeforge.simulation.domain

import com.routeforge.coredomain.DomainError

sealed interface RealLocationFailure : DomainError {
    data object ProviderDisabled : RealLocationFailure

    data object PermissionDenied : RealLocationFailure
}
