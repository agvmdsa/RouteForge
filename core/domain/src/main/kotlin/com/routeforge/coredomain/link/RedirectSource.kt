package com.routeforge.coredomain.link

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result

/** Where an address sends the caller on, learnt from the answer's headers alone. */
interface RedirectSource {
    /** The address [url] redirects to, or null when it does not redirect. The answer's body is never read. */
    suspend fun locationOf(url: String): Result<String?, DataError.Network>
}
