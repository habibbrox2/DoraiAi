package com.dorai.ai.ui.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.components.LogoAnimation
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.logo
import dorai.composeapp.generated.resources.nav_brand_title
import dorai.composeapp.generated.resources.nav_main_navigation
import dorai.composeapp.generated.resources.nav_waiting_approval
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Adaptive scaffold for UI v2:
 * - [Compact]: Bottom bar with 4 items.
 * - [Medium] / [Expanded]: Navigation Rail (78dp).
 * - [Large]: Permanent Navigation Drawer (224dp).
 *
 * Automatically injects [LocalDoraiWindowWidthClass] for all nested composables.
 */
@Composable
fun DoraiAdaptiveScaffold(
    selectedDestination: DoraiDestination,
    onDestinationSelected: (DoraiDestination) -> Unit,
    waitingApprovalsCount: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalDoraiColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        val widthClass = calculateDoraiWindowWidthClass(maxWidth)

        CompositionLocalProvider(LocalDoraiWindowWidthClass provides widthClass) {
            when (widthClass) {
                DoraiWindowWidthClass.Compact -> {
                    val density = LocalDensity.current
                    val imeVisible = WindowInsets.ime.getBottom(density) > 0

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .imePadding(),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            content()
                        }
                        if (!imeVisible) {
                            DoraiBottomNavBar(
                                selectedDestination = selectedDestination,
                                onDestinationSelected = onDestinationSelected,
                                waitingApprovalsCount = waitingApprovalsCount,
                            )
                        }
                    }
                }

                DoraiWindowWidthClass.Medium, DoraiWindowWidthClass.Expanded -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .imePadding(),
                    ) {
                        DoraiNavRail(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = onDestinationSelected,
                            waitingApprovalsCount = waitingApprovalsCount,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            content()
                        }
                    }
                }

                DoraiWindowWidthClass.Large -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .imePadding(),
                    ) {
                        DoraiPermanentNavDrawer(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = onDestinationSelected,
                            waitingApprovalsCount = waitingApprovalsCount,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact bottom navigation bar (height ~64dp + navigationBars inset).
 */
@Composable
private fun DoraiBottomNavBar(
    selectedDestination: DoraiDestination,
    onDestinationSelected: (DoraiDestination) -> Unit,
    waitingApprovalsCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val navA11yLabel = stringResource(Res.string.nav_main_navigation)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(width = 1.dp, color = colors.line)
            .semantics { contentDescription = navA11yLabel },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = DoraiSpacing.SM),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DoraiDestination.entries.forEach { destination ->
                val isSelected = destination == selectedDestination
                val showBadge = destination == DoraiDestination.Workspace && waitingApprovalsCount > 0

                DoraiBottomNavItem(
                    destination = destination,
                    isSelected = isSelected,
                    showBadge = showBadge,
                    onClick = { onDestinationSelected(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun DoraiBottomNavItem(
    destination: DoraiDestination,
    isSelected: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val label = stringResource(destination.labelRes)
    val waitingApprovalLabel = stringResource(Res.string.nav_waiting_approval)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.26f) else Color.Transparent,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.primary else colors.muted,
                        modifier = Modifier.size(24.dp),
                    )
                }

                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.warn)
                            .border(2.dp, colors.surface, CircleShape)
                            .semantics { contentDescription = waitingApprovalLabel },
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                color = if (isSelected) colors.text else colors.muted,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Medium & Expanded 78dp Navigation Rail.
 */
@Composable
private fun DoraiNavRail(
    selectedDestination: DoraiDestination,
    onDestinationSelected: (DoraiDestination) -> Unit,
    waitingApprovalsCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val navA11yLabel = stringResource(Res.string.nav_main_navigation)

    Column(
        modifier = modifier
            .width(DoraiAdaptiveDefaults.NavRailWidth)
            .fillMaxHeight()
            .background(colors.surface)
            .border(width = 1.dp, color = colors.line)
            .padding(vertical = DoraiSpacing.MD, horizontal = DoraiSpacing.SM)
            .semantics { contentDescription = navA11yLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Brand logo mark (34dp)
        LogoAnimation(
            size = 34.dp,
            animate = true,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Spacer(modifier = Modifier.height(DoraiSpacing.MD))

        DoraiDestination.entries.forEach { destination ->
            val isSelected = destination == selectedDestination
            val showBadge = destination == DoraiDestination.Workspace && waitingApprovalsCount > 0

            DoraiRailNavItem(
                destination = destination,
                isSelected = isSelected,
                showBadge = showBadge,
                onClick = { onDestinationSelected(destination) },
            )
            Spacer(modifier = Modifier.height(DoraiSpacing.XS))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DoraiRailNavItem(
    destination: DoraiDestination,
    isSelected: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val label = stringResource(destination.labelRes)
    val waitingApprovalLabel = stringResource(Res.string.nav_waiting_approval)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.26f) else Color.Transparent,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.primary else colors.muted,
                        modifier = Modifier.size(24.dp),
                    )
                }

                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.warn)
                            .border(2.dp, colors.surface, CircleShape)
                            .semantics { contentDescription = waitingApprovalLabel },
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                color = if (isSelected) colors.text else colors.muted,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Large (>= 1200dp) 224dp Permanent Navigation Drawer.
 */
@Composable
private fun DoraiPermanentNavDrawer(
    selectedDestination: DoraiDestination,
    onDestinationSelected: (DoraiDestination) -> Unit,
    waitingApprovalsCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val navA11yLabel = stringResource(Res.string.nav_main_navigation)

    Column(
        modifier = modifier
            .width(DoraiAdaptiveDefaults.LargeDrawerWidth)
            .fillMaxHeight()
            .background(colors.surface)
            .border(width = 1.dp, color = colors.line)
            .padding(DoraiSpacing.MD)
            .semantics { contentDescription = navA11yLabel },
    ) {
        // Brand row with 34dp logo + DORAI AI wordmark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = DoraiSpacing.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            LogoAnimation(
                size = 34.dp,
                animate = true,
            )
            Spacer(modifier = Modifier.width(DoraiSpacing.MD))
            Text(
                text = stringResource(Res.string.nav_brand_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }

        DoraiDestination.entries.forEach { destination ->
            val isSelected = destination == selectedDestination
            val showBadge = destination == DoraiDestination.Workspace && waitingApprovalsCount > 0

            DoraiDrawerNavItem(
                destination = destination,
                isSelected = isSelected,
                showBadge = showBadge,
                onClick = { onDestinationSelected(destination) },
            )
            Spacer(modifier = Modifier.height(DoraiSpacing.XS))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DoraiDrawerNavItem(
    destination: DoraiDestination,
    isSelected: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val label = stringResource(destination.labelRes)
    val waitingApprovalLabel = stringResource(Res.string.nav_waiting_approval)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = if (isSelected) colors.primary else colors.muted,
            modifier = Modifier.size(22.dp),
        )

        Spacer(modifier = Modifier.width(DoraiSpacing.MD))

        Text(
            text = label,
            color = if (isSelected) colors.text else colors.muted,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.warn)
                    .border(2.dp, colors.surface, CircleShape)
                    .semantics { contentDescription = waitingApprovalLabel },
            )
        }
    }
}
