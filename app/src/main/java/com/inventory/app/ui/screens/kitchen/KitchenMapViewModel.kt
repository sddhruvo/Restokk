package com.inventory.app.ui.screens.kitchen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KitchenZone(
    val locationId: Long?,
    val name: String,
    val itemCount: Int,
    val items: List<ItemWithDetails>,
    val tintColor: Color,
    val icon: ImageVector,
    val hasShelfLines: Boolean = false
)

data class KitchenMapUiState(
    val zones: List<KitchenZone> = emptyList(),
    val totalItems: Int = 0,
    val isLoading: Boolean = true,
    val userPreference: String = "INVENTORY"
)

@HiltViewModel
class KitchenMapViewModel @Inject constructor(
    itemRepository: ItemRepository,
    storageLocationRepository: StorageLocationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        // Known kitchen zone names → visual theme
        private data class ZoneTheme(
            val tintColor: Color,
            val icon: ImageVector,
            val hasShelfLines: Boolean = false,
            val sortOrder: Int
        )

        private val KNOWN_ZONES = mapOf(
            "refrigerator" to ZoneTheme(
                tintColor = Color(0xFFE3F2FD),
                icon = Icons.Filled.Kitchen,
                hasShelfLines = true,
                sortOrder = 0
            ),
            "freezer" to ZoneTheme(
                tintColor = Color(0xFFE1F5FE),
                icon = Icons.Filled.AcUnit,
                sortOrder = 1
            ),
            "pantry" to ZoneTheme(
                tintColor = Color(0xFFFFECB3),
                icon = Icons.Filled.ViewModule,
                hasShelfLines = true,
                sortOrder = 2
            ),
            "counter" to ZoneTheme(
                tintColor = Color(0xFFECEFF1),
                icon = Icons.Filled.Countertops,
                sortOrder = 3
            ),
            "spice rack" to ZoneTheme(
                tintColor = Color(0xFFFBE9E7),
                icon = Icons.Filled.LocalFireDepartment,
                sortOrder = 4
            )
        )

        private val DEFAULT_THEME = ZoneTheme(
            tintColor = Color(0xFFECEFF1),
            icon = Icons.Filled.Inventory2,
            sortOrder = 100
        )
    }

    private val _preference = MutableStateFlow("INVENTORY")

    init {
        viewModelScope.launch {
            _preference.value = settingsRepository.getString(
                OnboardingViewModel.KEY_USER_PREFERENCE, "INVENTORY"
            )
        }
    }

    val uiState = combine(
        storageLocationRepository.getAllWithItemCount(),
        itemRepository.getAllActiveWithDetails(),
        _preference
    ) { locations, items, pref ->
        val itemsByLocationId = items.groupBy { it.item.storageLocationId }

        // Build a map of locationId → name for lookup
        val locationIdToName = locations.associate { it.id to it.name }

        // Build zones from locations that have items or are known kitchen areas
        val zones = mutableListOf<KitchenZone>()

        // First: known kitchen zones in order
        val knownLocationIds = mutableSetOf<Long>()
        for (loc in locations) {
            val theme = KNOWN_ZONES[loc.name.lowercase().trim()]
            if (theme != null) {
                knownLocationIds.add(loc.id)
                val zoneItems = itemsByLocationId[loc.id] ?: emptyList()
                zones.add(
                    KitchenZone(
                        locationId = loc.id,
                        name = loc.name,
                        itemCount = zoneItems.size,
                        items = zoneItems,
                        tintColor = theme.tintColor,
                        icon = theme.icon,
                        hasShelfLines = theme.hasShelfLines
                    )
                )
            }
        }

        // Sort known zones by their defined sort order
        val sortedKnown = zones.sortedBy { zone ->
            KNOWN_ZONES[zone.name.lowercase().trim()]?.sortOrder ?: 99
        }
        zones.clear()
        zones.addAll(sortedKnown)

        // Second: other locations with items (not in known set)
        for (loc in locations) {
            if (loc.id !in knownLocationIds) {
                val zoneItems = itemsByLocationId[loc.id] ?: emptyList()
                if (zoneItems.isNotEmpty()) {
                    zones.add(
                        KitchenZone(
                            locationId = loc.id,
                            name = loc.name,
                            itemCount = zoneItems.size,
                            items = zoneItems,
                            tintColor = DEFAULT_THEME.tintColor,
                            icon = DEFAULT_THEME.icon,
                            hasShelfLines = DEFAULT_THEME.hasShelfLines
                        )
                    )
                }
            }
        }

        // Third: "Other" zone for items without a storage location
        val unlocatedItems = itemsByLocationId[null] ?: emptyList()
        if (unlocatedItems.isNotEmpty()) {
            zones.add(
                KitchenZone(
                    locationId = null,
                    name = "Other",
                    itemCount = unlocatedItems.size,
                    items = unlocatedItems,
                    tintColor = DEFAULT_THEME.tintColor,
                    icon = DEFAULT_THEME.icon
                )
            )
        }

        // Remove known zones that are completely empty (no items)
        val finalZones = zones.filter { it.itemCount > 0 || KNOWN_ZONES.containsKey(it.name.lowercase().trim()) }

        KitchenMapUiState(
            zones = finalZones,
            totalItems = items.size,
            isLoading = false,
            userPreference = pref
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KitchenMapUiState()
    )
}
