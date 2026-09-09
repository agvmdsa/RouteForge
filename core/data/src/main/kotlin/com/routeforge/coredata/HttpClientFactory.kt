package com.routeforge.coredata

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

object HttpClientFactory {
    fun create(): HttpClient = HttpClient(OkHttp)
}
