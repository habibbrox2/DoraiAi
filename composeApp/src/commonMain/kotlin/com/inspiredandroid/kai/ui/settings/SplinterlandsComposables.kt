package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.splinterlands.BattleLogEntry
import com.inspiredandroid.kai.splinterlands.BattlePhase
import com.inspiredandroid.kai.splinterlands.LlmServiceStatus
import com.inspiredandroid.kai.splinterlands.ModelStats
import com.inspiredandroid.kai.splinterlands.computeModelStats
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.BorderSoft
import com.inspiredandroid.kai.ui.CardFill
import com.inspiredandroid.kai.ui.DangerAlert
import com.inspiredandroid.kai.ui.KaiOutlinedTextField
import com.inspiredandroid.kai.ui.KaiShapes
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.SurfaceRaised
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForScroll
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.icons.DragIndicator
import com.inspiredandroid.kai.ui.icons.Visibility
import com.inspiredandroid.kai.ui.icons.VisibilityOff
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import sh.calvin.reorderable.ReorderableColumn
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun SplinterlandsSection(
    isEnabled: Boolean,
    accounts: ImmutableList<SplinterlandsAccountUiState>,
    instanceIds: ImmutableList<String>,
    addStatus: SplinterlandsAddStatus,
    battleLog: ImmutableList<BattleLogEntry>,
    availableServices: ImmutableList<com.inspiredandroid.kai.data.ServiceEntry>,
    onToggle: (Boolean) -> Unit,
    onTestAndAddAccount: (String, String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onReorderServices: (List<String>) -> Unit,
    onStartBattle: (String) -> Unit,
    onStopBattle: (String) -> Unit,
    onClearBattleLog: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleableHeadline(
            title = "Splinterlands",
            description = "Splinterlands is a blockchain-based trading card game. This experimental feature auto-battles Wild Ranked matches using an LLM to pick teams. Battle outcome heavily depends on the chosen model and its response speed (must respond within 180 seconds). Falls back to a simple greedy picker if the LLM fails.",
            checked = isEnabled,
            onCheckedChange = onToggle,
        )

        if (isEnabled) {
            Spacer(Modifier.height(12.dp))

            // Multi-service list
            SplinterlandsServiceList(
                instanceIds = instanceIds,
                availableServices = availableServices,
                onAddService = onAddService,
                onRemoveService = onRemoveService,
                onReorderServices = onReorderServices,
            )

            // Model Rankings (below services)
            val modelStats = remember(battleLog) { computeModelStats(battleLog).toImmutableList() }
            if (modelStats.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SplinterlandsModelRankings(modelStats)
            }

            // Account list
            if (accounts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                for (account in accounts) {
                    SplinterlandsAccountRow(
                        account = account,
                        hasServices = instanceIds.isNotEmpty(),
                        onRemove = { onRemoveAccount(account.accountId) },
                        onStartBattle = { onStartBattle(account.accountId) },
                        onStopBattle = { onStopBattle(account.accountId) },
                    )
                }
            }

            // Battle log
            if (battleLog.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BorderSoft)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent Battles",
                        style = KaiTypography.labelMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onClearBattleLog,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text("Clear", style = KaiTypography.labelSmall)
                    }
                }
                var showAll by remember { mutableStateOf(false) }
                val visibleEntries = if (showAll) battleLog else battleLog.take(5)
                for (entry in visibleEntries) {
                    SplinterlandsBattleLogRow(entry)
                }
                if (battleLog.size > 5 && !showAll) {
                    TextButton(
                        onClick = { showAll = true },
                        modifier = Modifier.handCursor(),
                    ) {
                        Text("Show more (${battleLog.size - 5})")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BorderSoft)
            Spacer(Modifier.height(12.dp))

            // Add account form
            SplinterlandsAddAccountForm(
                addStatus = addStatus,
                onTestAndAdd = onTestAndAddAccount,
            )
        }
    }
}

@Composable
private fun SplinterlandsServiceList(
    instanceIds: ImmutableList<String>,
    availableServices: ImmutableList<com.inspiredandroid.kai.data.ServiceEntry>,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onReorderServices: (List<String>) -> Unit,
) {
    val serviceMap = remember(availableServices) { availableServices.associateBy { it.instanceId } }

    if (instanceIds.isNotEmpty()) {
        Text(
            text = "LLM Services (priority order)",
            style = KaiTypography.labelMedium,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        ReorderableColumn(
            list = instanceIds,
            onSettle = { fromIndex, toIndex ->
                val reordered = instanceIds.toMutableList()
                reordered.add(toIndex, reordered.removeAt(fromIndex))
                onReorderServices(reordered)
            },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { index, id, _ ->
            key(id) {
                ReorderableItem {
                    val entry = serviceMap[id]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Drag handle
                        if (instanceIds.size >= 2) {
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = "Reorder",
                                modifier = Modifier.draggableHandle().handCursor(),
                                tint = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                        // Priority number
                        Text(
                            text = "${index + 1}.",
                            style = KaiTypography.labelMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                            modifier = Modifier.width(20.dp),
                        )
                        // Service icon
                        if (entry != null) {
                            Icon(
                                imageVector = org.jetbrains.compose.resources.vectorResource(entry.icon),
                                contentDescription = entry.serviceName,
                                modifier = Modifier.size(20.dp),
                                tint = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                        // Name + model
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry?.serviceName ?: id,
                                style = KaiTypography.bodySmall,
                                color = LocalDesignColorTokens.current.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (entry != null && entry.modelId.isNotBlank()) {
                                Text(
                                    text = entry.modelId,
                                    style = KaiTypography.bodySmall,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        // Remove button
                        IconButton(
                            onClick = { onRemoveService(id) },
                            modifier = Modifier.handCursor(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    // Add service dropdown
    val notYetAdded = remember(availableServices, instanceIds) {
        availableServices.filter { it.instanceId !in instanceIds }
    }
    if (notYetAdded.isNotEmpty()) {
        var showDropdown by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { showDropdown = true },
                modifier = Modifier.handCursor(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (instanceIds.isEmpty()) "Add Service" else "Add Another Service")
            }
            androidx.compose.material3.DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                for (service in notYetAdded) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Column {
                                Text(service.serviceName, style = KaiTypography.bodyMedium)
                                if (service.modelId.isNotBlank()) {
                                    Text(service.modelId, style = KaiTypography.bodySmall, color = LocalDesignColorTokens.current.textSecondary)
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = org.jetbrains.compose.resources.vectorResource(service.icon),
                                contentDescription = service.serviceName,
                                modifier = Modifier.size(20.dp),
                                tint = LocalDesignColorTokens.current.textSecondary,
                            )
                        },
                        onClick = {
                            onAddService(service.instanceId)
                            showDropdown = false
                        },
                        modifier = Modifier.handCursor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SplinterlandsModelRankings(modelStats: ImmutableList<ModelStats>) {
    Text(
        text = "Model Rankings",
        style = KaiTypography.labelMedium,
        color = LocalDesignColorTokens.current.textPrimary,
    )
    Spacer(Modifier.height(6.dp))
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = KaiShapes.sm,
        colors = CardDefaults.outlinedCardColors(
            containerColor = SurfaceRaised.copy(alpha = 0.3f),
        ),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            for ((index, stats) in modelStats.withIndex()) {
                if (index > 0) Spacer(Modifier.height(6.dp))
                SplinterlandsModelRow(index + 1, stats)
            }
        }
    }
}

@Composable
private fun SplinterlandsModelRow(rank: Int, stats: ModelStats) {
    val winPct = (stats.winRate * 100).toInt()
    val barColor = when {
        winPct >= 60 -> Color(0xFF4CAF50)
        winPct >= 40 -> AccentPrimary
        else -> DangerAlert
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "#$rank",
                style = KaiTypography.labelSmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
            Text(
                text = stats.modelName,
                style = KaiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${stats.wins}W ${stats.losses}L",
                style = KaiTypography.labelSmall,
                color = LocalDesignColorTokens.current.textSecondary,
                maxLines = 1,
            )
            Text(
                text = "$winPct%",
                style = KaiTypography.labelMedium,
                color = barColor,
                maxLines = 1,
            )
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
private fun SplinterlandsBattleLogRow(entry: BattleLogEntry) {
    var showActivity by remember { mutableStateOf(false) }
    val bgColor = if (entry.won) {
        CardFill.copy(alpha = 0.35f)
    } else {
        DangerAlert.copy(alpha = 0.35f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(KaiShapes.xs)
            .background(bgColor, KaiShapes.xs)
            .then(
                if (entry.activity.isNotEmpty()) {
                    Modifier.clickable { showActivity = true }.handCursor()
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // W/L badge
                Text(
                    text = if (entry.won) "Victory" else "Defeat",
                    style = KaiTypography.labelMedium,
                    color = if (entry.won) Color(0xFF4CAF50) else DangerAlert,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "vs ${entry.opponent}",
                    style = KaiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (entry.timestampMs > 0) {
                    val relTime = remember(entry.timestampMs) {
                        val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
                        formatRelativeTime(entry.timestampMs, nowMs)
                    }
                    Text(
                        text = relTime,
                        style = KaiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }
            }
            // Details row
            val details = buildList {
                if (entry.mana > 0) add("${entry.mana} mana")
                if (entry.rulesets.isNotBlank()) add(entry.rulesets)
            }
            if (details.isNotEmpty() || entry.llmPicked != null || entry.account.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val infoItems = buildList {
                        if (entry.account.isNotBlank()) add(entry.account)
                        addAll(details)
                    }
                    if (infoItems.isNotEmpty()) {
                        Text(
                            text = infoItems.joinToString(" · "),
                            style = KaiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (entry.llmPicked != null) {
                        val label = if (entry.llmPicked) entry.modelName.ifBlank { "LLM" } else "none"
                        Text(
                            text = label,
                            style = KaiTypography.labelSmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    if (showActivity) {
        SplinterlandsActivityDialog(entry) { showActivity = false }
    }
}

@Composable
private fun SplinterlandsActivityDialog(
    entry: BattleLogEntry,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battle Activity") },
        text = {
            val activityScrollState = rememberScrollState()
            Box {
                Column(modifier = Modifier.verticalScroll(activityScrollState)) {
                    for (line in entry.activity) {
                        Text(
                            text = line,
                            style = KaiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textPrimary,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
                VerticalScrollbarForScroll(
                    scrollState = activityScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        },
        confirmButton = {
            if (entry.battleId.isNotBlank()) {
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://splinterlands.com/battle/${entry.battleId}")
                    },
                    modifier = Modifier.handCursor(),
                ) { Text("View Battle") }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.handCursor(),
            ) { Text("Close") }
        },
    )
}

private fun formatRelativeTime(timestampMs: Long, nowMs: Long): String {
    val diffMs = nowMs - timestampMs
    val minutes = diffMs / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes == 1L -> "1 min"
        minutes < 60 -> "$minutes min"
        hours == 1L -> "1 hour"
        hours < 24 -> "$hours hours"
        days == 1L -> "1 day"
        else -> "$days days"
    }
}

@Composable
private fun SplinterlandsAddAccountForm(
    addStatus: SplinterlandsAddStatus,
    onTestAndAdd: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var postingKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    val isTesting = addStatus is SplinterlandsAddStatus.Testing

    // Collapse form after successful add
    LaunchedEffect(addStatus) {
        if (addStatus is SplinterlandsAddStatus.Idle) {
            if (expanded && username.isEmpty() && postingKey.isEmpty()) {
                expanded = false
            }
        }
    }

    if (!expanded) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.handCursor(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Account")
        }
    } else {
        Text(
            text = "Your posting key is stored securely on this device and is never sent to the LLM. It is only used to sign battle transactions on the Hive blockchain.",
            style = KaiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        KaiOutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Hive Username") },
            singleLine = true,
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        KaiOutlinedTextField(
            value = postingKey,
            onValueChange = { postingKey = it },
            label = { Text("Posting Key") },
            singleLine = true,
            enabled = !isTesting,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { showKey = !showKey },
                    modifier = Modifier.handCursor(),
                ) {
                    Icon(
                        imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showKey) "Hide" else "Show",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onTestAndAdd(username, postingKey)
                    username = ""
                    postingKey = ""
                },
                modifier = Modifier.handCursor(),
                enabled = username.isNotBlank() && postingKey.isNotBlank() && !isTesting,
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Text("Test & Add")
                }
            }
            OutlinedButton(
                onClick = {
                    expanded = false
                    username = ""
                    postingKey = ""
                },
                modifier = Modifier.handCursor(),
                enabled = !isTesting,
            ) {
                Text("Cancel")
            }
        }

        if (addStatus is SplinterlandsAddStatus.Error) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = addStatus.message,
                style = KaiTypography.bodySmall,
                color = DangerAlert,
            )
        }
    }
}

@Composable
private fun SplinterlandsAccountRow(
    account: SplinterlandsAccountUiState,
    hasServices: Boolean,
    onRemove: () -> Unit,
    onStartBattle: () -> Unit,
    onStopBattle: () -> Unit,
) {
    val bs = account.battleStatus
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Avatar
            if (account.avatarUrl.isNotBlank()) {
                coil3.compose.AsyncImage(
                    model = account.avatarUrl,
                    contentDescription = account.username,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CardFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = account.username.take(1).uppercase(),
                        style = KaiTypography.labelMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                }
            }

            // Username + energy
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = KaiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                if (account.energy >= 0) {
                    Text(
                        text = "\u26A1 ${account.energy}",
                        style = KaiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }
            }

            // Stats if any
            if (bs.wins > 0 || bs.losses > 0) {
                Text(
                    text = "${bs.wins}W ${bs.losses}L",
                    style = KaiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }

            // Error indicator
            if (bs.phase == BattlePhase.Error) {
                Text(
                    text = "Error",
                    style = KaiTypography.bodySmall,
                    color = DangerAlert,
                )
            }

            // Start/Stop button
            if (bs.isRunning) {
                OutlinedButton(
                    onClick = onStopBattle,
                    modifier = Modifier.handCursor(),
                    enabled = !bs.isStopping,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerAlert),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(if (bs.isStopping) "Stopping..." else "Stop", style = KaiTypography.bodySmall)
                }
            } else {
                Button(
                    onClick = onStartBattle,
                    modifier = Modifier.handCursor(),
                    enabled = hasServices,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("Start", style = KaiTypography.bodySmall)
                }
            }

            // Remove button with confirmation
            var showConfirm by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showConfirm = true },
                modifier = Modifier.handCursor(),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Remove Account") },
                    text = { Text("Remove ${account.username} from Splinterlands?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirm = false
                                onRemove()
                            },
                            modifier = Modifier.handCursor(),
                        ) {
                            Text("Remove", color = DangerAlert)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showConfirm = false },
                            modifier = Modifier.handCursor(),
                        ) { Text("Cancel") }
                    },
                )
            }
        }

        // Battle details (below player row)
        if (bs.isRunning) {
            val phaseText = when (bs.phase) {
                BattlePhase.LoggingIn -> "Logging in..."
                BattlePhase.CheckingEnergy -> "Checking energy..."
                BattlePhase.FindingMatch -> "Finding match..."
                BattlePhase.WaitingForOpponent -> "Waiting for opponent..."
                BattlePhase.FetchingCollection -> "Fetching cards..."
                BattlePhase.PickingTeam -> "Picking team..."
                BattlePhase.SubmittingTeam -> "Submitting team..."
                BattlePhase.WaitingForResult -> "Waiting for result..."
                BattlePhase.Finished -> "Done"
                else -> ""
            }

            // Match info row: opponent, mana, rulesets
            if (bs.currentOpponent.isNotBlank() || bs.currentMana > 0) {
                // FlowRow: three unweighted facts in a Row run off the right edge once
                // the text grows, taking the rulesets with them.
                FlowRow(
                    modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (bs.currentOpponent.isNotBlank()) {
                        Text(
                            text = "vs ${bs.currentOpponent}",
                            style = KaiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textPrimary,
                        )
                    }
                    if (bs.currentMana > 0) {
                        Text(
                            text = "${bs.currentMana} mana",
                            style = KaiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                    if (bs.currentRulesets.isNotBlank()) {
                        Text(
                            text = bs.currentRulesets,
                            style = KaiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Status row: phase + LLM indicator + timer
            Row(
                modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (phaseText.isNotBlank()) {
                    Text(
                        text = phaseText,
                        style = KaiTypography.bodySmall,
                        color = AccentPrimary,
                    )
                }
                val showLlmIndicator = bs.llmPickedTeam != null && bs.phase in setOf(
                    BattlePhase.SubmittingTeam,
                    BattlePhase.WaitingForResult,
                )
                if (showLlmIndicator) {
                    val label = if (bs.llmPickedTeam == true) bs.winningServiceName.ifBlank { "LLM" } else "Auto"
                    Text(
                        text = label,
                        style = KaiTypography.labelSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val showTimer = bs.teamDeadlineMs > 0L && bs.phase in setOf(
                    BattlePhase.FetchingCollection,
                    BattlePhase.PickingTeam,
                )
                if (showTimer) {
                    SplinterlandsCountdown(bs.teamDeadlineMs)
                }
            }

            // Per-service status rows during PickingTeam
            if (bs.phase == BattlePhase.PickingTeam && bs.serviceStatuses.isNotEmpty()) {
                for ((serviceId, status) in bs.serviceStatuses) {
                    Row(
                        modifier = Modifier.padding(start = 48.dp, top = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Status indicator
                        when (status) {
                            LlmServiceStatus.Querying -> CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = AccentPrimary,
                            )

                            LlmServiceStatus.ValidResponse -> Text("\u2714", style = KaiTypography.labelSmall, color = Color(0xFF4CAF50))

                            LlmServiceStatus.InvalidResponse -> Text("\u2718", style = KaiTypography.labelSmall, color = DangerAlert)

                            LlmServiceStatus.Failed -> Text("\u2718", style = KaiTypography.labelSmall, color = DangerAlert)

                            LlmServiceStatus.Selected -> Text("\u2605", style = KaiTypography.labelSmall, color = LocalDesignColorTokens.current.textSecondary)
                        }
                        Text(
                            text = serviceId,
                            style = KaiTypography.bodySmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Error message
        if (bs.errorMessage.isNotBlank() && bs.phase == BattlePhase.Error) {
            Text(
                text = bs.errorMessage,
                style = KaiTypography.bodySmall,
                color = DangerAlert,
                modifier = Modifier.padding(start = 40.dp),
            )
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
private fun SplinterlandsCountdown(deadlineMs: Long) {
    var remaining by remember { mutableStateOf(0L) }
    LaunchedEffect(deadlineMs) {
        if (deadlineMs > 0L) {
            while (true) {
                remaining = ((deadlineMs - kotlin.time.Clock.System.now().toEpochMilliseconds()) / 1000).coerceAtLeast(0L)
                kotlinx.coroutines.delay(1.seconds)
            }
        }
    }
    val mins = remaining / 60
    val secs = remaining % 60
    Text(
        text = "$mins:${secs.toString().padStart(2, '0')}",
        style = KaiTypography.labelMedium,
        color = if (remaining <= 30) DangerAlert else LocalDesignColorTokens.current.textSecondary,
    )
}
