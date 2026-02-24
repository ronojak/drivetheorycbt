package com.drivetheory.cbt.presentation.screens

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivetheory.cbt.presentation.viewmodel.ExamViewModel
import com.drivetheory.cbt.presentation.billing.SubscriptionActivity

@Composable
fun ExamScreen(
    onFinish: (score: Int, total: Int) -> Unit,
    desiredCount: Int? = null,
    useAll: Boolean = false,
    vehicle: String = "car",
    onRedirectToBilling: () -> Unit = {},
    vm: ExamViewModel = viewModel<ExamViewModel>(factory = ExamViewModel.factory(desiredCount, useAll, vehicle))
) {
    val state by vm.uiState.collectAsState()

    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    LaunchedEffect(Unit) {
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Time: ${state.remaining}s")
            Text(text = "Q ${state.index + 1} / ${state.total}")
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 24.dp)) {
            Text(text = state.questionText, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.padding(8.dp))
            state.options.forEachIndexed { idx, opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = state.selectedIndex == idx, onClick = { vm.select(idx) })
                    Text(text = opt, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { vm.prev() }, enabled = state.index > 0) { Text("Prev") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { vm.next() }, enabled = state.index < state.total - 1) { Text("Next") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { vm.finishAndScore() }) { Text("Submit") }
            }
        }
    }

    LaunchedEffect(state.finished) {
        if (state.finished) {
            onFinish(state.score, state.total)
        }
    }

    LaunchedEffect(state.redirectToBilling) {
        if (state.redirectToBilling) {
            val ctx = activity ?: context
            val intent = Intent(ctx, SubscriptionActivity::class.java)
            if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            onRedirectToBilling()
        }
    }
}
