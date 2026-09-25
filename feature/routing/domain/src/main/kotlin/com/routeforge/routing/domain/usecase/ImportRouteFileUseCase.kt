package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat

class ImportRouteFileUseCase(
    private val codecs: Map<RouteFileFormat, RouteFileCodec>,
) {
    operator fun invoke(
        bytes: ByteArray,
        format: RouteFileFormat,
    ): Result<RouteDraft, RouteFileFailure> = requireCodec(format).parse(bytes)

    private fun requireCodec(format: RouteFileFormat): RouteFileCodec =
        codecs[format] ?: error("No RouteFileCodec registered for $format")
}
