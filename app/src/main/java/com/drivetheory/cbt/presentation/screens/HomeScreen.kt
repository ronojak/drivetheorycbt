package com.drivetheory.cbt.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.drivetheory.cbt.core.ServiceLocator
import com.drivetheory.cbt.presentation.components.BrandLogo
import com.drivetheory.cbt.core.gating.AccessGate
import com.drivetheory.cbt.BuildConfig
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onStartCar: () -> Unit,
    onStartMotorcycle: () -> Unit,
    onStartLorry: () -> Unit,
    onStartBusCoach: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val (count, setCount) = remember { mutableStateOf<Int?>(null) }
    val (carCount, setCarCount) = remember { mutableStateOf<Int?>(null) }
    val (motoCount, setMotoCount) = remember { mutableStateOf<Int?>(null) }
    val (lorryCount, setLorryCount) = remember { mutableStateOf<Int?>(null) }
    val (busCount, setBusCount) = remember { mutableStateOf<Int?>(null) }
    val (carRem, setCarRem) = remember { mutableStateOf<Int?>(null) }
    val (motoRem, setMotoRem) = remember { mutableStateOf<Int?>(null) }
    val (lorryRem, setLorryRem) = remember { mutableStateOf<Int?>(null) }
    val (busRem, setBusRem) = remember { mutableStateOf<Int?>(null) }
    fun triggerRefresh() {
        scope.launch {
            // Load count of available questions to help verify seed import
            val repo = ServiceLocator.questions(context)
            val all = repo.loadQuestions()
            setCount(all.size)
            setCarCount(repo.loadQuestions("car").size)
            setMotoCount(repo.loadQuestions("motorcycle").size)
            setLorryCount(repo.loadQuestions("lorry").size)
            setBusCount(repo.loadQuestions("buscoach").size)

            // Load remaining today per category (debug info)
            val gate = AccessGate(context)
            setCarRem((10 - gate.consumedToday("car")).coerceAtLeast(0))
            setMotoRem((10 - gate.consumedToday("motorcycle")).coerceAtLeast(0))
            setLorryRem((10 - gate.consumedToday("lorry")).coerceAtLeast(0))
            setBusRem((10 - gate.consumedToday("buscoach")).coerceAtLeast(0))
        }
    }

    LaunchedEffect(Unit) { triggerRefresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                triggerRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(24.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogo(size = 120.dp)
        Text(text = "DriveTheory CBT")
        count?.let { Text(text = "Total Questions: $it") }

        Button(onClick = onStartCar, modifier = Modifier.padding(top = 16.dp)) {
            val label = buildString {
                append("Cars exams")
                carCount?.let { append(" ($it)") }
                if (BuildConfig.DEBUG) carRem?.let { append(" — ${it} left today") }
            }
            Text(label)
        }
        Button(onClick = onStartMotorcycle, modifier = Modifier.padding(top = 8.dp)) {
            val label = buildString {
                append("Motorcycle exams")
                motoCount?.let { append(" ($it)") }
                if (BuildConfig.DEBUG) motoRem?.let { append(" — ${it} left today") }
            }
            Text(label)
        }
        Button(onClick = onStartLorry, modifier = Modifier.padding(top = 8.dp)) {
            val label = buildString {
                append("Lorries exams")
                lorryCount?.let { append(" ($it)") }
                if (BuildConfig.DEBUG) lorryRem?.let { append(" — ${it} left today") }
            }
            Text(label)
        }
        Button(onClick = onStartBusCoach, modifier = Modifier.padding(top = 8.dp)) {
            val label = buildString {
                append("Buses and coaches exams")
                busCount?.let { append(" ($it)") }
                if (BuildConfig.DEBUG) busRem?.let { append(" — ${it} left today") }
            }
            Text(label)
        }

        Button(onClick = onOpenHistory, modifier = Modifier.padding(top = 16.dp)) { Text("View History") }

        // Always show a clear path to upgrade/premium
        Button(
            onClick = {
                val intent = android.content.Intent(context, com.drivetheory.cbt.presentation.billing.SubscriptionActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Go Premium") }

        if (BuildConfig.DEBUG) {
            Button(onClick = {
                AccessGate(context).resetTodayAll()
                Toast.makeText(context, "Daily counters reset", Toast.LENGTH_SHORT).show()
                triggerRefresh()
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Developer Tools: Reset Daily Counters") }
        }
    }
}
