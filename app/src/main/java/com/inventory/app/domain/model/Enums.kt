package com.inventory.app.domain.model

enum class ExpiryStatus {
    OK, EXPIRING_SOON, EXPIRED, NO_EXPIRY
}

enum class StockStatus {
    IN_STOCK, LOW_STOCK, OUT_OF_STOCK
}

enum class UsageType(val value: String, val label: String) {
    CONSUMED("consumed", "Consumed"),
    WASTED("wasted", "Wasted"),
    EXPIRED("expired", "Expired"),
    DONATED("donated", "Donated"),
    OTHER("other", "Other");

    companion object {
        fun fromValue(value: String): UsageType =
            entries.find { it.value == value } ?: OTHER
    }
}

enum class TemperatureZone(val value: String, val label: String) {
    FROZEN("frozen", "Frozen"),
    REFRIGERATED("refrigerated", "Refrigerated"),
    ROOM_TEMP("room_temp", "Room Temperature");

    companion object {
        fun fromValue(value: String?): TemperatureZone? =
            entries.find { it.value == value }
    }
}

enum class Priority(val value: Int, val label: String) {
    NORMAL(0, "Normal"),
    HIGH(1, "High"),
    URGENT(2, "Urgent");

    companion object {
        fun fromValue(value: Int): Priority =
            entries.find { it.value == value } ?: NORMAL
    }
}

enum class UnitType(val value: String, val label: String) {
    WEIGHT("weight", "Weight"),
    VOLUME("volume", "Volume"),
    COUNT("count", "Count"),
    LENGTH("length", "Length");

    companion object {
        fun fromValue(value: String?): UnitType? =
            entries.find { it.value == value }
    }
}
