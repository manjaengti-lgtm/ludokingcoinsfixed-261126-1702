package com.aibuilt.ludokingcoinsfixed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ludokingcoinsfixed.ui.AppRoot
import com.aibuilt.ludokingcoinsfixed.ui.theme.LudoKingTheme
import com.aibuilt.ludokingcoinsfixed.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = MainViewModel.Factory(application)
        setContent {
            LudoKingTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                AppRoot(viewModel)
            }
        }
    }
}
