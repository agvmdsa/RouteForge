package com.routeforge.coredata

import com.routeforge.coredomain.DataError
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HttpStatusErrorMappingTest {
    @Test
    fun `maps known status codes to their specific network error`() {
        assertEquals(DataError.Network.BAD_REQUEST, HttpStatusCode.BadRequest.toNetworkError())
        assertEquals(DataError.Network.UNAUTHORIZED, HttpStatusCode.Unauthorized.toNetworkError())
        assertEquals(DataError.Network.FORBIDDEN, HttpStatusCode.Forbidden.toNetworkError())
        assertEquals(DataError.Network.NOT_FOUND, HttpStatusCode.NotFound.toNetworkError())
        assertEquals(DataError.Network.REQUEST_TIMEOUT, HttpStatusCode.RequestTimeout.toNetworkError())
        assertEquals(DataError.Network.CONFLICT, HttpStatusCode.Conflict.toNetworkError())
        assertEquals(DataError.Network.PAYLOAD_TOO_LARGE, HttpStatusCode.PayloadTooLarge.toNetworkError())
        assertEquals(DataError.Network.TOO_MANY_REQUESTS, HttpStatusCode.TooManyRequests.toNetworkError())
        assertEquals(DataError.Network.SERVICE_UNAVAILABLE, HttpStatusCode.ServiceUnavailable.toNetworkError())
    }

    @Test
    fun `maps any other 5xx status to a server error`() {
        assertEquals(DataError.Network.SERVER_ERROR, HttpStatusCode.InternalServerError.toNetworkError())
        assertEquals(DataError.Network.SERVER_ERROR, HttpStatusCode.BadGateway.toNetworkError())
    }

    @Test
    fun `falls back to unknown for anything unmapped`() {
        assertEquals(DataError.Network.UNKNOWN, HttpStatusCode.PaymentRequired.toNetworkError())
    }
}
