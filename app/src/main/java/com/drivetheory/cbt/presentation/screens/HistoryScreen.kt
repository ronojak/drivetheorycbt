package com.drivetheory.cbt.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivetheory.cbt.presentation.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(onBack: () -> Unit, vm: HistoryViewModel = viewModel<HistoryViewModel>(factory = HistoryViewModel.Factory)) {
    val attempts = vm.attempts.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack) { Text("Back") }
        LazyColumn {
            items(attempts.value) { a ->
                Text("Score: ${a.score}/${a.totalQuestions} at ${a.timestamp}")
            }
        }
    }
}
