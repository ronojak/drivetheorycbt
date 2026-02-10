package com.drivetheory.cbt.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drivetheory.cbt.BuildConfig
import com.drivetheory.cbt.core.gating.AccessGate

@Composable
fun ExamSetupScreen(
    vehicle: String,
    onStart: (count: Int?, all: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val (count, setCount) = remember { mutableStateOf(50) }
    val (all, setAll) = remember { mutableStateOf(false) }
    val (remaining, setRemaining) = remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(vehicle) {
        val gate = AccessGate(context)
        val consumed = gate.consumedToday(vehicle)
        setRemaining((10 - consumed).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Exam Setup (${vehicleLabel(vehicle)})")

        // Informative hint shown in all builds (non-debug too)
        Text(
            text = "Free plan allows up to 10 questions per category each day. " +
                "Upgrade for unlimited access.",
            style = MaterialTheme.typography.bodySmall
        )

        if (BuildConfig.DEBUG && remaining != null) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(text = "Remaining today: ${remaining}/10", style = MaterialTheme.typography.bodySmall) }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Use all questions")
            Spacer(modifier = Modifier.padding(4.dp))
            Checkbox(checked = all, onCheckedChange = { setAll(it) })
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { setCount((count - 10).coerceAtLeast(1)) }, enabled = !all) { Text("-10") }
            Button(onClick = { setCount((count - 1).coerceAtLeast(1)) }, enabled = !all) { Text("-") }
            Text(if (all) "All" else "$count questions")
            Button(onClick = { setCount(count + 1) }, enabled = !all) { Text("+") }
            Button(onClick = { setCount(count + 10) }, enabled = !all) { Text("+10") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCancel) { Text("Cancel") }
            Button(onClick = { onStart(if (all) null else count, all) }) { Text("Start Exam") }
        }

        // Provide a visible entry point to upgrade
        Button(
            onClick = {
                val intent = android.content.Intent(context, com.drivetheory.cbt.presentation.billing.SubscriptionActivity::class.java)
                context.startActivity(intent)
            }
        ) { Text("Go Premium") }
    }
}

private fun vehicleLabel(vehicle: String): String = when (vehicle.lowercase()) {
    "car", "cars" -> "Cars"
    "motorcycle", "bike", "bikes" -> "Motorcycles"
    "lorry", "lorries", "truck", "trucks" -> "Lorries"
    "buscoach", "bus", "buses", "coach", "coaches" -> "Buses & Coaches"
    else -> vehicle
}
