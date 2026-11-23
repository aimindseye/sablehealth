package io.github.aimindseye.sablehealth.core

/** A key is reduced to pure JVM data for fast, deterministic device-profile tests. */
data class Press(
    val code: Int,
    val repeatCount: Int = 0,
    val alt: Boolean = false,
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val editingText: Boolean = false,
)

enum class UiAction { UP, DOWN, LEFT, RIGHT, CONFIRM, BACK, NEXT_TAB, REFRESH, HELP }

object KeyboardMapping {
    // Android KeyEvent integer constants; kept independent of android.jar for JVM tests.
    private const val BACK = 4
    private const val UP = 19
    private const val DOWN = 20
    private const val LEFT = 21
    private const val RIGHT = 22
    private const val CENTER = 23
    private const val I = 37
    private const val J = 38
    private const val K = 39
    private const val L = 40
    private const val R = 46
    private const val T = 48
    private const val SPACE = 62
    private const val ENTER = 66
    private const val SLASH = 76
    private const val ESC = 111

    fun action(profile: DeviceProfile, press: Press): UiAction? {
        val hasModifier = press.alt || press.ctrl || press.meta
        val blocked = hasModifier || press.editingText
        val mapped = if (blocked) {
            null
        } else {
            dpadAction(press.code) ?: commandAction(press.code) ?: letterAction(profile, press.code)
        }
        // Repetition is for navigation only, not selection or side effects.
        return if (press.repeatCount > 0 && !isDirectional(mapped)) null else mapped
    }

    private fun dpadAction(code: Int): UiAction? = when (code) {
        UP -> UiAction.UP
        DOWN -> UiAction.DOWN
        LEFT -> UiAction.LEFT
        RIGHT -> UiAction.RIGHT
        else -> null
    }

    private fun commandAction(code: Int): UiAction? = when (code) {
        CENTER, ENTER, SPACE -> UiAction.CONFIRM
        BACK, ESC -> UiAction.BACK
        T -> UiAction.NEXT_TAB
        R -> UiAction.REFRESH
        SLASH -> UiAction.HELP
        else -> null
    }

    private fun letterAction(profile: DeviceProfile, code: Int): UiAction? {
        if (!profile.letterNavigation) return null
        return when (code) {
            I -> UiAction.UP
            J -> UiAction.LEFT
            K -> UiAction.DOWN
            L -> UiAction.RIGHT
            else -> null
        }
    }

    private fun isDirectional(action: UiAction?): Boolean = when (action) {
        UiAction.UP, UiAction.DOWN, UiAction.LEFT, UiAction.RIGHT -> true
        else -> false
    }
}
