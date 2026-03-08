package com.example.yespaybiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.yespaybiz.ui.screens.HomeScreen
import com.example.yespaybiz.ui.screens.TransactionsScreen
import com.example.yespaybiz.ui.screens.ShowQrScreen
import com.example.yespaybiz.ui.screens.SettlementScreen
import com.example.yespaybiz.ui.theme.YesPayBizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YesPayBizTheme {
                YesPayBizApp()
            }
        }
    }
}

@Composable
fun YesPayBizApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen()
                AppDestinations.TRANSACTIONS -> TransactionsScreen()
                AppDestinations.SHOW_QR -> ShowQrScreen()
                AppDestinations.SETTLEMENT -> SettlementScreen()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    TRANSACTIONS("Transactions", Icons.Default.List),
    SHOW_QR("Show QR", Icons.Default.QrCode),
    SETTLEMENT("Settlement", Icons.Default.AccountBalanceWallet),
}
