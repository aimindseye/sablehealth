package io.github.aimindseye.sablehealth.core

/** Identity selects hardware defaults; window size, not model, selects the screen layout. */
enum class DeviceProfile(
    val label: String,
    val letterNavigation: Boolean,
) {
    Q25("Zinwa Q25", false),
    TITAN2("Unihertz Titan 2", true),
    P26("Zinwa P26", true),
    KEYBOARD("Other keyboard phone", true),
    TOUCH("Touch phone", false),
    ;

    companion object {
        fun detect(
            manufacturer: String,
            model: String,
            device: String,
            hasQwerty: Boolean,
            hasDpad: Boolean,
        ): DeviceProfile {
            val names =
                listOf("$manufacturer$model", device)
                    .map { it.lowercase().filter(Char::isLetterOrDigit) }
            return when {
                names.any { "titan2" in it } -> TITAN2
                names.any { "q25" in it } -> Q25
                names.any { "zinwap26" in it || it == "p26" || "passport" in it } -> P26
                hasQwerty && !hasDpad -> KEYBOARD
                else -> TOUCH
            }
        }
    }
}
