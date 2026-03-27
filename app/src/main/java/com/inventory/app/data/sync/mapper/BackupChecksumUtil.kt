package com.inventory.app.data.sync.mapper

import java.security.MessageDigest

/**
 * Computes a deterministic SHA-256 checksum of backup data
 * for integrity verification on restore.
 */
object BackupChecksumUtil {

    /**
     * Computes SHA-256 hash of all backup data maps.
     * Maps are sorted by key for determinism; lists are in their natural order.
     */
    fun computeChecksum(
        items: List<Map<String, Any?>>,
        shopping: List<Map<String, Any?>>,
        recipes: List<Map<String, Any?>>
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")

        // Process each collection in a fixed order
        digestCollection(digest, "items", items)
        digestCollection(digest, "shopping", shopping)
        digestCollection(digest, "recipes", recipes)

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun digestCollection(
        digest: MessageDigest,
        collectionName: String,
        documents: List<Map<String, Any?>>
    ) {
        digest.update(collectionName.toByteArray())
        digest.update(documents.size.toString().toByteArray())

        for (doc in documents) {
            // Sort keys for deterministic serialization
            for (key in doc.keys.sorted()) {
                digest.update(key.toByteArray())
                val value = doc[key]
                digest.update(serializeValue(value).toByteArray())
            }
        }
    }

    private fun serializeValue(value: Any?): String = when (value) {
        null -> "null"
        is Number -> value.toString()
        is Boolean -> value.toString()
        is String -> value
        else -> value.toString()
    }
}
