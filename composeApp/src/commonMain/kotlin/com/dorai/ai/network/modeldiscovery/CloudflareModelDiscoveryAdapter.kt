package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.OpenAICompatibleGenericException
import com.dorai.ai.network.ServiceCredentials
import com.dorai.ai.network.isValidCloudflareAccountId
import com.dorai.ai.network.parseCloudflareModelSearchError
import com.dorai.ai.network.parseCloudflareModelSearchPage
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * Model discovery for Cloudflare Workers AI — `GET /accounts/{account_id}/ai/models/search`
 * (v4 envelope, paged). Ported verbatim from the Cloudflare branch of the legacy
 * `Requests.getOpenAICompatibleModels` listing path; pagination runs through
 * [fetchAllPages], which keeps the legacy 20-page cap.
 *
 * The account id is carried in `credentials.baseUrl` (raw 32-hex id or a dashboard URL
 * containing `/accounts/<id>/`); URL resolution and SSRF guards stay in
 * [DiscoveryHttp.resolveUrl]. [Service.Cloudflare] and [Service.CloudflareAIGateway] share
 * the same account-scoped search endpoint, so one adapter instance is registered per
 * service; the AI Gateway variant additionally sends the `cf-aig-gateway-id` header.
 */
class CloudflareModelDiscoveryAdapter(
    private val httpClient: HttpClient,
    override val service: Service = Service.Cloudflare,
) : ModelDiscoveryAdapter {

    override suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel> {
        val modelsUrl = service.modelsUrl
            ?: throw OpenAICompatibleGenericException("Models URL not configured for ${service.displayName}")
        val apiKey = DiscoveryHttp.apiKeyFor(service, credentials, required = true)

        // Cloudflare Workers AI uses the Cloudflare v4 envelope (result/result_info), not
        // the OpenAI-compatible { data: [...] } model-list response. Fetch all pages explicitly.
        if (credentials.baseUrl.isBlank()) {
            throw OpenAICompatibleGenericException("Cloudflare Account ID is required to discover Workers AI models.")
        }
        val accountId = credentials.baseUrl.trim()
            .substringAfter("/accounts/", missingDelimiterValue = credentials.baseUrl.trim())
            .substringBefore("/")
        if (!isValidCloudflareAccountId(accountId)) {
            throw OpenAICompatibleGenericException("Enter a valid 32-character Cloudflare Account ID.")
        }

        val models = fetchAllPages { page ->
            val pageUrl = "${DiscoveryHttp.resolveUrl(service, credentials, modelsUrl)}?page=$page&per_page=$PER_PAGE"
            val response = httpClient.get(pageUrl) {
                apiKey?.let { bearerAuth(it) }
                DiscoveryHttp.sessionHeaders(service, credentials, sessionId = null).forEach { (name, value) ->
                    header(name, value)
                }
                DiscoveryHttp.cloudflareGatewayHeader(service, credentials)?.let { (name, value) ->
                    header(name, value)
                }
            }
            if (!response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val detail = parseCloudflareModelSearchError(responseBody)
                    ?: "HTTP ${response.status.value}"
                throw OpenAICompatibleGenericException("Cloudflare model discovery failed: $detail")
            }
            try {
                val parsedPage = parseCloudflareModelSearchPage(response.bodyAsText())
                ProviderPage(
                    models = parsedPage.models.map { model -> DiscoveredModel(id = model.name ?: model.id) },
                    totalPages = parsedPage.totalPages,
                )
            } catch (e: Exception) {
                throw OpenAICompatibleGenericException(
                    e.message?.takeIf { it.isNotBlank() } ?: "Cloudflare model search returned invalid data.",
                    e,
                )
            }
        }
        if (models.isEmpty()) {
            throw OpenAICompatibleGenericException("${service.displayName} returned no models...")
        }
        return models
    }

    private companion object {
        /** Cloudflare's documented maximum page size for the model search endpoint. */
        const val PER_PAGE = 100
    }
}
