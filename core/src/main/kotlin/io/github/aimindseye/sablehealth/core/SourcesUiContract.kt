package io.github.aimindseye.sablehealth.core

/** These labels distinguish a user's statement from verified Health Connect data. */
object SourcesUiContract {
    const val IMPORTED_M0 = "None · M0"

    fun companionStatus(userConfirmed: Boolean): String {
        return if (userConfirmed) {
            "User-confirmed sync"
        } else {
            "Setup not confirmed in this app"
        }
    }

    const val EXPORT_STATUS = "Health Connect export unverified"
}
