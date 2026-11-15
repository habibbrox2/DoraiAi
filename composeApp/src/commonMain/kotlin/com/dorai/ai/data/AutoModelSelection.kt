package com.dorai.ai.data

/**
 * "Auto" model selection.
 *
 * Auto is not a model alias: it selects every model present in the instance's Model List and
 * the request walks that list, falling back to the next model whenever the current one fails.
 * The user's own selection stays stored and is tried first, so Auto extends the selection
 * instead of replacing it.
 *
 * [ID] is a storage/UI marker only. It is never dispatched to a provider as a literal model
 * id — [RemoteDataRepository] resolves it to concrete model ids before sending a request.
 */
object AutoModelSelection {
    /** Marker for the "Auto" entry in the model picker; never sent to a provider. */
    const val ID: String = "__auto__"

    /** Short label for the "Auto" entry. Localized copy lives in `Res.string.chat_auto_model`. */
    const val DISPLAY_NAME: String = "Auto"

    /**
     * Order a request may run models in: the user's own selection first (Auto extends it,
     * never replaces it), then every other model in the Model List. With Auto off — or when
     * the provider has not exposed a list yet — only the stored selection is returned, so the
     * request path is unchanged.
     */
    fun ordered(selectedModelId: String, autoEnabled: Boolean, listedModelIds: List<String>): List<String> {
        val selected = selectedModelId.trim()
        if (!autoEnabled) return if (selected.isNotEmpty()) listOf(selected) else emptyList()
        val listed = listedModelIds.map { it.trim() }.filter { it.isNotEmpty() }
        if (listed.isEmpty()) return if (selected.isNotEmpty()) listOf(selected) else emptyList()
        return buildList {
            if (selected.isNotEmpty()) add(selected)
            listed.filter { it != selected }.forEach { add(it) }
        }
    }
}
