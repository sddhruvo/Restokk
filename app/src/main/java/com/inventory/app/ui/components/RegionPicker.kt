package com.inventory.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.domain.model.RegionRegistry
import com.inventory.app.ui.screens.onboarding.RegionInfo
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay

/**
 * Shared region picker content — search + region list + custom country form.
 * Used by both onboarding and settings.
 */
@Composable
fun RegionPickerContent(
    regions: List<RegionInfo>,
    selectedRegionCode: String,
    onSelect: (RegionInfo) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Int = 360
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCustomForm by remember { mutableStateOf(false) }

    val filteredRegions = remember(searchQuery, regions) {
        if (searchQuery.isBlank()) regions
        else regions.filter { region ->
            region.countryName.contains(searchQuery, ignoreCase = true) ||
            region.countryCode.contains(searchQuery, ignoreCase = true) ||
            region.currencySymbol.contains(searchQuery, ignoreCase = true)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Search field
            ThemedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    showCustomForm = false
                },
                placeholder = { Text("Search countries...") },
                leadingIcon = {
                    ThemedIcon(
                        materialIcon = Icons.Filled.Search,
                        inkIconRes = R.drawable.ic_ink_search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            AnimatedContent(
                targetState = showCustomForm,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(PaperInkMotion.DurationShort))
                },
                label = "pickerContent"
            ) { customFormVisible ->
                if (customFormVisible) {
                    CustomCountryForm(
                        initialCountryName = searchQuery,
                        onConfirm = { name, symbol ->
                            val customConfig = RegionRegistry.custom(name.trim(), symbol.trim())
                            onSelect(RegionInfo.fromConfig(customConfig))
                        }
                    )
                } else {
                    Column {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(filteredRegions, key = { it.countryCode }) { region ->
                                val isSelected = region.countryCode == selectedRegionCode
                                ListItem(
                                    headlineContent = {
                                        Text("${region.flag}  ${region.countryName}")
                                    },
                                    supportingContent = {
                                        Text("${region.currencySymbol} \u2022 ${region.dateFormatPreview}")
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            ThemedIcon(
                                                materialIcon = Icons.Filled.Check,
                                                inkIconRes = R.drawable.ic_ink_check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { onSelect(region) }
                                )
                            }

                            // "Can't find your country?" option
                            if (searchQuery.isNotBlank() && filteredRegions.size <= 3) {
                                item(key = "custom_country_option") {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                "Can't find your country?",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        supportingContent = {
                                            Text("Set up a custom region")
                                        },
                                        leadingContent = {
                                            ThemedIcon(
                                                materialIcon = Icons.Filled.Language,
                                                inkIconRes = R.drawable.ic_ink_language,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable { showCustomForm = true }
                                    )
                                }
                            }
                        }

                        // Also show "Can't find?" as a text button when no search query
                        if (searchQuery.isBlank()) {
                            TextButton(
                                onClick = { showCustomForm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text("Can't find your country?")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomCountryForm(
    initialCountryName: String,
    onConfirm: (countryName: String, currencySymbol: String) -> Unit
) {
    var countryName by remember { mutableStateOf(initialCountryName) }
    var currencySymbol by remember { mutableStateOf("") }

    val datePreview = remember {
        RegionRegistry.formatDatePreview("CUSTOM")
    }

    // Fade-up entrance
    var formVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        formVisible = true
    }
    val formY by animateFloatAsState(
        targetValue = if (formVisible) 0f else 16f,
        animationSpec = PaperInkMotion.GentleSpring, label = "customFormY"
    )
    val formAlpha by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(250), label = "customFormAlpha"
    )

    val isValid = countryName.isNotBlank() && currencySymbol.isNotBlank()

    Column(
        modifier = Modifier
            .graphicsLayer { translationY = formY; alpha = formAlpha }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your country isn't in our list yet — no worries! Just tell us a few basics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        ThemedTextField(
            value = countryName,
            onValueChange = { countryName = it },
            label = { Text("Country name") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ThemedTextField(
            value = currencySymbol,
            onValueChange = { if (it.length <= 5) currencySymbol = it },
            label = { Text("Currency symbol") },
            placeholder = { Text("e.g. kr, \u20AC, \u00A5") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // Read-only date preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Date format",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$datePreview (from device)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        ThemedButton(
            onClick = { onConfirm(countryName, currencySymbol) },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Use This", style = MaterialTheme.typography.titleSmall)
        }
    }
}
