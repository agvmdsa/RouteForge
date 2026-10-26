package com.routeforge.corelink

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result

class FakeRedirectSource(
    private val answers: Map<String, Result<String?, DataError.Network>> = emptyMap(),
    private val answerForUnknownUrl: Result<String?, DataError.Network> = Result.Success(null),
) : RedirectSource {
    val requested = mutableListOf<String>()
    var lookingUpCalledBeforeFirstRequest: Boolean? = null
    var onFirstRequest: () -> Boolean = { true }

    override suspend fun locationOf(url: String): Result<String?, DataError.Network> {
        if (requested.isEmpty()) lookingUpCalledBeforeFirstRequest = onFirstRequest()
        requested += url
        return answers[url] ?: answerForUnknownUrl
    }
}
