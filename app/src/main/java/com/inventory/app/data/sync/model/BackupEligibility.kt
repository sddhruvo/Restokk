package com.inventory.app.data.sync.model

/**
 * Represents whether a backup can be performed right now.
 */
sealed interface BackupEligibility {
    data object Eligible : BackupEligibility
    data object NotSignedIn : BackupEligibility
    data object AnonymousOnly : BackupEligibility
    data class RateLimited(val minutesRemaining: Int) : BackupEligibility
    data object NoNetwork : BackupEligibility
}
