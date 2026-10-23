package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class RouteFileTransfer(
    private val importRouteFile: ImportRouteFileUseCase,
    private val exportRouteFile: ExportRouteFileUseCase,
    private val draftWaypointsHolder: DraftWaypointsHolder,
) {
    internal fun importRoute(
        bytes: ByteArray,
        format: RouteFileFormat,
        state: MutableStateFlow<RouteRequestState>,
    ) {
        when (val result = importRouteFile(bytes, format)) {
            is Result.Success -> {
                state.update {
                    it.copy(
                        draft = result.data,
                        chosenMode = null,
                        route = null,
                        errorType = null,
                    )
                }
                draftWaypointsHolder.set(result.data.points)
            }
            is Result.Error -> state.update { it.copy(errorType = result.error.toRouteRequestError()) }
        }
    }

    internal fun exportRoute(
        format: RouteFileFormat,
        state: MutableStateFlow<RouteRequestState>,
    ): ByteArray? {
        val route = state.value.route ?: return null
        return exportRouteFile(route, format)
    }
}
