package io.github.mz.mpicker.api

import androidx.annotation.ColorInt

/**
 * Runtime colors for the built-in picker, crop and editor screens.
 * Use the presets for common looks, or copy one and override the colors you need.
 */
data class PickerTheme(
    @ColorInt val rootBackground: Int,
    @ColorInt val topBarBackground: Int,
    @ColorInt val bottomBarBackground: Int,
    @ColorInt val primary: Int,
    @ColorInt val primaryPressed: Int,
    @ColorInt val primaryDisabled: Int,
    @ColorInt val onPrimary: Int,
    @ColorInt val topBarText: Int,
    @ColorInt val bodyText: Int,
    @ColorInt val subtleText: Int,
    @ColorInt val partialBarBackground: Int,
    @ColorInt val partialBarText: Int,
    @ColorInt val editorBackground: Int,
    @ColorInt val editorToolbarBackground: Int,
    @ColorInt val editorPanelBackground: Int,
    @ColorInt val editorToolText: Int,
    @ColorInt val editorToolSelectedBackground: Int,
    @ColorInt val editorToolStroke: Int,
) {
    companion object {
        @JvmField
        val GREEN = PickerTheme(
            rootBackground = 0xFFFFFFFF.toInt(),
            topBarBackground = 0xFF222222.toInt(),
            bottomBarBackground = 0xFFF2F2F2.toInt(),
            primary = 0xFF16A34A.toInt(),
            primaryPressed = 0xFF15803D.toInt(),
            primaryDisabled = 0xFFC8D2CC.toInt(),
            onPrimary = 0xFFFFFFFF.toInt(),
            topBarText = 0xFFFFFFFF.toInt(),
            bodyText = 0xFF444444.toInt(),
            subtleText = 0xFF999999.toInt(),
            partialBarBackground = 0xFFFFF8E1.toInt(),
            partialBarText = 0xFF5D4037.toInt(),
            editorBackground = 0xFF000000.toInt(),
            editorToolbarBackground = 0xFF222222.toInt(),
            editorPanelBackground = 0xFF151515.toInt(),
            editorToolText = 0xFFD8D8D8.toInt(),
            editorToolSelectedBackground = 0xFF16A34A.toInt(),
            editorToolStroke = 0xFF5BE58A.toInt(),
        )

        @JvmField
        val WECHAT_DARK = GREEN.copy(
            topBarBackground = 0xFF191919.toInt(),
            bottomBarBackground = 0xFF1F1F1F.toInt(),
            rootBackground = 0xFF101010.toInt(),
            primary = 0xFF07C160.toInt(),
            primaryPressed = 0xFF06AD56.toInt(),
            primaryDisabled = 0xFF3A4A40.toInt(),
            bodyText = 0xFFEDEDED.toInt(),
            subtleText = 0xFF8C8C8C.toInt(),
            partialBarBackground = 0xFF2B2414.toInt(),
            partialBarText = 0xFFFFD480.toInt(),
            editorToolbarBackground = 0xFF111111.toInt(),
            editorPanelBackground = 0xFF171717.toInt(),
            editorToolSelectedBackground = 0xFF07C160.toInt(),
            editorToolStroke = 0xFF48E18A.toInt(),
        )

        @JvmField
        val SKY = GREEN.copy(
            topBarBackground = 0xFF0F4C81.toInt(),
            bottomBarBackground = 0xFFEAF4FF.toInt(),
            primary = 0xFF147DCC.toInt(),
            primaryPressed = 0xFF0F5F99.toInt(),
            primaryDisabled = 0xFFBFD4E7.toInt(),
            bodyText = 0xFF16324F.toInt(),
            subtleText = 0xFF6A7F94.toInt(),
            partialBarBackground = 0xFFE8F3FF.toInt(),
            partialBarText = 0xFF0F4C81.toInt(),
            editorToolbarBackground = 0xFF0B2239.toInt(),
            editorPanelBackground = 0xFF102A44.toInt(),
            editorToolSelectedBackground = 0xFF147DCC.toInt(),
            editorToolStroke = 0xFF6CB7F2.toInt(),
        )

        @JvmField
        val AMBER = GREEN.copy(
            topBarBackground = 0xFF3A2618.toInt(),
            bottomBarBackground = 0xFFFFF3DF.toInt(),
            primary = 0xFFFF8A00.toInt(),
            primaryPressed = 0xFFE26F00.toInt(),
            primaryDisabled = 0xFFEAD4B6.toInt(),
            bodyText = 0xFF3D2B1F.toInt(),
            subtleText = 0xFF8B7564.toInt(),
            partialBarBackground = 0xFFFFF0CC.toInt(),
            partialBarText = 0xFF6B3F00.toInt(),
            editorToolbarBackground = 0xFF1E1712.toInt(),
            editorPanelBackground = 0xFF261A12.toInt(),
            editorToolSelectedBackground = 0xFFFF8A00.toInt(),
            editorToolStroke = 0xFFFFB55C.toInt(),
        )

        @JvmStatic
        fun default(): PickerTheme = GREEN
    }
}
