package com.drivetheory.cbt.presentation.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onGoPremium: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("DriveTheory CBT") },
        actions = {
            TextButton(onClick = { expanded = true }) { Text("Menu") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Go Premium") }, onClick = { expanded = false; onGoPremium() })
                DropdownMenuItem(text = { Text("Profile") }, onClick = { expanded = false; onOpenProfile() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { expanded = false; onOpenSettings() })
            }
        },
        modifier = modifier
    )
}
