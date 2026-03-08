package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yespaybiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header — gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFECF1FF), Color(0xFFD5E0FF))
                    )
                )
        ) {
            TopAppBar(
                title = {
                    Text(
                        "Settlement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(15.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettlementAlert() }
            item { PendingSettlementCard() }
            item { PreviousSettlementsHeader() }
            item { SettlementSearchAndFilter() }
            items(settlementData) { settlement ->
                SettlementItemCard(settlement)
            }
            // Bottom spacer for nav bar
            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}

@Composable
fun SettlementAlert() {
    // Figma: "Trial/BG4" gradient — pink to warm gold
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(AlertBannerStart, AlertBannerEnd)
                )
            )
            .padding(15.dp)
    ) {
        Column {
            Text(
                text = "Next Auto settlement : 7:00 AM, 07 Mar 2026",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryCTA // Figma: gradient text blue, using closest solid
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "If you do not initiate settlement, the pending settlement amount will be Auto-settled.",
                color = TextDark.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun PendingSettlementCard() {
    // Figma: "Pro Card BG" gradient — cyan to teal
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(PendingCyanLight, PendingCyanDark)
                )
            )
    ) {
        Column {
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Amount tag — Figma: gradient pill with left-flush, right-rounded
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AmountTagStart, AmountTagEnd)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        "₹30,000.00",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }

                // Show Breakup button — Figma: white pill with border
                Surface(
                    modifier = Modifier.padding(end = 15.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(23.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StrokeLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Show Breakup",
                            color = PrimaryCTA,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = PrimaryCTA,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Pending Settlement info
            Column(modifier = Modifier.padding(horizontal = 35.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Pending Settlement Amount",
                    color = TextDark,
                    fontSize = 14.sp
                )
                Text(
                    "(20 Jan'22 01:34 PM - Till Now)",
                    color = TextDark,
                    fontSize = 14.sp
                )
            }

            // Initiate Settlement Button — Figma: PrimaryCTA blue
            Button(
                onClick = {},
                modifier = Modifier
                    .padding(horizontal = 35.dp)
                    .padding(top = 16.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCTA),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Initiate Settlement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PreviousSettlementsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Previous Settlements",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Text(
            "VIEW ALL",
            color = PrimaryCTA,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SettlementSearchAndFilter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search box — Figma: white bg, #E3E9EE border, 12dp radius
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text(
                    "Search UTR Number",
                    fontSize = 16.sp,
                    color = TextGray
                )
            },
            trailingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryCTA)
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = StrokeLight,
                focusedBorderColor = PrimaryCTA,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Calendar button — Figma: #F6F7FC bg, 12dp radius
        Surface(
            modifier = Modifier.size(45.dp),
            shape = RoundedCornerShape(12.dp),
            color = ItemCardBg
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = PrimaryCTA,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettlementItemCard(settlement: SettlementData) {
    // Figma: #F6F7FC bg, 1px #E3E9EE border, 12dp radius
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ItemCardBg)
            .border(1.dp, StrokeLight, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    settlement.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark
                )
                Text(
                    "₹ ${settlement.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // UTR or error message
            Text(
                settlement.subtitle,
                color = if (settlement.status == SettlementStatus.FAILED) ErrorRed else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (settlement.status == SettlementStatus.FAILED) FontWeight.Normal else FontWeight.Bold
            )
        }

        // Status tag — Figma: bottom-right, asymmetric radius (12px 0px)
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SettledTagBorder)
        ) {
            Text(
                text = if (settlement.status == SettlementStatus.SETTLED) "Settled" else "Failed",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                color = if (settlement.status == SettlementStatus.SETTLED) SettledTagText else ErrorRed,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class SettlementStatus { SETTLED, FAILED }

data class SettlementData(
    val date: String,
    val subtitle: String,
    val amount: String,
    val status: SettlementStatus
)

val settlementData = listOf(
    SettlementData(
        "02 Mar'26, 03:07 AM",
        "UTR: YESF260613520320",
        "2,500.00",
        SettlementStatus.SETTLED
    ),
    SettlementData(
        "28 Feb'26, 09:21 PM",
        "Technical Error",
        "1,700.00",
        SettlementStatus.FAILED
    ),
    SettlementData(
        "13 Feb'26, 03:05 AM",
        "UTR: YESF260443408076",
        "1.00",
        SettlementStatus.SETTLED
    ),
    SettlementData(
        "07 Feb'26, 03:06 AM",
        "UTR: YESF260383474365",
        "101.00",
        SettlementStatus.SETTLED
    )
)
