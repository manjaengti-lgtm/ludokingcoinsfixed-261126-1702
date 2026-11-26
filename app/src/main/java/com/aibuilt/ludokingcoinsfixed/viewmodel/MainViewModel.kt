package com.aibuilt.ludokingcoinsfixed.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aibuilt.ludokingcoinsfixed.data.BundledFile
import com.aibuilt.ludokingcoinsfixed.data.BundledFiles
import com.aibuilt.ludokingcoinsfixed.data.ChecklistStore
import com.aibuilt.ludokingcoinsfixed.data.LedgerFile
import com.aibuilt.ludokingcoinsfixed.data.TrackerRepository
import com.aibuilt.ludokingcoinsfixed.model.LedgerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TrackerUiState(
    val loading: Boolean = true,
    val balance: Long = 0L,
    val entries: List<LedgerEntry> = emptyList(),
    val error: String? = null
)

sealed interface FileUiState {
    data object Idle : FileUiState
    data object Loading : FileUiState
    data class Ready(val name: String, val content: String) : FileUiState
    data class Failed(val message: String) : FileUiState
}

class MainViewModel(application: Application) : ViewModel() {

    private val repository = TrackerRepository(application)
    private val checklist = ChecklistStore(application)
    private val assets = application.assets

    private val mutableState = MutableStateFlow(TrackerUiState())
    val state: StateFlow<TrackerUiState> = mutableState.asStateFlow()

    val checks: StateFlow<Set<String>> = checklist.checked

    private val mutableFile = MutableStateFlow<FileUiState>(FileUiState.Idle)
    val fileState: StateFlow<FileUiState> = mutableFile.asStateFlow()

    init {
        viewModelScope.launch {
            val data = repository.load()
            mutableState.value = TrackerUiState(
                loading = false,
                balance = data.balance,
                entries = data.entries
            )
        }
    }

    fun addTransaction(amountText: String, noteText: String) {
        val current = mutableState.value
        val amount = amountText.trim().removePrefix("+").toLongOrNull()
        if (amount == null || amount == 0L) {
            mutableState.value = current.copy(error = "Enter a non-zero whole number, e.g. 2500 or -500.")
            return
        }
        val updatedBalance = try {
            Math.addExact(current.balance, amount)
        } catch (_: ArithmeticException) {
            mutableState.value = current.copy(error = "That transaction is too large.")
            return
        }
        val entry = LedgerEntry(
            amount = amount,
            note = noteText.trim().ifBlank { "Manual transaction" }.take(60),
            timestamp = System.currentTimeMillis()
        )
        commit(current.copy(balance = updatedBalance, entries = listOf(entry) + current.entries, error = null))
    }

    fun setBalance(balanceText: String) {
        val current = mutableState.value
        val balance = balanceText.trim().toLongOrNull()
        if (balance == null || balance < 0L) {
            mutableState.value = current.copy(error = "Enter a balance of zero or more.")
            return
        }
        val difference = try {
            Math.subtractExact(balance, current.balance)
        } catch (_: ArithmeticException) {
            mutableState.value = current.copy(error = "That balance is too large.")
            return
        }
        if (difference == 0L) {
            mutableState.value = current.copy(error = null)
            return
        }
        val entry = LedgerEntry(
            amount = difference,
            note = "Balance correction",
            timestamp = System.currentTimeMillis()
        )
        commit(current.copy(balance = balance, entries = listOf(entry) + current.entries, error = null))
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    fun setCheck(id: String, value: Boolean) = checklist.set(id, value)

    fun resetChecks() = checklist.reset()

    fun loadFile(id: String) {
        val file: BundledFile = BundledFiles.byId(id) ?: run {
            mutableFile.value = FileUiState.Failed("That helper file does not exist.")
            return
        }
        mutableFile.value = FileUiState.Loading
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    assets.open(file.assetName).bufferedReader().use { it.readText() }
                }.getOrNull()
            }
            mutableFile.value = if (text == null) {
                FileUiState.Failed("The helper file could not be read from the app.")
            } else {
                FileUiState.Ready(file.fileName, text)
            }
        }
    }

    private fun commit(next: TrackerUiState) {
        mutableState.value = next
        viewModelScope.launch {
            val ok = repository.save(LedgerFile(next.balance, next.entries))
            if (!ok) {
                mutableState.value = mutableState.value.copy(error = "The records could not be saved on this device.")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(MainViewModel::class.java)) {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
            return MainViewModel(application) as T
        }
    }
}
