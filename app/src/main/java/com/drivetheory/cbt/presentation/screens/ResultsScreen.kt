package com.drivetheory.cbt.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivetheory.cbt.presentation.navigation.Routes

@Composable
fun ResultsScreen(score: Int, total: Int, onBackHome: () -> Unit, onReview: (() -> Unit)? = null) {
    val percent = if (total > 0) (score * 100) / total else 0
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(text = "Results")
        Text(text = "$score / $total correct (${percent}%)")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBackHome) { Text("Back to Home") }
            if (onReview != null && total > 0) {
                Button(onClick = onReview) { Text("Review Answers") }
            }
        }
    }
}
