package com.aibuilt.ludokingcoinsfixed.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChecklistStore(context: Context) {

    private val prefs = context.getSharedPreferences("build_checks", Context.MODE_PRIVATE)
    private val checkedFlow = MutableStateFlow(read())
    val checked: StateFlow<Set<String>> = checkedFlow

    private fun read(): Set<String> = prefs.all
        .filterValues { it == true }
        .keys
        .filter { it.startsWith(PREFIX) }
        .map { it.removePrefix(PREFIX) }
        .toSet()

    fun set(id: String, value: Boolean) {
        prefs.edit().putBoolean(PREFIX + id, value).apply()
        checkedFlow.value = checkedFlow.value.toMutableSet().apply {
            if (value) add(id) else remove(id)
        }
    }

    fun reset() {
        prefs.edit().clear().apply()
        checkedFlow.value = emptySet()
    }

    private companion object {
        const val PREFIX = "chk_"
    }
}
