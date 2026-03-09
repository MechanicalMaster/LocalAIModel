package com.example.yespaybiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yespaybiz.ai.ChatViewModel
import com.example.yespaybiz.ui.screens.ConversationScreen
import com.example.yespaybiz.ui.screens.HomeScreen
import com.example.yespaybiz.ui.screens.TransactionsScreen
import com.example.yespaybiz.ui.screens.ShowQrScreen
import com.example.yespaybiz.ui.screens.SettlementScreen
import com.example.yespaybiz.ui.screens.ProfileScreen
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
    var showProfile by rememberSaveable { mutableStateOf(false) }

    // Single ChatViewModel instance shared across recompositions
    val chatViewModel: ChatViewModel = viewModel()

    // Observe AI navigation commands and switch tabs automatically
    LaunchedEffect(Unit) {
        chatViewModel.navigationEvent.collect { event ->
            when (event) {
                "navigateToTransactions" -> currentDestination = AppDestinations.TRANSACTIONS
                "navigateToQR" -> currentDestination = AppDestinations.SHOW_QR
                "navigateToSettlement" -> currentDestination = AppDestinations.SETTLEMENT
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(63.dp)
                    .shadow(
                        elevation = 15.dp,
                        spotColor = Color(0x26000000),
                        ambientColor = Color(0x26000000)
                    )
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.example.yespaybiz.ui.theme.NavBarSelectedIcon,
                    selectedTextColor = com.example.yespaybiz.ui.theme.NavBarSelectedText,
                    unselectedIconColor = com.example.yespaybiz.ui.theme.TextGray,
                    unselectedTextColor = com.example.yespaybiz.ui.theme.TextGray,
                    indicatorColor = Color.Transparent
                )
                AppDestinations.entries.forEach {
                    NavigationBarItem(
                        icon = {
                            if (it.iconVector != null) {
                                Icon(
                                    imageVector = it.iconVector,
                                    contentDescription = it.label
                                )
                            } else if (it.iconResId != null) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = it.iconResId),
                                    contentDescription = it.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { 
                            Text(
                                text = it.label,
                                fontSize = 11.sp,
                                fontWeight = if (it == currentDestination) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            if (showProfile) {
                ProfileScreen(onBackClick = { showProfile = false }, chatViewModel = chatViewModel)
            } else {
                when (currentDestination) {
                    AppDestinations.HOME         -> HomeScreen(onProfileClick = { showProfile = true })

                    AppDestinations.TRANSACTIONS -> TransactionsScreen()
                    AppDestinations.SHOW_QR      -> ShowQrScreen()
                    AppDestinations.SETTLEMENT   -> SettlementScreen()
                    AppDestinations.AI_CHAT      -> ConversationScreen(viewModel = chatViewModel)
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val iconVector: ImageVector? = null,
    val iconResId: Int? = null
) {
    HOME("Home", iconVector = Icons.Default.Home),
    TRANSACTIONS("Transactions", iconVector = Icons.Default.List),
    SHOW_QR("Show QR", iconVector = Icons.Default.QrCode),
    SETTLEMENT("Settlement", iconVector = Icons.Default.AccountBalanceWallet),
    AI_CHAT("Insights", iconResId = R.drawable.ic_insights),
}
