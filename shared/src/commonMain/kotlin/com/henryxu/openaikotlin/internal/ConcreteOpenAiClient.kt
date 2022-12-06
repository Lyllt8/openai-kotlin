package com.henryxu.openaikotlin.internal

import com.henryxu.openaikotlin.OpenAiApi
import com.henryxu.openaikotlin.OpenAiClient
import com.henryxu.openaikotlin.OpenAiClientBuilder
import com.henryxu.openaikotlin.Version
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal class ConcreteOpenAiClient(
    private val apiKey: String,
    version: Version,
    organization: String,
): OpenAiClient {
    override val api: OpenAiApi

    internal constructor(builder: OpenAiClientBuilder) : this(
        builder.apiKey,
        builder.version,
        builder.organization,
    )

    init {
        val client = HttpClient {
            expectSuccess = true

            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(apiKey, "")
                    }
                }
            }

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                })
            }
            install(Resources)

            defaultRequest {
                url(OpenAiConstants.BASE_URL + "/" + version.value + "/")
                contentType(ContentType.Application.Json)
                if (organization.isNotBlank()) {
                    header(OpenAiConstants.ORGANIZATION_HEADER, organization)
                }
            }
        }

        client.plugin(HttpSend).intercept { request ->
            execute(request)
            // TODO: allow hooks for intercepting
        }

        api = ConcreteOpenAiService(client = client)
    }
}
