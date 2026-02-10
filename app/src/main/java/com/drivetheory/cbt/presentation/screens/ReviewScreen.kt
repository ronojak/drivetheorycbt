package com.drivetheory.cbt.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drivetheory.cbt.presentation.viewmodel.AnswerReview
import com.drivetheory.cbt.presentation.viewmodel.ReviewStore

@Composable
fun ReviewScreen(onBack: () -> Unit) {
    val reviews = ReviewStore.items
    val mistakesCount = reviews.count { !it.isCorrect }
    val (showMistakesOnly, setShowMistakesOnly) = remember { mutableStateOf(false) }
    val list = if (showMistakesOnly) reviews.filter { !it.isCorrect } else reviews

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(16.dp)) { Text("Back") }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showMistakesOnly,
                onClick = { setShowMistakesOnly(false) },
                label = { Text(text = "All (${reviews.size})") }
            )
            FilterChip(
                selected = showMistakesOnly,
                onClick = { setShowMistakesOnly(true) },
                label = { Text(text = "Mistakes (${mistakesCount})") }
            )
        }

        if (list.isEmpty()) {
            val msg = if (showMistakesOnly) "No mistakes to review." else "No questions to review."
            Text(msg, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(list) { item -> ReviewItem(item) }
            }
        }
    }
}

@Composable
private fun ReviewItem(item: AnswerReview) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = item.question.text, style = MaterialTheme.typography.titleMedium)
        item.question.options.forEachIndexed { idx, opt ->
            val color: Color = when {
                idx == item.correctIndex -> Color(0xFF2E7D32) // green
                item.selectedIndex == idx && idx != item.correctIndex -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(text = "• $opt", color = color, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        }
    }
}
