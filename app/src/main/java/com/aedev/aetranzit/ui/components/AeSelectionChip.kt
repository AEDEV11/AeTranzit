package com.aedev.aetranzit.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TabularAmountStyle
import com.aedev.aetranzit.ui.theme.TabularLabelStyle

enum class AeChipTone {
    PRIMARY,
    PRIMARY_CONTAINER,
    DISCOUNT_TEAL
}

@Composable
fun AeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: AeChipTone = AeChipTone.PRIMARY,
    leadingIcon: ImageVector? = null,
    showCheckWhenSelected: Boolean = false,
    useTabularStyle: Boolean = false,
    emphasizeTabularNumbers: Boolean = false
) {
    val selectedContainerColor = when (tone) {
        AeChipTone.PRIMARY -> MaterialTheme.colorScheme.primary
        AeChipTone.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primaryContainer
        AeChipTone.DISCOUNT_TEAL -> MaterialTheme.colorScheme.secondaryContainer
    }
    val selectedContentColor = when (tone) {
        AeChipTone.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        AeChipTone.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.onPrimaryContainer
        AeChipTone.DISCOUNT_TEAL -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val selectedBorderColor = when (tone) {
        AeChipTone.PRIMARY -> MaterialTheme.colorScheme.primary
        AeChipTone.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primary
        AeChipTone.DISCOUNT_TEAL -> MaterialTheme.colorScheme.secondary
    }
    val borderWidth = if (selected && tone != AeChipTone.PRIMARY) {
        AeDimens.BorderWidthThick
    } else {
        AeDimens.BorderWidth
    }

    val resolvedLeadingIcon: (@Composable () -> Unit)? = when {
        leadingIcon != null -> {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(AeDimens.IconXs)
                )
            }
        }
        selected && showCheckWhenSelected -> {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(AeDimens.IconXs)
                )
            }
        }
        else -> null
    }

    val labelStyle = when {
        emphasizeTabularNumbers -> TabularAmountStyle.copy(
            fontSize = MaterialTheme.typography.labelLarge.fontSize,
            lineHeight = MaterialTheme.typography.labelLarge.lineHeight
        )
        useTabularStyle -> TabularLabelStyle.merge(
            MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
        )
        else -> MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        leadingIcon = resolvedLeadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = selectedContainerColor,
            selectedLabelColor = selectedContentColor,
            selectedLeadingIconColor = selectedContentColor,
            selectedTrailingIconColor = selectedContentColor
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = AeDimens.ZeroElevation,
            pressedElevation = AeDimens.ZeroElevation,
            focusedElevation = AeDimens.ZeroElevation,
            hoveredElevation = AeDimens.ZeroElevation,
            draggedElevation = AeDimens.ZeroElevation,
            disabledElevation = AeDimens.ZeroElevation
        ),
        border = BorderStroke(
            width = borderWidth,
            color = if (selected) selectedBorderColor else MaterialTheme.colorScheme.outline
        ),
        horizontalArrangement = Arrangement.spacedBy(
            AeDimens.ChipIconSpacing,
            Alignment.CenterHorizontally
        ),
        contentPadding = PaddingValues(
            horizontal = AeDimens.ChipHorizontalPadding,
            vertical = AeDimens.ChipVerticalPadding
        ),
        label = {
            Text(
                text = label,
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.heightIn(min = AeDimens.MinTouchTarget)
    )
}

@Preview(showBackground = true, name = "AeFilterChip - Light")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "AeFilterChip - Dark"
)
@Composable
private fun AeFilterChipPreview() {
    AeTranzitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AeDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AeDimens.SpaceSm),
                    verticalArrangement = Arrangement.spacedBy(AeDimens.SpaceXs)
                ) {
                    AeFilterChip(
                        label = "Today",
                        selected = false,
                        useTabularStyle = true,
                        onClick = {}
                    )
                    AeFilterChip(
                        label = "7 Days",
                        selected = true,
                        showCheckWhenSelected = true,
                        useTabularStyle = true,
                        onClick = {}
                    )
                    AeFilterChip(
                        label = "Dau (₱195.00)",
                        selected = true,
                        tone = AeChipTone.PRIMARY_CONTAINER,
                        showCheckWhenSelected = true,
                        useTabularStyle = true,
                        onClick = {}
                    )
                    AeFilterChip(
                        label = "Student (-20%)",
                        selected = true,
                        tone = AeChipTone.DISCOUNT_TEAL,
                        leadingIcon = Icons.Default.LocalOffer,
                        useTabularStyle = true,
                        onClick = {}
                    )
                    AeFilterChip(
                        label = "Conductor",
                        selected = false,
                        leadingIcon = Icons.Default.Badge,
                        onClick = {}
                    )
                }
            }
        }
    }
}
