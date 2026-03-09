package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yespaybiz.ui.theme.*

// Colors from Figma
private val TopBgStart = Color(0xFFECF1FF)
private val TopBgEnd = Color(0xFFD5E0FF)
private val TextDark80 = Color(0xFF222222)
private val TextGray40 = Color(0xFF677F9D)
private val PrimaryCta = Color(0xFF1C85DB)
private val CardBg = Color(0xFFF6F7FC)
private val StrokeLight = Color(0xFFE3E9EE)
private val SuccessGreen = Color(0xFF5CC285)
private val PurpleGradientStart = Color(0xFF4B6AC2)
private val PurpleGradientEnd = Color(0xFF8F85CD)
private val TealGradientStart = Color(0xFF6CD0C6)
private val TealGradientEnd = Color(0xFF5DC2D8)
private val StatusNeutral = Color(0xFF798CC2)

@Composable
fun TransactionsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- HEADER SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, spotColor = Color(0x29000000), ambientColor = Color(0x29000000))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TopBgStart, TopBgEnd)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp) // SafeArea simulation
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark80
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PrimaryCta,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.SaveAlt,
                        contentDescription = "Download",
                        tint = PrimaryCta,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Collections (Active)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Collections",
                            color = Color(0xFF2A7DC0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(98.dp)
                                .height(2.dp)
                                .background(Color(0xFF798CC2))
                        )
                    }
                    // Requests
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Requests",
                            color = TextGray40,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(98.dp).height(2.dp))
                    }
                    // CBDC
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "CBDC",
                            color = TextGray40,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(98.dp).height(2.dp))
                    }
                }
            }
        }

        // --- SCROLLABLE CONTENT ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // space for bottom nav
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // View Total Settlement Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "View Total Settlement Amount",
                        color = PrimaryCta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = PrimaryCta,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(15.dp))
            }

            item {
                // --- SUMMARY / FILTER CARD ---
                TransactionSummaryCard()
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Date Header
                Text(
                    "Today  •  24 Mar’23",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark80,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transaction Items
            item {
                TransactionItem(
                    name = "Bhushan Anil Dixit",
                    amount = "₹35,000.00",
                    rrn = "1234 567890AB",
                    time = "24 Mar ’23, 10:13 AM",
                    isReward = false,
                    isSuccess = true
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                TransactionItem(
                    name = "BIZ Reward",
                    amount = "₹35,000.00",
                    rrn = "1234 567890AB",
                    time = "23 Mar ’23, 10:13 AM",
                    isReward = true,
                    isSuccess = true,
                    settledTime = "Settled on 25 Mar ’23, 09:53 PM"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                TransactionItem(
                    name = "Bhushan Anil Dixit",
                    amount = "₹35,000.00",
                    rrn = "1234 567890AB",
                    time = "24 Mar ’23, 10:13 AM",
                    isReward = false,
                    isSuccess = true,
                    settledTime = "Settled on 25 Mar ’23, 09:53 PM"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                TransactionItem(
                    name = "Bhushan Anil Dixit",
                    amount = "₹35,000.00",
                    rrn = "1234 567890AB",
                    time = "24 Mar ’23, 10:13 AM",
                    isReward = false,
                    isSuccess = true,
                    settledTime = "Settled on 25 Mar ’23, 09:53 PM"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun TransactionSummaryCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background blur/gradient overlay from Figma (light teal block behind illustration)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(120.dp)
                    .clip(RoundedCornerShape(bottomEnd = 16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.White.copy(alpha = 0f), Color(0xFFCDF0FF).copy(alpha = 0.5f))
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                ) {
                    // "Today" Button
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PurpleGradientStart, PurpleGradientEnd)
                                ),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Today", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // "Filters" Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TealGradientStart, TealGradientEnd)
                                ),
                                shape = RoundedCornerShape(bottomStart = 16.dp, topEnd = 16.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FilterAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Filters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Collection", fontSize = 11.sp, color = TextGray40)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("₹15,000", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark80)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(StrokeLight)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text("Transactions", fontSize = 11.sp, color = TextGray40)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("75", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark80)
                    }
                }
            }
            
            // Placeholder for the illustration on the right
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                tint = PrimaryCta.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 10.dp)
                    .size(50.dp)
            )
        }
    }
}

@Composable
fun TransactionItem(
    name: String,
    amount: String,
    rrn: String,
    time: String,
    isReward: Boolean,
    isSuccess: Boolean,
    settledTime: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = BorderStroke(1.dp, StrokeLight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 17.dp, vertical = 10.dp)
            ) {
                // Left Icon (UPI Collect or Reward)
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(35.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isReward) {
                            Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color(0xFF374957), modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryCta, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextDark80
                        )
                        Text(
                            amount,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark80
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "RRN : $rrn",
                        fontSize = 11.sp,
                        color = if (isReward) Color(0xFF798CC2) else TextGray40
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        time,
                        fontSize = 11.sp,
                        color = if (isReward) Color(0xFF798CC2) else TextGray40
                    )
                }
            }

            // Bottom-Left Status Tag
            Surface(
                modifier = Modifier.align(Alignment.BottomStart),
                color = if (isReward) Color(0xFF798CC2) else SuccessGreen,
                shape = RoundedCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
            ) {
                Text(
                    if (isReward) "Reward" else "UPI Collect",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            // Bottom-Right Settled Tag
            if (settledTime != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                    border = BorderStroke(1.dp, Color(0x33798CC2))
                ) {
                    Text(
                        settledTime,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF798CC2),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            } else if (isSuccess && !isReward) {
                // If not explicitly settled but success, show green line indicator matching Figma UI
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .fillMaxHeight(0.6f)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(SuccessGreen)
                )
            }
        }
    }
}
