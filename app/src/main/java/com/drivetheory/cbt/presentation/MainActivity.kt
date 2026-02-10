package com.drivetheory.cbt.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.drivetheory.cbt.presentation.navigation.AppNavGraph
import com.drivetheory.cbt.presentation.theme.DriveTheoryTheme
import com.drivetheory.cbt.presentation.components.AppTopBar
import com.drivetheory.cbt.presentation.billing.SubscriptionActivity
import com.drivetheory.cbt.presentation.account.ProfileActivity
import com.drivetheory.cbt.presentation.account.SettingsActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DriveTheoryTheme {
                val ctx = LocalContext.current
                Scaffold(
                    topBar = {
                        AppTopBar(
                            onGoPremium = { ctx.startActivity(Intent(ctx, SubscriptionActivity::class.java)) },
                            onOpenProfile = { ctx.startActivity(Intent(ctx, ProfileActivity::class.java)) },
                            onOpenSettings = { ctx.startActivity(Intent(ctx, SettingsActivity::class.java)) }
                        )
                    }
                ) { innerPadding ->
                    Surface {
                        AppNavGraph(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
