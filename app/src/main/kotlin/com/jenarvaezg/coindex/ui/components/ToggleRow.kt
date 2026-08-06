package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One switch of a configuration: what it is called, why it is greyed, and its state.
 *
 * The label carries the tap as well as the switch does, because a thumb aiming at a 20dp track on a
 * phone held in one hand is the same measured miss the filter shelf fixed by taking the whole line
 * (`FilterShelf`).
 *
 * [note] is where a disabled switch says why, and it is what keeps grey from reading as broken: a
 * control the collector cannot move owes them the reason on the spot, not in a help screen.
 */
@Composable
fun ToggleRow(
    label: String,
    note: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Paper.ink else Paper.muted,
            )
            note?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Paper.muted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Paper.paper,
                checkedTrackColor = Paper.moss,
                uncheckedThumbColor = Paper.muted,
                uncheckedTrackColor = Paper.paperDeep,
                uncheckedBorderColor = Paper.hairline,
                // Grey and still readable as on or off: a disabled switch is reporting the
                // configuration as well as refusing to change it.
                disabledCheckedThumbColor = Paper.paper,
                disabledCheckedTrackColor = Paper.hairline,
                disabledUncheckedThumbColor = Paper.hairline,
                disabledUncheckedTrackColor = Paper.paperDeep,
                disabledUncheckedBorderColor = Paper.hairline,
            ),
        )
    }
}
