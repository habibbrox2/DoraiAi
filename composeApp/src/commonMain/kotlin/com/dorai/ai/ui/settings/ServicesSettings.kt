@file:OptIn(ExperimentalMaterial3Api::class)

package com.dorai.ai.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dorai.ai.data.Service
import com.dorai.ai.formatFileSize
import com.dorai.ai.inference.DevicePerformance
import com.dorai.ai.inference.DownloadError
import com.dorai.ai.inference.LocalModel
import com.dorai.ai.inference.ModelImportError
import com.dorai.ai.inference.calculateDevicePerformance
import com.dorai.ai.inference.estimateGpuMemoryMb
import com.dorai.ai.network.dtos.SponsorsResponseDto
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiClearableTextField
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.WarningAmber
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.DoraiSlider
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.components.VerticalScrollbarForScroll
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.doraiAdaptiveCardSurface
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.icons.DragIndicator
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_delete_downloaded_model
import dorai.composeapp.generated.resources.a11y_open_on_github
import dorai.composeapp.generated.resources.ic_arrow_drop_down
import dorai.composeapp.generated.resources.litert_cancel
import dorai.composeapp.generated.resources.litert_context_size
import dorai.composeapp.generated.resources.litert_download
import dorai.composeapp.generated.resources.litert_error_checksum_mismatch
import dorai.composeapp.generated.resources.litert_error_download_incomplete
import dorai.composeapp.generated.resources.litert_error_import_failed
import dorai.composeapp.generated.resources.litert_error_import_invalid
import dorai.composeapp.generated.resources.litert_error_import_too_small
import dorai.composeapp.generated.resources.litert_error_network
import dorai.composeapp.generated.resources.litert_error_not_enough_disk_space
import dorai.composeapp.generated.resources.litert_free_space
import dorai.composeapp.generated.resources.litert_import
import dorai.composeapp.generated.resources.litert_import_description
import dorai.composeapp.generated.resources.litert_imported
import dorai.composeapp.generated.resources.litert_importing
import dorai.composeapp.generated.resources.litert_on_device_description
import dorai.composeapp.generated.resources.litert_performance_good
import dorai.composeapp.generated.resources.litert_performance_ok
import dorai.composeapp.generated.resources.litert_performance_poor
import dorai.composeapp.generated.resources.litert_recommended
import dorai.composeapp.generated.resources.litert_tool_support
import dorai.composeapp.generated.resources.logo
import dorai.composeapp.generated.resources.model_discovery_fetching
import dorai.composeapp.generated.resources.model_discovery_retry
import dorai.composeapp.generated.resources.services_all_connected
import dorai.composeapp.generated.resources.services_checking_connections
import dorai.composeapp.generated.resources.services_configured_many
import dorai.composeapp.generated.resources.services_configured_one
import dorai.composeapp.generated.resources.services_empty_state
import dorai.composeapp.generated.resources.services_group_custom
import dorai.composeapp.generated.resources.services_group_hosted
import dorai.composeapp.generated.resources.services_group_on_device
import dorai.composeapp.generated.resources.services_needs_attention_many
import dorai.composeapp.generated.resources.services_needs_attention_one
import dorai.composeapp.generated.resources.settings_add_service
import dorai.composeapp.generated.resources.settings_api_key_label
import dorai.composeapp.generated.resources.settings_api_key_optional_label
import dorai.composeapp.generated.resources.settings_base_url_label
import dorai.composeapp.generated.resources.settings_become_sponsor
import dorai.composeapp.generated.resources.settings_business_partnerships
import dorai.composeapp.generated.resources.settings_business_partnerships_description
import dorai.composeapp.generated.resources.settings_contact_sponsorship
import dorai.composeapp.generated.resources.settings_custom_model_hint
import dorai.composeapp.generated.resources.settings_custom_model_label
import dorai.composeapp.generated.resources.settings_free_tier_description
import dorai.composeapp.generated.resources.settings_free_tier_title
import dorai.composeapp.generated.resources.settings_gateway_name_hint
import dorai.composeapp.generated.resources.settings_gateway_name_label
import dorai.composeapp.generated.resources.settings_model_label
import dorai.composeapp.generated.resources.settings_open_app_settings
import dorai.composeapp.generated.resources.settings_openai_compatible_or_other_service
import dorai.composeapp.generated.resources.settings_openai_compatible_providers
import dorai.composeapp.generated.resources.settings_openai_compatible_setup_ollama
import dorai.composeapp.generated.resources.settings_remove_service
import dorai.composeapp.generated.resources.settings_reorder_content_description
import dorai.composeapp.generated.resources.settings_sign_in_copy_api_key_from
import dorai.composeapp.generated.resources.settings_sponsors
import dorai.composeapp.generated.resources.settings_status_checking
import dorai.composeapp.generated.resources.settings_status_connected
import dorai.composeapp.generated.resources.settings_status_error
import dorai.composeapp.generated.resources.settings_status_error_connection_failed
import dorai.composeapp.generated.resources.settings_status_error_invalid_key
import dorai.composeapp.generated.resources.settings_status_error_local_network
import dorai.composeapp.generated.resources.settings_status_error_quota_exhausted
import dorai.composeapp.generated.resources.settings_status_error_rate_limited
import dorai.composeapp.generated.resources.setup_api_key_from
import dorai.composeapp.generated.resources.setup_api_key_optional
import dorai.composeapp.generated.resources.setup_api_key_required
import dorai.composeapp.generated.resources.setup_cloudflare
import dorai.composeapp.generated.resources.setup_custom_endpoint
import dorai.composeapp.generated.resources.setup_no_key_required
import dorai.composeapp.generated.resources.setup_on_device
import dorai.composeapp.generated.resources.sponsors_collapse
import dorai.composeapp.generated.resources.sponsors_past
import dorai.composeapp.generated.resources.sponsors_show_all
import dorai.composeapp.generated.resources.sponsors_show_all_a11y
import dorai.composeapp.generated.resources.state_collapsed
import dorai.composeapp.generated.resources.state_expanded
import dorai.composeapp.generated.resources.status_checking
import dorai.composeapp.generated.resources.status_connected
import dorai.composeapp.generated.resources.status_connection_failed
import dorai.composeapp.generated.resources.status_error
import dorai.composeapp.generated.resources.status_invalid_api_key
import dorai.composeapp.generated.resources.status_network_blocked
import dorai.composeapp.generated.resources.status_not_checked
import dorai.composeapp.generated.resources.status_quota_exhausted
import dorai.composeapp.generated.resources.status_rate_limited
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

@Composable
internal fun FreeSettings(
    currentSponsors: ImmutableList<SponsorsResponseDto.Sponsor> = persistentListOf(),
    pastSponsors: ImmutableList<SponsorsResponseDto.Sponsor> = persistentListOf(),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = doraiAdaptiveCardColors(),
        border = doraiAdaptiveCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.settings_free_tier_title),
                style = DoraiTypography.titleMedium,
                color = AccentPrimary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.settings_free_tier_description),
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )

            Spacer(Modifier.height(12.dp))

            // Support area (Plan 5, Phase 3): the DORAI AI owner profile comes
            // first in both collapsed and expanded states, then a responsive
            // expand/collapse sponsor grid. Sponsorship and partnership calls
            // to action stay below the grid with configuration-owned links.
            SupportSection(
                currentSponsors = currentSponsors,
                pastSponsors = pastSponsors,
            )

            Spacer(Modifier.height(12.dp))

            val uriHandler = LocalUriHandler.current
            Button(
                onClick = {
                    uriHandler.openUri(SupportLinks.BECOME_SPONSOR)
                },
                Modifier
                    .align(CenterHorizontally)
                    .handCursor(),
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.settings_become_sponsor))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.settings_business_partnerships),
                style = DoraiTypography.labelLarge,
                color = LocalDesignColorTokens.current.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.settings_business_partnerships_description),
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )

            TextButton(
                onClick = {
                    uriHandler.openUri(SupportLinks.PARTNERSHIPS)
                },
                Modifier
                    .handCursor(),
            ) {
                Text(stringResource(Res.string.settings_contact_sponsorship))
            }
        }
    }
}

/**
 * Every external destination for the support area lives in one configuration
 * block so the composables never inline unverified URLs (Plan 5: links owned
 * by configuration).
 */
private object SupportLinks {
    /** Repository owner — the DORAI AI maintainer profile. */
    const val OWNER_PROFILE = "https://github.com/habibbrox2"

    /** Base for remote sponsor profiles, as before. */
    const val SPONSOR_PROFILE_BASE = "https://github.com/"

    /** Existing upstream sponsorship CTA, unchanged. */
    const val BECOME_SPONSOR = "https://github.com/sponsors/habibbrox2"

    /** Existing partnership contact, unchanged. */
    const val PARTNERSHIPS = "https://schubert-simon.de"
}

/**
 * One entry in the support area (Plan 5, Phase 3).
 *
 * The owner profile is synthesized from the repository identity and always
 * renders first; remote sponsors keep the current/past distinction from the
 * GitHub feed.
 */
@Immutable
internal data class SupportProfile(
    val id: String,
    val name: String,
    val subtitle: String?,
    val avatarUrl: String?,
    val destinationUrl: String?,
    val isOwnerProfile: Boolean,
    val isCurrentSponsor: Boolean,
)

/**
 * Owner profile first, then current sponsors, then past sponsors. Past entries
 * that are also current are dropped, mirroring the previously merged list.
 */
internal fun buildSupportProfiles(
    current: ImmutableList<SponsorsResponseDto.Sponsor>,
    past: ImmutableList<SponsorsResponseDto.Sponsor>,
): List<SupportProfile> {
    val owner = SupportProfile(
        id = "brox-dorai-owner",
        name = "DORAI AI",
        subtitle = "habibbrox2 · project maintainer",
        avatarUrl = null,
        destinationUrl = SupportLinks.OWNER_PROFILE,
        isOwnerProfile = true,
        isCurrentSponsor = false,
    )
    val seen = mutableSetOf(owner.id)
    val sponsors = buildList {
        current.forEach { sponsor ->
            if (seen.add(sponsor.username)) {
                add(
                    SupportProfile(
                        id = sponsor.username,
                        name = sponsor.username,
                        subtitle = null,
                        avatarUrl = sponsor.avatar,
                        destinationUrl = "${SupportLinks.SPONSOR_PROFILE_BASE}${sponsor.username}",
                        isOwnerProfile = false,
                        isCurrentSponsor = true,
                    ),
                )
            }
        }
        past.forEach { sponsor ->
            if (seen.add(sponsor.username)) {
                add(
                    SupportProfile(
                        id = sponsor.username,
                        name = sponsor.username,
                        subtitle = null,
                        avatarUrl = sponsor.avatar,
                        destinationUrl = "${SupportLinks.SPONSOR_PROFILE_BASE}${sponsor.username}",
                        isOwnerProfile = false,
                        isCurrentSponsor = false,
                    ),
                )
            }
        }
    }
    return listOf(owner) + sponsors
}

/**
 * The DORAI AI support profile (Plan 5, Phase 3). It uses the local branded
 * logo so it renders identically offline and links to the repository owner.
 */
@Composable
private fun OwnerProfileCard(profile: SupportProfile) {
    val uriHandler = LocalUriHandler.current
    val destination = profile.destinationUrl
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // A card with no approved link stays informational and must not
                // look tappable (Plan 5, Phase 3).
                if (destination != null) {
                    Modifier
                        .handCursor()
                        .clickable { uriHandler.openUri(destination) }
                        .semantics {
                            contentDescription =
                                "Open ${profile.name} profile, ${destination.removePrefix("https://")}"
                        }
                } else {
                    Modifier
                },
            ),
        colors = doraiAdaptiveCardColors(),
        border = doraiAdaptiveCardBorder(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                // Local branded avatar — no network dependency (Plan 5, Phase 3).
                Image(
                    imageVector = vectorResource(Res.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = DoraiTypography.titleMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                profile.subtitle?.let {
                    Text(
                        text = it,
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }
            }
            Text(
                text = SupportLinks.OWNER_PROFILE.removePrefix("https://"),
                style = DoraiTypography.labelSmall,
                color = LocalDesignColorTokens.current.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SponsorTile(profile: SupportProfile, tileWidth: Dp) {
    val uriHandler = LocalUriHandler.current
    val destination = profile.destinationUrl
    val openContentDescription = stringResource(Res.string.a11y_open_on_github, profile.name)
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .width(tileWidth)
            .clip(DoraiShapes.xs)
            .then(
                if (destination != null) {
                    Modifier
                        .clickable { uriHandler.openUri(destination) }
                        .handCursor()
                        .semantics {
                            // Link action is part of the name so screen readers
                            // announce where the tap goes (Plan 5, Phase 4).
                            contentDescription = openContentDescription
                        }
                } else {
                    Modifier
                },
            )
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.avatarUrl != null) {
                coil3.compose.AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Neutral initial when no avatar image exists (Plan 5, Phase 3).
                Text(
                    text = profile.name.take(1).uppercase(),
                    style = DoraiTypography.titleMedium,
                    color = AccentPrimary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = profile.name,
            style = DoraiTypography.labelSmall,
            color = LocalDesignColorTokens.current.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SponsorGrid(profiles: List<SupportProfile>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The tile width is sized for the username at the default font scale; without
        // scaling it, names are cut to three characters at the largest one.
        val tileWidth = 72.dp * LocalDensity.current.fontScale
        profiles.forEach { profile -> SponsorTile(profile = profile, tileWidth = tileWidth) }
    }
}

/**
 * Support area (Plan 5, Phase 3): the owner profile card renders first in both
 * collapsed and expanded states; the sponsor grid collapses by width — 2 tiles
 * on compact, 4 on medium, 6 on desktop — and past sponsors only appear when
 * expanded. The toggle announces how many profiles are hidden.
 */
@Composable
private fun SupportSection(
    currentSponsors: ImmutableList<SponsorsResponseDto.Sponsor>,
    pastSponsors: ImmutableList<SponsorsResponseDto.Sponsor>,
) {
    val profiles = remember(currentSponsors, pastSponsors) {
        buildSupportProfiles(currentSponsors, pastSponsors)
    }
    val owner = profiles.first { it.isOwnerProfile }
    val current = profiles.filter { !it.isOwnerProfile && it.isCurrentSponsor }
    val past = profiles.filter { !it.isOwnerProfile && !it.isCurrentSponsor }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Short transition for expand/collapse; keeps focus on the toggle
            // because the button node never leaves composition.
            .animateContentSize(),
    ) {
        OwnerProfileCard(profile = owner)

        if (current.isNotEmpty() || past.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val tileLimit = when {
                    maxWidth >= 900.dp -> 6
                    maxWidth >= 600.dp -> 4
                    else -> 2
                }
                val totalSponsors = current.size + past.size
                // Lists at or below the width limit render fully with no
                // redundant toggle; longer lists collapse to the limit and
                // reveal past sponsors only when expanded (Plan 5, acceptance).
                val needsToggle = totalSponsors > tileLimit
                val collapsedProfiles = (current + past).take(tileLimit)
                val hiddenSponsorCount = if (expanded || !needsToggle) {
                    0
                } else {
                    totalSponsors - collapsedProfiles.size
                }
                val collapseSponsorsLabel = stringResource(Res.string.sponsors_collapse)
                val showAllSponsorsA11yLabel = stringResource(Res.string.sponsors_show_all_a11y, hiddenSponsorCount)
                Column {
                    val mainProfiles = if (expanded) current else collapsedProfiles
                    if (mainProfiles.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.settings_sponsors),
                            style = DoraiTypography.labelLarge,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        SponsorGrid(profiles = mainProfiles)
                    }
                    if (expanded && past.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.sponsors_past),
                            style = DoraiTypography.labelLarge,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        SponsorGrid(profiles = past)
                    }
                    if (needsToggle || expanded) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier
                                .handCursor()
                                .semantics {
                                    // The hidden count is part of the label so
                                    // screen-reader users know the size of the
                                    // list they are expanding into.
                                    contentDescription = if (expanded) {
                                        collapseSponsorsLabel
                                    } else {
                                        showAllSponsorsA11yLabel
                                    }
                                },
                        ) {
                            Text(if (expanded) collapseSponsorsLabel else stringResource(Res.string.sponsors_show_all))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ServicesContent(uiState: SettingsUiState, actions: SettingsActions) {
    var showAddServiceSheet by remember { mutableStateOf(false) }

    // Configured services list
    val entries = uiState.configuredServices

    // Compact Services overview (Plan 5, Phase 1): configured count and any
    // service requiring attention, readable before any card is opened.
    if (entries.isEmpty()) {
        CollectionEmptyState(
            message = stringResource(Res.string.services_empty_state),
        )
        Spacer(Modifier.height(12.dp))
    } else {
        ServicesOverview(entries)
        Spacer(Modifier.height(12.dp))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        entries.forEach { entry ->
            ConfiguredServiceCardContent(
                entry = entry,
                isExpanded = uiState.expandedServiceId == entry.instanceId,
                onExpand = { actions.onExpandService(if (uiState.expandedServiceId == entry.instanceId) null else entry.instanceId) },
                onChangeApiKey = { apiKey -> actions.onChangeApiKey(entry.instanceId, apiKey) },
                onChangeBaseUrl = { baseUrl -> actions.onChangeBaseUrl(entry.instanceId, baseUrl) },
                onChangeDisplayName = { name -> actions.onChangeDisplayName(entry.instanceId, name) },
                onSelectModel = { modelId -> actions.onSelectModel(entry.instanceId, modelId) },
                onRetryFetchModels = { actions.onRetryFetchModels(entry.instanceId) },
                onToggleUseCustomModel = { use -> actions.onToggleUseCustomModel(entry.instanceId, use) },
                onChangeCustomModelId = { id -> actions.onChangeCustomModelId(entry.instanceId, id) },
                onRemove = { actions.onRemoveService(entry.instanceId) },
                isDragging = false,
                dragHandleModifier = null,
                localAvailableModels = uiState.localAvailableModels,
                localImportedModels = uiState.localImportedModels,
                totalDeviceMemoryBytes = uiState.totalDeviceMemoryBytes,
                localFreeSpaceBytes = uiState.localFreeSpaceBytes,
                localDownloadingModelId = uiState.localDownloadingModelId,
                localDownloadProgress = uiState.localDownloadProgress,
                localDownloadError = uiState.localDownloadError,
                localImportingFileName = uiState.localImportingFileName,
                localImportProgress = uiState.localImportProgress,
                localImportError = uiState.localImportError,
                onDownloadLocalModel = actions.onDownloadLocalModel,
                onCancelLocalModelDownload = actions.onCancelLocalModelDownload,
                onImportLocalModel = actions.onImportLocalModel,
                onCancelLocalModelImport = actions.onCancelLocalModelImport,
                onDeleteLocalModel = actions.onDeleteLocalModel,
                onChangeModelContextTokens = actions.onChangeModelContextTokens,
                modelContextTokens = uiState.modelContextTokens,
                onOpenAppPermissionSettings = actions.onOpenAppPermissionSettings,
                onRecheckLocalNetworkPermission = { actions.onRecheckLocalNetworkPermission(entry.instanceId) },
            )
        }

        if (uiState.availableServicesToAdd.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showAddServiceSheet = true }, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.settings_add_service))
            }
        }

        Spacer(Modifier.height(16.dp))
        FreeSettings(
            currentSponsors = uiState.currentSponsors,
            pastSponsors = uiState.pastSponsors,
        )
    }

    // Add service bottom sheet
    if (showAddServiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddServiceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val addServiceScrollState = rememberScrollState()
            Box {
                Column(modifier = Modifier.verticalScroll(addServiceScrollState).padding(16.dp)) {
                    val services = uiState.availableServicesToAdd
                    // Grouped discovery (Plan 5, Phase 2): hosted providers, then
                    // on-device, then custom endpoints — each entry summarizing
                    // what setup it needs before it is added.
                    val hosted = services.filter {
                        !it.isOnDevice && it !is Service.OpenAICompatible
                    }
                    val onDevice = services.filter { it.isOnDevice }
                    val custom = services.filter { it is Service.OpenAICompatible }
                    val totalItems = services.size
                    var itemCounter = 0
                    listOf(
                        stringResource(Res.string.services_group_hosted) to hosted,
                        stringResource(Res.string.services_group_on_device) to onDevice,
                        stringResource(Res.string.services_group_custom) to custom,
                    ).forEach { (title, group) ->
                        if (group.isEmpty()) return@forEach
                        ProviderGroupHeader(title)
                        group.forEach { service ->
                            val index = itemCounter++
                            ProviderRowItem(
                                service = service,
                                isFirst = index == 0,
                                isLast = index == totalItems - 1,
                                onClick = {
                                    actions.onAddService(service)
                                    showAddServiceSheet = false
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                VerticalScrollbarForScroll(
                    scrollState = addServiceScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
    }
}

/** Short setup requirement summary shown under each provider name (Plan 5, Phase 2). */
@Composable
private fun setupRequirement(service: Service): String = when {
    service.isOnDevice -> stringResource(Res.string.setup_on_device)
    service is Service.OpenAICompatible -> stringResource(Res.string.setup_custom_endpoint)
    service is Service.Cloudflare -> stringResource(Res.string.setup_cloudflare)
    service.requiresApiKey && service.apiKeyUrlDisplay != null -> stringResource(Res.string.setup_api_key_from, service.apiKeyUrlDisplay)
    service.requiresApiKey -> stringResource(Res.string.setup_api_key_required)
    service.supportsOptionalApiKey -> stringResource(Res.string.setup_api_key_optional)
    else -> stringResource(Res.string.setup_no_key_required)
}

@Composable
private fun ProviderGroupHeader(title: String) {
    Text(
        text = title,
        style = DoraiTypography.labelLarge,
        color = AccentPrimary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun ProviderRowItem(
    service: Service,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val itemShape = RoundedCornerShape(
        topStart = if (isFirst) 12.dp else 0.dp,
        topEnd = if (isFirst) 12.dp else 0.dp,
        bottomStart = if (isLast) 12.dp else 0.dp,
        bottomEnd = if (isLast) 12.dp else 0.dp,
    )
    val isSpecial = service.isOnDevice || service is Service.OpenAICompatible || service is Service.AtlasCloud || service is Service.Cloudflare
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .handCursor()
            .clickable { onClick() }
            .clip(itemShape)
            .background(CardFill, itemShape)
            .border(BorderStroke(1.dp, BorderSoft), itemShape)
            .semantics { role = Role.Button },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (isSpecial) {
                            Modifier.background(
                                color = SurfaceRaised,
                                shape = DoraiShapes.xs,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = vectorResource(service.icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = LocalDesignColorTokens.current.textPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.displayName,
                    style = DoraiTypography.bodyLarge,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Text(
                    // Requirement summary so nobody adds a service and only then
                    // discovers it needs a key or a base URL (Plan 5, Phase 2).
                    text = setupRequirement(service),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }
    }
}

/** True when a connection status represents a failure worth surfacing. */
private fun ConnectionStatus.needsAttention(): Boolean = when (this) {
    ConnectionStatus.Connected,
    ConnectionStatus.Checking,
    ConnectionStatus.Unknown,
    -> false

    else -> true
}

/**
 * Compact Services overview (Plan 5, Phase 1): how many services are configured
 * and whether any of them needs attention, visible without opening a card.
 */
@Composable
private fun ServicesOverview(entries: ImmutableList<ConfiguredServiceEntry>) {
    val attention = entries.count { it.connectionStatus.needsAttention() }
    val checking = entries.count { it.connectionStatus == ConnectionStatus.Checking }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = doraiAdaptiveCardColors(),
        border = doraiAdaptiveCardBorder(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entries.size.toString(),
                style = DoraiTypography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entries.size == 1) {
                        stringResource(Res.string.services_configured_one)
                    } else {
                        stringResource(Res.string.services_configured_many)
                    },
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.height(2.dp))
                StatusBadge(
                    label = when {
                        attention > 0 -> if (attention == 1) {
                            stringResource(Res.string.services_needs_attention_one)
                        } else {
                            stringResource(Res.string.services_needs_attention_many, attention)
                        }

                        checking > 0 -> stringResource(Res.string.services_checking_connections)

                        else -> stringResource(Res.string.services_all_connected)
                    },
                    status = when {
                        attention > 0 -> OperationalStatus.Error
                        checking > 0 -> OperationalStatus.Connecting
                        else -> OperationalStatus.Connected
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfiguredServiceCardContent(
    entry: ConfiguredServiceEntry,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onChangeApiKey: (String) -> Unit,
    onChangeBaseUrl: (String) -> Unit,
    onChangeDisplayName: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onRetryFetchModels: () -> Unit = {},
    onToggleUseCustomModel: (Boolean) -> Unit = {},
    onChangeCustomModelId: (String) -> Unit = {},
    onRemove: () -> Unit,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier? = null,
    localAvailableModels: ImmutableList<LocalModel> = persistentListOf(),
    localImportedModels: ImmutableList<LocalModel> = persistentListOf(),
    totalDeviceMemoryBytes: Long = Long.MAX_VALUE,
    localFreeSpaceBytes: Long = 0L,
    localDownloadingModelId: String? = null,
    localDownloadProgress: Float? = null,
    localDownloadError: DownloadError? = null,
    localImportingFileName: String? = null,
    localImportProgress: Float? = null,
    localImportError: ModelImportError? = null,
    onDownloadLocalModel: (LocalModel) -> Unit = {},
    onCancelLocalModelDownload: () -> Unit = {},
    onImportLocalModel: (PlatformFile) -> Unit = {},
    onCancelLocalModelImport: () -> Unit = {},
    onDeleteLocalModel: (String) -> Unit = {},
    onChangeModelContextTokens: (String, Int) -> Unit = { _, _ -> },
    modelContextTokens: ImmutableMap<String, Int> = persistentMapOf(),
    onOpenAppPermissionSettings: () -> Unit = {},
    onRecheckLocalNetworkPermission: () -> Unit = {},
) {
    // Clear a stale denied status when the user returns from granting the permission in
    // system settings; the recheck never re-prompts, so this is a no-op while still denied.
    if (entry.connectionStatus == ConnectionStatus.ErrorLocalNetworkDenied) {
        LifecycleResumeEffect(entry.instanceId) {
            onRecheckLocalNetworkPermission()
            onPauseOrDispose { }
        }
    }
    val expandedStateDescription = stringResource(Res.string.state_expanded)
    val collapsedStateDescription = stringResource(Res.string.state_collapsed)
    Column(
        modifier = Modifier
            .doraiAdaptiveCardSurface()
            .fillMaxWidth()
            .clickable { onExpand() }
            .handCursor()
            .semantics {
                role = Role.Button
                liveRegion = LiveRegionMode.Polite
                stateDescription = if (isExpanded) {
                    expandedStateDescription
                } else {
                    collapsedStateDescription
                }
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Drag handle with a 48dp hit area (Plan 5, Phase 4); the glyph
                // itself stays 24dp so the header weight does not change.
                if (dragHandleModifier != null) {
                    Box(
                        modifier = dragHandleModifier
                            .minimumInteractiveComponentSize()
                            .handCursor(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragIndicator,
                            contentDescription = stringResource(Res.string.settings_reorder_content_description),
                            modifier = Modifier.size(24.dp),
                            tint = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Connection status badge (Plan 5, Phase 1): text alongside the
                // icon — color alone cannot carry status.
                val (statusLabel, statusKind) = when (entry.connectionStatus) {
                    ConnectionStatus.Unknown -> stringResource(Res.string.status_not_checked) to OperationalStatus.Disabled
                    ConnectionStatus.Checking -> stringResource(Res.string.status_checking) to OperationalStatus.Connecting
                    ConnectionStatus.Connected -> stringResource(Res.string.status_connected) to OperationalStatus.Connected
                    ConnectionStatus.ErrorInvalidKey -> stringResource(Res.string.status_invalid_api_key) to OperationalStatus.Error
                    ConnectionStatus.ErrorQuotaExhausted -> stringResource(Res.string.status_quota_exhausted) to OperationalStatus.NeedsSetup
                    ConnectionStatus.ErrorRateLimited -> stringResource(Res.string.status_rate_limited) to OperationalStatus.Error
                    ConnectionStatus.ErrorConnectionFailed -> stringResource(Res.string.status_connection_failed) to OperationalStatus.Error
                    ConnectionStatus.ErrorLocalNetworkDenied -> stringResource(Res.string.status_network_blocked) to OperationalStatus.Error
                    ConnectionStatus.Error -> stringResource(Res.string.status_error) to OperationalStatus.Error
                }
                StatusBadge(label = statusLabel, status = statusKind)

                Spacer(Modifier.width(12.dp))

                // Provider identity icon — consistent with the add-sheet, picker, and
                // free-suggestion surfaces which all show the provider's icon.
                Icon(
                    imageVector = vectorResource(entry.service.icon),
                    contentDescription = null, // decorative — the service name is adjacent
                    modifier = Modifier.size(20.dp),
                    tint = LocalDesignColorTokens.current.textSecondary,
                )

                Spacer(Modifier.width(8.dp))

                // Service name and model
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.customName.ifBlank { entry.service.displayName },
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    val displayModelId = when {
                        entry.useCustomModel && entry.customModelId.isNotBlank() -> entry.customModelId
                        entry.selectedModel != null -> entry.selectedModel.id
                        else -> null
                    }
                    if (displayModelId != null) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { Text(text = displayModelId, style = DoraiTypography.bodyMedium) },
                            state = rememberTooltipState(),
                        ) {
                            Text(
                                text = displayModelId,
                                style = DoraiTypography.bodySmall,
                                color = LocalDesignColorTokens.current.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Expand/collapse chevron
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }

        // Expanded content
        if (isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                if (entry.service.isOnDevice) {
                    LiteRTSettings(
                        selectedModel = entry.selectedModel,
                        downloadedModels = entry.models,
                        availableModels = localAvailableModels,
                        importedModels = localImportedModels,
                        totalDeviceMemoryBytes = totalDeviceMemoryBytes,
                        freeSpaceBytes = localFreeSpaceBytes,
                        downloadingModelId = localDownloadingModelId,
                        downloadProgress = localDownloadProgress,
                        downloadError = localDownloadError,
                        importingFileName = localImportingFileName,
                        importProgress = localImportProgress,
                        importError = localImportError,
                        onSelectModel = onSelectModel,
                        onDownloadModel = onDownloadLocalModel,
                        onCancelDownload = onCancelLocalModelDownload,
                        onImportModel = onImportLocalModel,
                        onCancelImport = onCancelLocalModelImport,
                        onDeleteModel = onDeleteLocalModel,
                        onChangeModelContextTokens = onChangeModelContextTokens,
                        modelContextTokens = modelContextTokens,
                    )
                } else if (entry.service is Service.OpenAICompatible || entry.service is Service.Cloudflare || entry.service.modelsUrl != null || entry.service.chatUrl.isNotBlank()) {
                    OpenAICompatibleSettings(
                        isCloudflare = entry.service is Service.Cloudflare,
                        displayName = entry.customName,
                        onChangeDisplayName = onChangeDisplayName,
                        baseUrl = entry.baseUrl,
                        onChangeBaseUrl = onChangeBaseUrl,
                        apiKey = entry.apiKey,
                        onChangeApiKey = onChangeApiKey,
                        selectedModel = entry.selectedModel,
                        models = entry.models,
                        onSelectModel = onSelectModel,
                        isFetchingModels = entry.isFetchingModels,
                        modelFetchError = entry.modelFetchError,
                        onRetryFetchModels = onRetryFetchModels,
                        useCustomModel = entry.useCustomModel,
                        customModelId = entry.customModelId,
                        onToggleUseCustomModel = onToggleUseCustomModel,
                        onChangeCustomModelId = onChangeCustomModelId,
                        connectionStatus = entry.connectionStatus,
                        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
                    )
                } else {
                    ServiceSettings(
                        apiKey = entry.apiKey,
                        onChangeApiKey = onChangeApiKey,
                        apiKeyUrl = entry.service.apiKeyUrl ?: "",
                        apiKeyUrlDisplay = entry.service.apiKeyUrlDisplay ?: "",
                        selectedModel = entry.selectedModel,
                        models = entry.models,
                        onSelectModel = onSelectModel,
                        connectionStatus = entry.connectionStatus,
                        isFetchingModels = entry.isFetchingModels,
                        modelFetchError = entry.modelFetchError,
                        onRetryFetchModels = onRetryFetchModels,
                        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Remove action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_remove_service),
                            color = DangerAlert,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceSettings(
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    apiKeyUrl: String,
    apiKeyUrlDisplay: String,
    selectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onSelectModel: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    isFetchingModels: Boolean = false,
    modelFetchError: String? = null,
    onRetryFetchModels: () -> Unit = {},
    testTag: String? = null,
    onOpenAppPermissionSettings: () -> Unit = {},
) {
    ApiKeyField(
        apiKey = apiKey,
        onChangeApiKey = onChangeApiKey,
        labelText = stringResource(Res.string.settings_api_key_label),
        testTag = testTag,
        isPassword = true,
    )

    Spacer(Modifier.height(8.dp))

    ConnectionStatusIndicator(connectionStatus, onOpenAppPermissionSettings)

    Spacer(Modifier.height(8.dp))

    val linkColor = AccentPrimary

    val copyApiKeyPromptString = stringResource(Res.string.settings_sign_in_copy_api_key_from)
    val annotatedString = remember(apiKeyUrl, apiKeyUrlDisplay) {
        buildAnnotatedString {
            append(copyApiKeyPromptString)
            append(" ")
            withLink(LinkAnnotation.Url(url = apiKeyUrl)) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append(apiKeyUrlDisplay)
                }
            }
        }
    }
    Text(
        annotatedString,
        modifier = Modifier.fillMaxWidth(),
        color = LocalDesignColorTokens.current.textPrimary,
    )

    Spacer(Modifier.height(16.dp))

    if (models.isNotEmpty()) {
        ModelFetchFeedback(
            isFetchingModels = isFetchingModels,
            error = modelFetchError,
            canRetry = apiKey.isNotBlank(),
            onRetry = onRetryFetchModels,
            showEmpty = false,
        )
    }
    if (connectionStatus == ConnectionStatus.Connected || models.isNotEmpty() || isFetchingModels || modelFetchError != null) {
        ModelSelection(
            currentSelectedModel = selectedModel,
            models = models,
            onClick = onSelectModel,
            isLoading = isFetchingModels,
            errorMessage = modelFetchError,
            onRetry = onRetryFetchModels,
            canRetry = apiKey.isNotBlank(),
        )
    }
}

@Composable
private fun ModelFetchFeedback(
    isFetchingModels: Boolean,
    error: String?,
    canRetry: Boolean,
    onRetry: () -> Unit,
    showEmpty: Boolean,
) {
    when {
        isFetchingModels -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(Res.string.model_discovery_fetching),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }

        error != null -> {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = error,
                    style = DoraiTypography.bodySmall,
                    color = DangerAlert,
                )
                TextButton(
                    onClick = onRetry,
                    enabled = canRetry,
                    modifier = Modifier.align(Alignment.End).handCursor(),
                ) {
                    Text(stringResource(Res.string.model_discovery_retry))
                }
            }
        }

        showEmpty -> {
            Text(
                text = "The provider is connected, but it returned no available models.",
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun OpenAICompatibleSettings(
    isCloudflare: Boolean = false,
    displayName: String = "",
    onChangeDisplayName: (String) -> Unit = {},
    baseUrl: String,
    onChangeBaseUrl: (String) -> Unit,
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    selectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onSelectModel: (String) -> Unit,
    useCustomModel: Boolean,
    customModelId: String,
    onToggleUseCustomModel: (Boolean) -> Unit,
    onChangeCustomModelId: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    isFetchingModels: Boolean = false,
    modelFetchError: String? = null,
    onRetryFetchModels: () -> Unit = {},
    onOpenAppPermissionSettings: () -> Unit = {},
) {
    if (!isCloudflare) {
        DoraiClearableTextField(
            value = displayName,
            onValueChange = onChangeDisplayName,
            label = {
                Text(
                    stringResource(Res.string.settings_gateway_name_label),
                    color = LocalDesignColorTokens.current.textPrimary,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )
        Text(
            text = stringResource(Res.string.settings_gateway_name_hint),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
    }

    DoraiClearableTextField(
        value = baseUrl,
        onValueChange = onChangeBaseUrl,
        label = {
            Text(
                if (isCloudflare) "Cloudflare Account ID" else stringResource(Res.string.settings_base_url_label),
                color = LocalDesignColorTokens.current.textPrimary,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
    )
    if (baseUrl.isNotBlank()) {
        val endpointPreview = if (isCloudflare) {
            "https://api.cloudflare.com/client/v4/accounts/${baseUrl.trim()}/ai/v1/chat/completions"
        } else {
            "${Service.normalizeOpenAICompatibleBaseUrl(baseUrl)}${Service.OpenAICompatible.chatUrl}"
        }
        Text(
            text = endpointPreview,
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
    }

    Spacer(Modifier.height(8.dp))

    ApiKeyField(
        apiKey = apiKey,
        onChangeApiKey = onChangeApiKey,
        labelText = stringResource(Res.string.settings_api_key_optional_label),
        singleLine = true,
        isPassword = true,
    )

    Spacer(Modifier.height(8.dp))

    ConnectionStatusIndicator(connectionStatus, onOpenAppPermissionSettings)

    Spacer(Modifier.height(8.dp))

    val linkColor = AccentPrimary
    val setupOllamaText = stringResource(Res.string.settings_openai_compatible_setup_ollama)
    val orOtherServiceText = stringResource(Res.string.settings_openai_compatible_or_other_service)
    val providersText = stringResource(Res.string.settings_openai_compatible_providers)
    val annotatedString = remember(setupOllamaText, orOtherServiceText, providersText, linkColor) {
        buildAnnotatedString {
            append(setupOllamaText)
            append(" ")
            withLink(LinkAnnotation.Url(url = "https://github.com/ollama/ollama")) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append("github.com/ollama/ollama")
                }
            }
            append(" ")
            append(orOtherServiceText)
            append(" ")
            withLink(LinkAnnotation.Url(url = "https://docs.litellm.ai/docs/providers")) {
                withStyle(style = SpanStyle(color = linkColor)) {
                    append(providersText)
                }
            }
        }
    }
    Text(
        annotatedString,
        modifier = Modifier.fillMaxWidth(),
        color = LocalDesignColorTokens.current.textPrimary,
    )

    Spacer(Modifier.height(16.dp))

    if (models.isNotEmpty()) {
        ModelFetchFeedback(
            isFetchingModels = isFetchingModels,
            error = modelFetchError,
            canRetry = apiKey.isNotBlank() || !isCloudflare,
            onRetry = onRetryFetchModels,
            showEmpty = false,
        )
    }
    if (connectionStatus == ConnectionStatus.Connected || models.isNotEmpty() || isFetchingModels || modelFetchError != null) {
        ModelSelection(
            currentSelectedModel = selectedModel,
            models = models,
            onClick = onSelectModel,
            isLoading = isFetchingModels,
            errorMessage = modelFetchError,
            onRetry = onRetryFetchModels,
            canRetry = apiKey.isNotBlank() || !isCloudflare,
        )
        Spacer(Modifier.height(8.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleUseCustomModel(!useCustomModel) }
            .handCursor(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = useCustomModel,
            onCheckedChange = onToggleUseCustomModel,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.settings_custom_model_label),
            style = DoraiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textPrimary,
        )
    }

    if (useCustomModel) {
        Spacer(Modifier.height(8.dp))
        DoraiClearableTextField(
            value = customModelId,
            onValueChange = onChangeCustomModelId,
            label = {
                Text(
                    stringResource(Res.string.settings_model_label),
                    color = LocalDesignColorTokens.current.textPrimary,
                )
            },
            singleLine = true,
        )
        Text(
            text = stringResource(Res.string.settings_custom_model_hint),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        )
    }
}

@Composable
private fun LiteRTSettings(
    selectedModel: SettingsModel?,
    downloadedModels: ImmutableList<SettingsModel>,
    availableModels: ImmutableList<LocalModel>,
    importedModels: ImmutableList<LocalModel>,
    totalDeviceMemoryBytes: Long,
    freeSpaceBytes: Long,
    downloadingModelId: String?,
    downloadProgress: Float?,
    downloadError: DownloadError?,
    importingFileName: String?,
    importProgress: Float?,
    importError: ModelImportError?,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onCancelDownload: () -> Unit,
    onImportModel: (PlatformFile) -> Unit,
    onCancelImport: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onChangeModelContextTokens: (String, Int) -> Unit,
    modelContextTokens: ImmutableMap<String, Int>,
) {
    val downloadedIds = remember(downloadedModels) { downloadedModels.map { it.id }.toSet() }
    val isBusy = downloadingModelId != null || importingFileName != null
    val isPreview = LocalInspectionMode.current

    val filePickerLauncher = if (!isPreview) {
        rememberFilePickerLauncher(
            type = FileKitType.File(extensions = listOf("litertlm")),
        ) { file ->
            if (file != null) onImportModel(file)
        }
    } else {
        null
    }

    Text(
        text = stringResource(Res.string.litert_on_device_description),
        style = DoraiTypography.bodyMedium,
        color = LocalDesignColorTokens.current.textSecondary,
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = stringResource(Res.string.litert_tool_support),
        style = DoraiTypography.bodySmall,
        color = LocalDesignColorTokens.current.textSecondary,
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(Res.string.litert_import_description),
        style = DoraiTypography.bodySmall,
        color = LocalDesignColorTokens.current.textSecondary,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { filePickerLauncher?.launch() },
        modifier = Modifier.handCursor(),
        enabled = !isBusy && filePickerLauncher != null,
    ) {
        Text(stringResource(Res.string.litert_import))
    }

    if (importingFileName != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.litert_importing) + " $importingFileName",
            style = DoraiTypography.labelSmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )
        if (importProgress != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { importProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(importProgress * 100).toInt()}%",
                    style = DoraiTypography.labelSmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                TextButton(
                    onClick = onCancelImport,
                    modifier = Modifier.handCursor(),
                ) {
                    Text(
                        text = stringResource(Res.string.litert_cancel),
                        style = DoraiTypography.labelSmall,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }

    if (importError != null && importingFileName == null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (importError) {
                    ModelImportError.INVALID_EXTENSION -> Res.string.litert_error_import_invalid
                    ModelImportError.NOT_ENOUGH_DISK_SPACE -> Res.string.litert_error_not_enough_disk_space
                    ModelImportError.FILE_TOO_SMALL -> Res.string.litert_error_import_too_small
                    ModelImportError.COPY_FAILED -> Res.string.litert_error_import_failed
                    ModelImportError.CANCELLED -> Res.string.litert_error_import_failed
                },
            ),
            style = DoraiTypography.bodySmall,
            color = DangerAlert,
        )
    }

    Spacer(Modifier.height(12.dp))

    availableModels.forEach { model ->
        LocalModelCard(
            model = model,
            isDownloaded = model.id in downloadedIds,
            isSelected = selectedModel?.id == model.id,
            isDownloading = downloadingModelId == model.id,
            downloadProgress = downloadProgress,
            isBusy = isBusy,
            totalDeviceMemoryBytes = totalDeviceMemoryBytes,
            modelContextTokens = modelContextTokens,
            onSelectModel = onSelectModel,
            onDownloadModel = onDownloadModel,
            onCancelDownload = onCancelDownload,
            onDeleteModel = onDeleteModel,
            onChangeModelContextTokens = onChangeModelContextTokens,
            showImportedBadge = false,
        )
    }

    if (importedModels.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.litert_imported),
            style = DoraiTypography.titleSmall,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        importedModels.forEach { model ->
            LocalModelCard(
                model = model,
                isDownloaded = true,
                isSelected = selectedModel?.id == model.id,
                isDownloading = false,
                downloadProgress = null,
                isBusy = isBusy,
                totalDeviceMemoryBytes = totalDeviceMemoryBytes,
                modelContextTokens = modelContextTokens,
                onSelectModel = onSelectModel,
                onDownloadModel = onDownloadModel,
                onCancelDownload = onCancelDownload,
                onDeleteModel = onDeleteModel,
                onChangeModelContextTokens = onChangeModelContextTokens,
                showImportedBadge = true,
            )
        }
    }

    if (downloadError != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (downloadError) {
                    DownloadError.NOT_ENOUGH_DISK_SPACE -> Res.string.litert_error_not_enough_disk_space
                    DownloadError.NETWORK_ERROR -> Res.string.litert_error_network
                    DownloadError.DOWNLOAD_INCOMPLETE -> Res.string.litert_error_download_incomplete
                    DownloadError.CHECKSUM_MISMATCH -> Res.string.litert_error_checksum_mismatch
                },
            ),
            style = DoraiTypography.bodySmall,
            color = DangerAlert,
        )
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.litert_free_space, formatFileSize(freeSpaceBytes)),
        style = DoraiTypography.bodySmall,
        color = LocalDesignColorTokens.current.textSecondary,
    )
}

@Composable
private fun LocalModelCard(
    model: LocalModel,
    isDownloaded: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isBusy: Boolean,
    totalDeviceMemoryBytes: Long,
    modelContextTokens: ImmutableMap<String, Int>,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (LocalModel) -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onChangeModelContextTokens: (String, Int) -> Unit,
    showImportedBadge: Boolean,
) {
    val steps = (model.maxContextTokens - model.defaultContextTokens) / 1024
    val storedContextTokens = modelContextTokens[model.id] ?: model.defaultContextTokens
    var contextSliderValue by remember(storedContextTokens) {
        mutableStateOf(((storedContextTokens - model.defaultContextTokens) / 1024).toFloat())
    }
    val contextTokens = model.defaultContextTokens + (contextSliderValue.roundToInt() * 1024)
    val estimatedMemoryMb = estimateGpuMemoryMb(model, contextTokens)
    val performance = calculateDevicePerformance(totalDeviceMemoryBytes, estimatedMemoryMb)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(DoraiShapes.xs)
            .background(SurfaceRaised, DoraiShapes.xs)
            .border(BorderStroke(1.dp, BorderSoft), DoraiShapes.xs),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDownloaded) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectModel(model.id) },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            model.isRecommended ->
                                "${model.displayName} (${stringResource(Res.string.litert_recommended)})"

                            showImportedBadge ->
                                "${model.displayName} (${stringResource(Res.string.litert_imported)})"

                            else -> model.displayName
                        },
                        style = DoraiTypography.bodyLarge,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatFileSize(model.sizeBytes),
                            style = DoraiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                        Spacer(Modifier.width(8.dp))
                        DevicePerformanceLabel(performance)
                    }
                }
                if (isDownloaded) {
                    IconButton(
                        onClick = { onDeleteModel(model.id) },
                        modifier = Modifier.handCursor(),
                        enabled = !isBusy,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.a11y_delete_downloaded_model),
                            tint = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                } else if (!isDownloading) {
                    TextButton(
                        onClick = { onDownloadModel(model) },
                        modifier = Modifier.handCursor(),
                        enabled = !isBusy,
                    ) {
                        Text(stringResource(Res.string.litert_download))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.litert_context_size, "${contextTokens / 1024}K"),
                style = DoraiTypography.labelSmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
            // A model whose export tops out at its own default (LFM2.5) has nothing to
            // drag: a 0f..0f range divides by zero working out the thumb fraction. Show
            // the fixed size as a label and leave the slider out.
            if (steps > 0) {
                DoraiSlider(
                    value = contextSliderValue,
                    onValueChange = { contextSliderValue = it },
                    onValueChangeFinished = {
                        onChangeModelContextTokens(model.id, contextTokens)
                    },
                    valueRange = 0f..steps.toFloat(),
                    steps = (steps - 1).coerceAtLeast(0),
                )
            }
            if (isDownloading && downloadProgress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = DoraiTypography.labelSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    TextButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.litert_cancel),
                            style = DoraiTypography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicePerformanceLabel(performance: DevicePerformance) {
    when (performance) {
        DevicePerformance.GOOD -> Text(
            text = stringResource(Res.string.litert_performance_good),
            style = DoraiTypography.labelSmall,
            color = StatusColorConnected,
        )

        DevicePerformance.OK -> Text(
            text = stringResource(Res.string.litert_performance_ok),
            style = DoraiTypography.labelSmall,
            color = StatusColorChecking,
        )

        DevicePerformance.POOR -> Text(
            text = stringResource(Res.string.litert_performance_poor),
            style = DoraiTypography.labelSmall,
            color = StatusColorError,
        )
    }
}

@Composable
private fun ConnectionStatusIndicator(status: ConnectionStatus, onOpenAppPermissionSettings: () -> Unit = {}) {
    when (status) {
        ConnectionStatus.Unknown -> return

        ConnectionStatus.Checking -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_checking),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }

        ConnectionStatus.Connected -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AccentPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_connected),
                    style = DoraiTypography.bodySmall,
                    color = AccentPrimary,
                )
            }
        }

        ConnectionStatus.ErrorQuotaExhausted -> {
            val warningColor = WarningAmber
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = warningColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_status_error_quota_exhausted),
                    style = DoraiTypography.bodySmall,
                    color = warningColor,
                )
            }
        }

        ConnectionStatus.ErrorInvalidKey,
        ConnectionStatus.ErrorRateLimited,
        ConnectionStatus.ErrorConnectionFailed,
        ConnectionStatus.ErrorLocalNetworkDenied,
        ConnectionStatus.Error,
        -> {
            val errorMessage = when (status) {
                ConnectionStatus.ErrorInvalidKey -> stringResource(Res.string.settings_status_error_invalid_key)
                ConnectionStatus.ErrorRateLimited -> stringResource(Res.string.settings_status_error_rate_limited)
                ConnectionStatus.ErrorConnectionFailed -> stringResource(Res.string.settings_status_error_connection_failed)
                ConnectionStatus.ErrorLocalNetworkDenied -> stringResource(Res.string.settings_status_error_local_network)
                else -> stringResource(Res.string.settings_status_error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DangerAlert,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = DoraiTypography.bodySmall,
                    color = DangerAlert,
                )
            }
            if (status == ConnectionStatus.ErrorLocalNetworkDenied) {
                TextButton(
                    onClick = onOpenAppPermissionSettings,
                    modifier = Modifier.handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_open_app_settings))
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    apiKey: String,
    onChangeApiKey: (String) -> Unit,
    labelText: String,
    testTag: String? = null,
    singleLine: Boolean = false,
    isPassword: Boolean = true,
) {
    DoraiClearableTextField(
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        value = apiKey,
        onValueChange = onChangeApiKey,
        label = {
            Text(
                labelText,
                color = LocalDesignColorTokens.current.textPrimary,
            )
        },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
    )
}
