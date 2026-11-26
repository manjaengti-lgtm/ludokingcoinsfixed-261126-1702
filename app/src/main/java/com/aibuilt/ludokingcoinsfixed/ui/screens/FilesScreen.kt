package com.aibuilt.ludokingcoinsfixed.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aibuilt.ludokingcoinsfixed.data.BundledFile
import com.aibuilt.ludokingcoinsfixed.data.BundledFiles
import com.aibuilt.ludokingcoinsfixed.ui.components.SectionCard
import com.aibuilt.ludokingcoinsfixed.ui.theme.MonoFamily
import com.aibuilt.ludokingcoinsfixed.viewmodel.FileUiState
import com.aibuilt.ludokingcoinsfixed.viewmodel.MainViewModel

@Composable
fun FilesScreen(viewModel: MainViewModel, onOpenFile: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "The build can be reproduced from these three files. Tap one to read it, copy it, " +
                    "or send it somewhere.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(BundledFiles.all) { file ->
            FileCard(file) { onOpenFile(file.id) }
        }
    }
}

@Composable
private fun FileCard(file: BundledFile, onClick: () -> Unit) {
    SectionCard(title = file.fileName, modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = file.about,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FileDetailScreen(viewModel: MainViewModel, id: String) {
    val state by viewModel.fileState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(id) { viewModel.loadFile(id) }

    when (val current = state) {
        is FileUiState.Idle, is FileUiState.Loading -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text("Reading the file…", modifier = Modifier.padding(top = 12.dp))
        }

        is FileUiState.Failed -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(current.message, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { viewModel.loadFile(id) },
                modifier = Modifier.padding(top = 12.dp)
            ) { Text("Try again") }
        }

        is FileUiState.Ready -> Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(current.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { clipboard.setText(AnnotatedString(current.content)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copy whole file") }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, current.name)
                                putExtra(Intent.EXTRA_TEXT, current.content)
                            }
                            context.startActivity(Intent.createChooser(intent, "Send ${current.name}"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Share file") }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = current.content,
                        fontFamily = MonoFamily,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
