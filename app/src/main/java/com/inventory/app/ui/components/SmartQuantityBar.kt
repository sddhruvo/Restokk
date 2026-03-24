package com.inventory.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors
import com.inventory.app.ui.theme.formSectionLabel
import com.inventory.app.ui.theme.statValue

enum class QuantityBarMode { STEPPER, CONFIRM_ONLY }

@Composable
fun SmartQuantityBar(
    quantity: Double,
    itemName: String,
    unitAbbreviation: String?,
    sourceHint: String,
    mode: QuantityBarMode,
    onQuantityChange: (Double) -> Unit,
    onConfirm: (Double) -> Unit,
    onExpand: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val step = if (quantity != quantity.toLong().toDouble()) 0.5 else 1.0
    val atMin = quantity <= 0.5
    val atMax = quantity >= 999.0

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f))
    ) {
        when (mode) {
            QuantityBarMode.STEPPER -> StepperContent(
                quantity = quantity,
                itemName = itemName,
                unitAbbreviation = unitAbbreviation,
                sourceHint = sourceHint,
                step = step,
                atMin = atMin,
                atMax = atMax,
                onQuantityChange = onQuantityChange,
                onConfirm = onConfirm
            )

            QuantityBarMode.CONFIRM_ONLY -> ConfirmOnlyContent(
                quantity = quantity,
                itemName = itemName,
                unitAbbreviation = unitAbbreviation,
                onExpand = onExpand,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun StepperContent(
    quantity: Double,
    itemName: String,
    unitAbbreviation: String?,
    sourceHint: String,
    step: Double,
    atMin: Boolean,
    atMax: Boolean,
    onQuantityChange: (Double) -> Unit,
    onConfirm: (Double) -> Unit
) {
    Column(modifier = Modifier.padding(Dimens.spacingLg)) {
        // Header row: item name + source hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.formSectionLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (sourceHint.isNotBlank()) {
                Text(
                    text = sourceHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Dimens.spacingSm)
                )
            }
        }

        // Stepper row: [ - ]  qty unit  [ + ]  [ ✓ ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Minus button
            Surface(
                shape = CircleShape,
                color = if (atMin) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (!atMin) Modifier.clickable {
                            onQuantityChange((quantity - step).coerceAtLeast(0.5))
                        } else Modifier
                    )
            ) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Remove,
                    inkIconRes = R.drawable.ic_ink_minus,
                    contentDescription = "Decrease",
                    modifier = Modifier
                        .padding(Dimens.spacingSm)
                        .alpha(if (atMin) 0.38f else 1f)
                )
            }

            // Quantity display — use actual value to avoid animation rounding issues
            Spacer(modifier = Modifier.width(Dimens.spacingLg))
            Text(
                text = quantity.formatQty(),
                style = MaterialTheme.typography.statValue
            )
            unitAbbreviation?.let {
                Text(
                    text = " $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Dimens.spacingXs)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.spacingLg))

            // Plus button
            Surface(
                shape = CircleShape,
                color = if (atMax) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (!atMax) Modifier.clickable {
                            onQuantityChange((quantity + step).coerceAtMost(999.0))
                        } else Modifier
                    )
            ) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Add,
                    inkIconRes = R.drawable.ic_ink_add,
                    contentDescription = "Increase",
                    modifier = Modifier
                        .padding(Dimens.spacingSm)
                        .alpha(if (atMax) 0.38f else 1f)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spacingLg))

            // Confirm button
            ThemedButton(
                onClick = { onConfirm(quantity) },
                modifier = Modifier.size(44.dp)
            ) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Check,
                    inkIconRes = R.drawable.ic_ink_check,
                    contentDescription = "Confirm",
                    modifier = Modifier.size(Dimens.iconSizeSm)
                )
            }
        }
    }
}

@Composable
private fun ConfirmOnlyContent(
    quantity: Double,
    itemName: String,
    unitAbbreviation: String?,
    onExpand: (() -> Unit)?,
    onConfirm: (Double) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand?.invoke() }
            .padding(Dimens.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append("Added ${quantity.formatQty()}")
                    unitAbbreviation?.let { append(" $it") }
                    append(" $itemName")
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Tap to adjust",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.appColors.statusInStock.copy(alpha = 0.15f),
            modifier = Modifier
                .size(36.dp)
                .clickable { onConfirm(quantity) }
        ) {
            ThemedIcon(
                materialIcon = Icons.Filled.Check,
                inkIconRes = R.drawable.ic_ink_check,
                contentDescription = "Confirm",
                tint = MaterialTheme.appColors.statusInStock,
                modifier = Modifier.padding(Dimens.spacingSm)
            )
        }
    }
}
