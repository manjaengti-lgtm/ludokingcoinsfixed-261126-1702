package com.aibuilt.ludokingcoinsfixed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibuilt.ludokingcoinsfixed.model.LedgerEntry
import com.aibuilt.ludokingcoinsfixed.viewmodel.MainViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun TrackerScreen(viewModel: MainViewModel, onOpenHistory: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }

    if (state.loading) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text("Loading your records…", modifier = Modifier.padding(top = 12.dp))
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "This tracker is local bookkeeping only — it never touches the game file or a server balance.",
            style = MaterialTheme.typography.bodyMedium
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Recorded balance", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.balance.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Newest record first · stored on this device", style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = balanceInput,
                onValueChange = {
                    balanceInput = it.filter { c -> c.isDigit() }.take(12)
                    viewModel.clearError()
                },
                label = { Text("Set balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { viewModel.setBalance(balanceInput); balanceInput = "" },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Save") }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it.filter { c -> c.isDigit() || c == '-' || c == '+' }.take(13)
                viewModel.clearError()
            },
            label = { Text("Change (+2500 or -500)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(60); viewModel.clearError() },
            label = { Text("Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.addTransaction(amount, note); amount = ""; note = "" },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Record change") }

        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent activity", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenHistory) { Text("Full history") }
        }

        if (state.entries.isEmpty()) {
            Text("No records yet. Set a balance or log a change above.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.entries.take(5)) { entry -> EntryRow(entry) }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "${state.entries.size} record(s) · newest first",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.entries.isEmpty()) {
            Text(
                text = "Nothing recorded yet. Log a change on the tracker screen.",
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.entries) { entry -> EntryRow(entry) }
            }
        }
    }
}

@Composable
fun EntryRow(entry: LedgerEntry) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.note, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = DateFormat.getDateTimeInstance().format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = if (entry.amount > 0) "+${entry.amount}" else entry.amount.toString(),
                fontWeight = FontWeight.Medium,
                color = if (entry.amount >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
        }
    }
}
