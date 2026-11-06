package com.dorai.ai.data

import com.dorai.ai.inference.LocalModel
import com.dorai.ai.inference.LocalRuntime
import com.dorai.ai.inference.LocalRuntimeId
import com.dorai.ai.inference.RuntimeAvailability

/**
 * How the gateway picks an engine for a turn. [MANUAL] is the current default and preserves
 * the existing configured-service ordering exactly.
 */
enum class RoutingMode {
    /** User's configured service order; local only when a local service is primary. */
    MANUAL,

    /** Prefer a local runtime when eligible; cloud otherwise. Pre-flight only. */
    LOCAL_FIRST,

    /** Cloud primary; local only as an explicit opt-in fallback when cloud is unavailable. */
    CLOUD_FIRST,

    /** Score each turn and pick local vs cloud per turn. Pre-flight only. */
    AUTOMATIC_HYBRID,

    /** Local runtime only. Fails closed — never routes to cloud. */
    LOCAL_ONLY,
}

/**
 * The outcome of one routing decision. Immutable, persisted with the turn, and surfaced in
 * the route badge so users always know which engine served them.
 */
data class RouteDecision(
    val mode: RoutingMode,
    /** The local runtime selected, if any. Null means cloud / no local preference. */
    val runtimeId: LocalRuntimeId?,
    /** The local model selected, if any. Null means cloud / no local preference. */
    val modelId: String?,
    /** The cloud service selected, if any. Null means local only / no cloud preference. */
    val service: Service?,
    /** True when a local engine was judged eligible for this turn. */
    val localEligible: Boolean,
    /** Human-readable reason for the decision, shown in the route badge. */
    val reason: String,
    /** True only when a cloud call would be made. [RoutingMode.LOCAL_ONLY] is always false. */
    val usesCloud: Boolean,
)

/**
 * All inputs to a local-eligibility check. Grouped so [RoutingPolicy.decide] keeps a
 * manageable signature and so the negotiator can be unit-tested in isolation.
 */
data class LocalEligibility(
    val availability: RuntimeAvailability,
    val contextTokens: Int,
    val maxContextTokens: Int,
    val hasAttachments: Boolean,
    val supportsVision: Boolean,
    val hasActiveSkill: Boolean,
    val usesMcp: Boolean,
    val usesEmailOrScheduling: Boolean,
    val requiredTools: List<String>,
    val modelSupportsTools: Boolean,
    val isAgenticFlow: Boolean,
    val thermalThrottled: Boolean,
    val isForeground: Boolean = true,
    val quotaExceeded: Boolean = false,
)

/** The result of a local-eligibility check. */
data class LocalEligibilityResult(
    val eligible: Boolean,
    val reason: String,
)

/**
 * Pure per-turn eligibility for local execution. No side effects, no network, no inference.
 */
class CapabilityNegotiator {
    fun isLocalEligible(
        context: LocalEligibility,
        localToolsAllowlist: Set<String> = emptySet(),
    ): LocalEligibilityResult {
        val allowlist = localToolsAllowlist

        // 1. Runtime must be present and ready.
        if (context.availability != RuntimeAvailability.AVAILABLE) {
            return LocalEligibilityResult(false, "Local runtime unavailable (${context.availability})")
        }

        // 2. Context must fit with headroom.
        if (context.contextTokens > context.maxContextTokens * 0.8) {
            return LocalEligibilityResult(false, "Prompt exceeds local context headroom")
        }

        // 3. No attachments unless the model supports vision.
        if (context.hasAttachments && !context.supportsVision) {
            return LocalEligibilityResult(false, "Attachments not supported by local model")
        }

        // 4. No active skill, MCP, email, or scheduling — these need cloud tooling.
        if (context.hasActiveSkill || context.usesMcp || context.usesEmailOrScheduling) {
            return LocalEligibilityResult(false, "Turn requires cloud-only tools")
        }

        // 5. Required tools must be in the local allowlist and the model must support tools.
        if (context.requiredTools.isNotEmpty()) {
            if (!context.modelSupportsTools) {
                return LocalEligibilityResult(false, "Local model does not support tool calling")
            }
            val disallowed = context.requiredTools.filter { it !in allowlist }
            if (disallowed.isNotEmpty()) {
                return LocalEligibilityResult(false, "Tools not allowed locally: ${disallowed.joinToString()}")
            }
        }

        // 6. Not a background/agentic flow.
        if (context.isAgenticFlow) {
            return LocalEligibilityResult(false, "Agentic flows are cloud-only")
        }

        // 7. Respect thermal/battery pressure.
        if (context.thermalThrottled) {
            return LocalEligibilityResult(false, "Device thermally throttled")
        }

        // 8. Foreground execution requirement.
        if (!context.isForeground) {
            return LocalEligibilityResult(false, "Local execution requires foreground application state")
        }

        // 9. Quota/rate constraints.
        if (context.quotaExceeded) {
            return LocalEligibilityResult(false, "Local execution quota exceeded")
        }

        return LocalEligibilityResult(true, "Local execution eligible")
    }
}

/**
 * Pure routing policy. Decides which engine serves a turn given the current mode and
 * eligibility. Does not perform inference, does not fall back mid-turn, and never silently
 * routes a local failure to cloud.
 *
 * This is intentionally side-effect free so it can be unit-tested in isolation. Wiring it
 * into [com.dorai.ai.data.RemoteDataRepository] is a later phase.
 */
class RoutingPolicy(
    private val negotiator: CapabilityNegotiator = CapabilityNegotiator(),
) {
    /**
     * Returns the [RouteDecision] for a turn. [localRuntime] and [localModel] are the
     * currently installed/available local engine and model (null when none). [configured]
     * is the user's ordered cloud services (existing gateway behavior owns their ordering).
     */
    fun decide(
        mode: RoutingMode,
        localRuntime: LocalRuntime?,
        localModel: LocalModel?,
        eligibility: LocalEligibility,
        configured: List<Service>,
    ): RouteDecision {
        val localEligible = if (localRuntime != null && localModel != null) {
            negotiator.isLocalEligible(eligibility)
        } else {
            LocalEligibilityResult(false, "No local runtime or model installed")
        }

        return when (mode) {
            RoutingMode.LOCAL_ONLY -> {
                // Fail closed: local only, never cloud.
                val rt = localRuntime
                val m = localModel
                if (localEligible.eligible && rt != null && m != null) {
                    RouteDecision(
                        mode = RoutingMode.LOCAL_ONLY,
                        runtimeId = rt.id,
                        modelId = m.id,
                        service = null,
                        localEligible = true,
                        reason = "Local Only: local model",
                        usesCloud = false,
                    )
                } else {
                    RouteDecision(
                        mode = RoutingMode.LOCAL_ONLY,
                        runtimeId = null,
                        modelId = null,
                        service = null,
                        localEligible = false,
                        reason = "Local Only: ${localEligible.reason}",
                        usesCloud = false,
                    )
                }
            }

            RoutingMode.MANUAL -> {
                // Preserve current behavior: no local override; the gateway's configured
                // service order rules. No local preference expressed here.
                RouteDecision(
                    mode = RoutingMode.MANUAL,
                    runtimeId = null,
                    modelId = null,
                    service = configured.firstOrNull(),
                    localEligible = false,
                    reason = "Manual: configured service order",
                    usesCloud = true,
                )
            }

            RoutingMode.LOCAL_FIRST -> {
                if (localEligible.eligible && localRuntime != null && localModel != null) {
                    RouteDecision(
                        mode = RoutingMode.LOCAL_FIRST,
                        runtimeId = localRuntime.id,
                        modelId = localModel.id,
                        service = null,
                        localEligible = true,
                        reason = "Local First: local model",
                        usesCloud = false,
                    )
                } else {
                    RouteDecision(
                        mode = RoutingMode.LOCAL_FIRST,
                        runtimeId = null,
                        modelId = null,
                        service = configured.firstOrNull(),
                        localEligible = false,
                        reason = if (localEligible.eligible) "Local First: local eligible but unavailable" else "Local First: ${localEligible.reason}",
                        usesCloud = true,
                    )
                }
            }

            RoutingMode.CLOUD_FIRST -> {
                RouteDecision(
                    mode = RoutingMode.CLOUD_FIRST,
                    runtimeId = null,
                    modelId = null,
                    service = configured.firstOrNull(),
                    localEligible = false,
                    reason = "Cloud First: cloud primary",
                    usesCloud = true,
                )
            }

            RoutingMode.AUTOMATIC_HYBRID -> {
                if (localEligible.eligible && localRuntime != null && localModel != null) {
                    RouteDecision(
                        mode = RoutingMode.AUTOMATIC_HYBRID,
                        runtimeId = localRuntime.id,
                        modelId = localModel.id,
                        service = null,
                        localEligible = true,
                        reason = "Hybrid: local model",
                        usesCloud = false,
                    )
                } else {
                    RouteDecision(
                        mode = RoutingMode.AUTOMATIC_HYBRID,
                        runtimeId = null,
                        modelId = null,
                        service = configured.firstOrNull(),
                        localEligible = false,
                        reason = if (localEligible.eligible) "Hybrid: local eligible but unavailable" else "Hybrid: ${localEligible.reason}",
                        usesCloud = true,
                    )
                }
            }
        }
    }
}

/** Default routing mode; preserves current behavior. */
val DefaultRoutingMode: RoutingMode get() = RoutingMode.MANUAL
