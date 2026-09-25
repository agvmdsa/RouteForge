package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.Route
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteFileFormat

class ExportRouteFileUseCase(
    private val codecs: Map<RouteFileFormat, RouteFileCodec>,
) {
    operator fun invoke(
        route: Route,
        format: RouteFileFormat,
    ): ByteArray = requireCodec(format).serialize(route)

    private fun requireCodec(format: RouteFileFormat): RouteFileCodec =
        codecs[format] ?: error("No RouteFileCodec registered for $format")
}
