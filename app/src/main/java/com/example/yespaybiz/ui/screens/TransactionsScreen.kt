package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yespaybiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFFEBF4FF), Color.White),
                    endY = 400f
                )
            )
    ) {
        // Header
        TopAppBar(
            title = { Text("Transactions", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryBlue)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = PrimaryBlue)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // Tabs
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Collections", "Requests", "POS")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryBlue,
            divider = { HorizontalDivider(color = Color(0xFFF3F4F6)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == index) PrimaryBlue else TextGray
                        )
                    }
                )
            }
        }

        // View Pending Settlement
        TextButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "View Pending Settlement Amount",
                color = FilterTeal,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = FilterTeal,
                modifier = Modifier.size(16.dp)
            )
        }

        // Summary Dashboard Card
        TransactionFilterCard()

        Spacer(modifier = Modifier.height(32.dp))

        // Empty State
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Transaction Found",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextDark
            )
        }
    }
}

@Composable
fun TransactionFilterCard() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            // Colored Filter Bars
            Row(modifier = Modifier.fillMaxWidth()) {
                // "Today" purple filter
                Surface(
                    modifier = Modifier.weight(1f),
                    color = FilterPurple,
                    shape = RoundedCornerShape(bottomEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Today",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // "Filters" teal filter
                Surface(
                    modifier = Modifier.weight(1f),
                    color = FilterTeal,
                    shape = RoundedCornerShape(bottomStart = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Filters",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Stats Content
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Text("Collection", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹ 0.00", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                }
                Spacer(modifier = Modifier.width(24.dp))
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = Color(0xFFE5E7EB)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text("Transactions", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("0", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                }
            }
        }
    }
}
