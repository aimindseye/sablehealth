package io.github.aimindseye.sablehealth.core

enum class SetupEntry { FIRST_RUN, REVIEW_ALL, SELECTED_STEPS, PERMISSIONS_ONLY }

enum class SetupStage(
    val title: String,
) {
    WELCOME("Welcome"),
    SOURCES("Sources"),
    PERMISSIONS("Read permissions"),
    VERIFY("Verify"),
    PERSONALIZE("Personalize"),
    REVIEW("Review"),
}

object SetupPlan {
    fun stages(
        entry: SetupEntry,
        selected: Set<SetupStage>,
    ): List<SetupStage> =
        when (entry) {
            SetupEntry.FIRST_RUN, SetupEntry.REVIEW_ALL -> SetupStage.entries
            SetupEntry.PERMISSIONS_ONLY -> listOf(SetupStage.PERMISSIONS, SetupStage.VERIFY, SetupStage.REVIEW)
            SetupEntry.SELECTED_STEPS -> SetupStage.entries.filter { it in selected || it == SetupStage.REVIEW }
        }

    fun missing(
        selected: Set<String>,
        supported: Set<String>,
        granted: Set<String>,
    ): Set<String> = selected.intersect(supported) - granted

    fun next(
        current: Int,
        size: Int,
    ): Int = (current + 1).coerceAtMost(size - 1)

    fun previous(current: Int): Int = (current - 1).coerceAtLeast(0)
}
