package com.routeforge.designsystem.theme

import androidx.compose.ui.unit.dp

/** Shared spacing tokens for screen/sheet layout — collapses what used to be the same `16.dp`/
 *  `8.dp` private constants, redeclared under several different names (`ScreenContentPadding`,
 *  `SheetContentPadding`, `ControlsRowSpacing`), independently in a dozen files. */
object Spacing {
    /** Padding around a screen's or sheet's outermost content. */
    val ScreenContent = 16.dp

    /** Horizontal gap between buttons/icons in a row of controls. */
    val ControlsRow = 8.dp
}
