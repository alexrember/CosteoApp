package com.mg.costeoapp.feature.sync.data

data class SyncResult(
    val success: Boolean,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val errors: List<String> = emptyList()
) {
    operator fun plus(other: SyncResult): SyncResult = SyncResult(
        success = success && other.success,
        pushedCount = pushedCount + other.pushedCount,
        pulledCount = pulledCount + other.pulledCount,
        errors = errors + other.errors
    )
}
