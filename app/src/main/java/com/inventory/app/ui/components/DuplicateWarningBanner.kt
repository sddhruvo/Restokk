package com.inventory.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.domain.model.ProductMatcher
import com.inventory.app.domain.model.SuggestedAction

/**
 * Reusable duplicate warning banner for ProductMatcher results.
 * Shows different UI based on the best match confidence tier:
 * - DEFINITE: Strong warning — "This item already exists"
 * - LIKELY: Subtle suggestion — "Similar item found"
 * - POSSIBLE: Expandable "Similar items" section (collapsed by default)
 */
@Composable
fun DuplicateWarningBanner(
    matches: List<ProductMatcher.MatchCandidate>,
    suggestedAction: SuggestedAction,
    onNavigateToItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val bestMatch = matches.firstOrNull()

    AnimatedVisibility(
        visible = bestMatch != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val match = bestMatch ?: return@AnimatedVisibility
        when (match.confidence) {
            ProductMatcher.MatchConfidence.DEFINITE -> {
                // Strong warning
                AppCard(modifier = modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Warning,
                            inkIconRes = R.drawable.ic_ink_warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "This item already exists",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "\"${match.itemName}\" is in your inventory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onNavigateToItem(match.itemId) }) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.Edit,
                                inkIconRes = R.drawable.ic_ink_edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Update")
                        }
                    }
                }
            }
            ProductMatcher.MatchConfidence.LIKELY -> {
                // Subtle inline suggestion
                AppCard(modifier = modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Info,
                            inkIconRes = R.drawable.ic_ink_info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Similar item found: \"${match.itemName}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onNavigateToItem(match.itemId) }) {
                            Text("View", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            ProductMatcher.MatchConfidence.POSSIBLE -> {
                // Expandable "Similar items" section
                var expanded by remember { mutableStateOf(false) }
                AppCard(modifier = modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Similar items (${matches.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            ThemedIcon(
                                materialIcon = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                inkIconRes = if (expanded) R.drawable.ic_ink_collapse else R.drawable.ic_ink_expand,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                matches.forEach { match ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToItem(match.itemId) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = match.itemName,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(match.score * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
