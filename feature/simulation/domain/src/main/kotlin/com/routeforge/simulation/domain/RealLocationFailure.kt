package com.routeforge.simulation.domain

import com.routeforge.coredomain.Error

sealed interface RealLocationFailure : Error {
    data object ProviderDisabled : RealLocationFailure

    data object PermissionDenied : RealLocationFailure
}
