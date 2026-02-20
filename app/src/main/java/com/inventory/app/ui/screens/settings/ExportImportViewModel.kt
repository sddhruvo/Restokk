package com.inventory.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.LocalDate
import javax.inject.Inject

data class ExportImportUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val exportedCount: Int = 0,
    val importedCount: Int = 0
)

@HiltViewModel
class ExportImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository,
    private val unitRepository: UnitRepository,
    private val database: InventoryDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportImportUiState())
    val uiState = _uiState.asStateFlow()

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            try {
                val items = withContext(Dispatchers.IO) {
                    itemRepository.getAllActiveWithDetails().first()
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                            writer.write("Name,Barcode,Brand,Category,Location,Quantity,Min Quantity,Unit,Purchase Price,Expiry Date,Notes,Smart Min Quantity")
                            writer.newLine()

                            items.forEach { item ->
                                writer.write(buildCsvRow(
                                    item.item.name,
                                    item.item.barcode ?: "",
                                    item.item.brand ?: "",
                                    item.category?.name ?: "",
                                    item.storageLocation?.name ?: "",
                                    item.item.quantity.toString(),
                                    item.item.minQuantity.toString(),
                                    item.unit?.abbreviation ?: "",
                                    item.item.purchasePrice?.toString() ?: "",
                                    item.item.expiryDate?.toString() ?: "",
                                    item.item.notes ?: "",
                                    item.item.smartMinQuantity.toString()
                                ))
                                writer.newLine()
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(isExporting = false, message = "Exported ${items.size} items", exportedCount = items.size)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, message = "Export failed: ${e.message}")
                }
            }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            try {
                var count = 0
                var skipped = 0
                val errors = mutableListOf<String>()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        reader.readLine() // skip header
                        val lines = reader.readLines()

                        database.withTransaction {
                            lines.forEachIndexed { index, line ->
                                val lineNum = index + 2 // +2 for header + 0-index
                                try {
                                    val fields = parseCsvLine(line)
                                    if (fields.size < 2) {
                                        errors.add("Row $lineNum: not enough columns")
                                        return@forEachIndexed
                                    }
                                    val name = fields[0].trim()
                                    if (name.isBlank()) {
                                        errors.add("Row $lineNum: empty name")
                                        return@forEachIndexed
                                    }

                                    // Duplicate detection
                                    if (itemRepository.findByName(name) != null) {
                                        skipped++
                                        return@forEachIndexed
                                    }

                                    // Resolve category
                                    val categoryName = fields.getOrNull(3)?.trim()
                                    val category = if (!categoryName.isNullOrBlank()) {
                                        categoryRepository.findCategoryByName(categoryName)
                                    } else null

                                    // Resolve location
                                    val locationName = fields.getOrNull(4)?.trim()
                                    val location = if (!locationName.isNullOrBlank()) {
                                        locationRepository.findByName(locationName)
                                    } else null

                                    // Resolve unit
                                    val unitAbbr = fields.getOrNull(7)?.trim()
                                    val unit = if (!unitAbbr.isNullOrBlank()) {
                                        unitRepository.findByAbbreviation(unitAbbr)
                                            ?: unitRepository.findByName(unitAbbr)
                                    } else null

                                    // Parse expiry date
                                    val expiryStr = fields.getOrNull(9)?.trim()
                                    val expiryDate = if (!expiryStr.isNullOrBlank()) {
                                        try { LocalDate.parse(expiryStr) } catch (_: Exception) { null }
                                    } else null

                                    val item = ItemEntity(
                                        name = name,
                                        barcode = fields.getOrNull(1)?.trim()?.ifBlank { null },
                                        brand = fields.getOrNull(2)?.trim()?.ifBlank { null },
                                        categoryId = category?.id,
                                        storageLocationId = location?.id,
                                        quantity = fields.getOrNull(5)?.toDoubleOrNull() ?: 1.0,
                                        minQuantity = fields.getOrNull(6)?.toDoubleOrNull() ?: 0.0,
                                        unitId = unit?.id,
                                        purchasePrice = fields.getOrNull(8)?.toDoubleOrNull(),
                                        expiryDate = expiryDate,
                                        notes = fields.getOrNull(10)?.trim()?.ifBlank { null },
                                        smartMinQuantity = fields.getOrNull(11)?.toDoubleOrNull() ?: 0.0
                                    )
                                    itemRepository.insert(item)
                                    count++
                                } catch (e: Exception) {
                                    errors.add("Row $lineNum: ${e.message}")
                                }
                            }
                        }
                    }
                }
                val msg = buildString {
                    append("Imported $count items")
                    if (skipped > 0) append(", $skipped duplicates skipped")
                    if (errors.isNotEmpty()) {
                        append(". ${errors.size} errors: ${errors.take(3).joinToString("; ")}")
                        if (errors.size > 3) append("...")
                    }
                }
                _uiState.update {
                    it.copy(isImporting = false, message = msg, importedCount = count)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, message = "Import failed: ${e.message}")
                }
            }
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            try {
                var count = 0
                var skipped = 0
                val errors = mutableListOf<String>()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val jsonText = reader.readText()
                        val jsonArray = JSONArray(jsonText)

                        database.withTransaction {
                            for (i in 0 until jsonArray.length()) {
                                try {
                                    val obj = jsonArray.getJSONObject(i)
                                    val name = obj.optString("name", "").trim()
                                    if (name.isBlank()) {
                                        errors.add("Item ${i + 1}: empty name")
                                        continue
                                    }

                                    // Duplicate detection
                                    if (itemRepository.findByName(name) != null) {
                                        skipped++
                                        continue
                                    }

                                    val categoryName = obj.optStringOrNull("category")
                                    val category = if (!categoryName.isNullOrBlank()) {
                                        categoryRepository.findCategoryByName(categoryName)
                                    } else null

                                    val locationName = obj.optStringOrNull("location")
                                    val location = if (!locationName.isNullOrBlank()) {
                                        locationRepository.findByName(locationName)
                                    } else null

                                    val unitAbbr = obj.optStringOrNull("unit")
                                    val unit = if (!unitAbbr.isNullOrBlank()) {
                                        unitRepository.findByAbbreviation(unitAbbr)
                                            ?: unitRepository.findByName(unitAbbr)
                                    } else null

                                    val expiryStr = obj.optStringOrNull("expiry_date")
                                    val expiryDate = if (!expiryStr.isNullOrBlank()) {
                                        try { LocalDate.parse(expiryStr) } catch (_: Exception) { null }
                                    } else null

                                    val item = ItemEntity(
                                        name = name,
                                        barcode = obj.optStringOrNull("barcode"),
                                        brand = obj.optStringOrNull("brand"),
                                        categoryId = category?.id,
                                        storageLocationId = location?.id,
                                        quantity = obj.optDouble("quantity", 1.0),
                                        minQuantity = obj.optDouble("min_quantity", 0.0),
                                        unitId = unit?.id,
                                        purchasePrice = if (obj.has("purchase_price") && !obj.isNull("purchase_price")) obj.optDouble("purchase_price") else null,
                                        expiryDate = expiryDate,
                                        notes = obj.optStringOrNull("notes"),
                                        smartMinQuantity = obj.optDouble("smart_min_quantity", 0.0)
                                    )
                                    itemRepository.insert(item)
                                    count++
                                } catch (e: Exception) {
                                    errors.add("Item ${i + 1}: ${e.message}")
                                }
                            }
                        }
                    }
                }
                val msg = buildString {
                    append("Imported $count items from JSON")
                    if (skipped > 0) append(", $skipped duplicates skipped")
                    if (errors.isNotEmpty()) {
                        append(". ${errors.size} errors: ${errors.take(3).joinToString("; ")}")
                        if (errors.size > 3) append("...")
                    }
                }
                _uiState.update {
                    it.copy(isImporting = false, message = msg, importedCount = count)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, message = "JSON import failed: ${e.message}")
                }
            }
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            try {
                val items = withContext(Dispatchers.IO) {
                    itemRepository.getAllActiveWithDetails().first()
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                            writer.write("[\n")
                            items.forEachIndexed { index, item ->
                                writer.write("  {\n")
                                writer.write("    \"name\": ${jsonEscape(item.item.name)},\n")
                                writer.write("    \"barcode\": ${jsonEscape(item.item.barcode)},\n")
                                writer.write("    \"brand\": ${jsonEscape(item.item.brand)},\n")
                                writer.write("    \"category\": ${jsonEscape(item.category?.name)},\n")
                                writer.write("    \"location\": ${jsonEscape(item.storageLocation?.name)},\n")
                                writer.write("    \"quantity\": ${item.item.quantity},\n")
                                writer.write("    \"min_quantity\": ${item.item.minQuantity},\n")
                                writer.write("    \"unit\": ${jsonEscape(item.unit?.abbreviation)},\n")
                                writer.write("    \"purchase_price\": ${item.item.purchasePrice ?: "null"},\n")
                                writer.write("    \"expiry_date\": ${jsonEscape(item.item.expiryDate?.toString())},\n")
                                writer.write("    \"notes\": ${jsonEscape(item.item.notes)},\n")
                                writer.write("    \"smart_min_quantity\": ${item.item.smartMinQuantity}\n")
                                writer.write("  }${if (index < items.size - 1) "," else ""}\n")
                            }
                            writer.write("]\n")
                        }
                    }
                }

                _uiState.update {
                    it.copy(isExporting = false, message = "Exported ${items.size} items as JSON", exportedCount = items.size)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, message = "Export failed: ${e.message}")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun buildCsvRow(vararg fields: String): String {
        return fields.joinToString(",") { field ->
            if (field.contains(',') || field.contains('"') || field.contains('\n')) {
                "\"${field.replace("\"", "\"\"")}\""
            } else {
                field
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    private fun jsonEscape(value: String?): String {
        if (value == null) return "null"
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return v.ifBlank { null }
    }
}
