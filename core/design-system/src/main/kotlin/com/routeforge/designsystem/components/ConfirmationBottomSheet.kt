package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SheetContentPadding = 16.dp

/** A bottom sheet for "are you sure" style prompts — title up top, then a caller-provided body
 *  (message text plus however many action buttons the situation needs: some confirmations are a
 *  plain two-button choice, others need a third "do something else instead" option). Deliberately
 *  decoupled from any specific screen or feature, same as [com.routeforge.designsystem.speed.SpeedSelectorDialog]. */
@Composable
fun ConfirmationBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(SheetContentPadding)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
