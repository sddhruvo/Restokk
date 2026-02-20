package com.inventory.app.data.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.ItemImageEntity
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.local.entity.SubcategoryEntity
import com.inventory.app.data.local.entity.UnitEntity

data class ItemWithDetails(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "subcategory_id", entityColumn = "id")
    val subcategory: SubcategoryEntity?,
    @Relation(parentColumn = "storage_location_id", entityColumn = "id")
    val storageLocation: StorageLocationEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id")
    val unit: UnitEntity?,
    @Relation(parentColumn = "id", entityColumn = "item_id")
    val images: List<ItemImageEntity>
)

data class CategoryWithSubcategories(
    @Embedded val category: CategoryEntity,
    @Relation(parentColumn = "id", entityColumn = "category_id")
    val subcategories: List<SubcategoryEntity>
)

data class CategoryWithItemCount(
    @Embedded val category: CategoryEntity,
    val itemCount: Int
)

data class LocationWithItemCount(
    @Embedded val location: StorageLocationEntity,
    val itemCount: Int
)

data class ShoppingListItemWithDetails(
    @Embedded val shoppingItem: ShoppingListItemEntity,
    @Relation(parentColumn = "item_id", entityColumn = "id")
    val item: ItemEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id")
    val unit: UnitEntity?
)

// Need to import the entity
private typealias ShoppingListItemEntity = com.inventory.app.data.local.entity.ShoppingListItemEntity
