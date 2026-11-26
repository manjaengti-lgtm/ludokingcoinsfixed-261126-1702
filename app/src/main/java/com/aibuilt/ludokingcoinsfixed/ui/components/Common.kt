package com.aibuilt.ludokingcoinsfixed.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aibuilt.ludokingcoinsfixed.ui.theme.MonoFamily

@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
fun MonoBlock(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        SelectionContainer {
            Text(
                text = text,
                modifier = Modifier.padding(10.dp),
                fontFamily = MonoFamily,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LabelValue(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
