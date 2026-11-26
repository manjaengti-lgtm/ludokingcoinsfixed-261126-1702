package com.aibuilt.ludokingcoinsfixed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aibuilt.ludokingcoinsfixed.model.CheckItem
import com.aibuilt.ludokingcoinsfixed.model.PatchSite
import com.aibuilt.ludokingcoinsfixed.model.PatchSpec
import com.aibuilt.ludokingcoinsfixed.ui.components.LabelValue
import com.aibuilt.ludokingcoinsfixed.ui.components.MonoBlock
import com.aibuilt.ludokingcoinsfixed.ui.components.SectionCard
import com.aibuilt.ludokingcoinsfixed.viewmodel.MainViewModel

@Composable
fun PatchScreen(viewModel: MainViewModel) {
    val checks by viewModel.checks.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TargetCard() }
        item { ValueCard() }
        items(PatchSpec.sites) { site -> SiteCard(site) }
        item { UntouchedCard() }
        item {
            Text(
                text = "Checks before finishing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(PatchSpec.checks) { check ->
            CheckRow(
                check = check,
                ticked = checks.contains(check.id),
                onCheckedChange = { viewModel.setCheck(check.id, it) }
            )
        }
        item {
            TextButton(onClick = { viewModel.resetChecks() }) {
                Text("Clear all ticks")
            }
        }
    }
}

@Composable
private fun TargetCard() {
    SectionCard(title = "Target build") {
        LabelValue("Package", PatchSpec.PACKAGE_NAME, mono = true)
        LabelValue("Version", "${PatchSpec.VERSION_NAME} (${PatchSpec.VERSION_CODE})")
        LabelValue("Architecture", PatchSpec.ABI)
        LabelValue("SDK", "min ${PatchSpec.MIN_SDK} / target ${PatchSpec.TARGET_SDK}")
        LabelValue("Output file", PatchSpec.OUTPUT_NAME)
        Text(
            text = "One merged, standalone APK. All four stock splits are folded in, the manifest stays " +
                "byte-identical and only ${PatchSpec.LIBRARY} is patched.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ValueCard() {
    SectionCard(title = "Forced coin value") {
        Text(
            text = PatchSpec.COIN_VALUE.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        MonoBlock("hex ${PatchSpec.COIN_HEX}  ·  spec sheet ${PatchSpec.SPEC_DECIMAL}")
        Text(
            text = "Both getters return this value, so the display and every local stake check stay high " +
                "and no table is ever refused for coins.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SiteCard(site: PatchSite) {
    SectionCard(title = site.method) {
        LabelValue("RVA", site.rva, mono = true)
        MonoBlock(site.bytes)
        Text(
            text = site.instructions,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = site.effect,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun UntouchedCard() {
    SectionCard(title = "Left stock", modifier = Modifier.fillMaxWidth()) {
        LabelValue("RVA", PatchSpec.UNTOUCHED_RVA, mono = true)
        Text(
            text = PatchSpec.UNTOUCHED_METHOD,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "No edits on the setter: the forced writes sit on top of the constant getters, so an " +
                "incoming server value still ends up overridden when the client reads it back.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun CheckRow(check: CheckItem, ticked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SectionCard(title = null) {
        Row(Modifier.fillMaxWidth()) {
            Checkbox(checked = ticked, onCheckedChange = onCheckedChange)
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    text = check.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = check.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
