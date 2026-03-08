package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yespaybiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        TopAppBar(
            title = { Text("Show QR", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = PrimaryBlue)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = PrimaryBlue)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
            )
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Switcher — pill shape
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = PrimaryBlue
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    // UPI QR (active)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("UPI QR", color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                    // CBDC QR (inactive)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CBDC QR", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Scan the code below to make the payment",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // QR Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // QR Code placeholder
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp),
                            tint = TextDark
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // VPA line with orange diamond marker
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(12.dp)
                                    .rotate(45f),
                                color = UpiOrange,
                                shape = RoundedCornerShape(2.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "VPA : yespay.bizbiz144072@yesbankltd",
                                fontSize = 12.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "RONAK SHANTILAL SETHIYA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                    }

                    // UPI badge — bottom left
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(y = (-24).dp),
                        color = UpiOrange,
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    ) {
                        Text(
                            "UPI",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dropdown Selector — solid blue
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF3B82F6),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Base Location (Master Terminal)",
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Pay with any app", fontSize = 14.sp, color = TextGray)
            Spacer(modifier = Modifier.height(16.dp))

            // Payment partner icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = "Bank", tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                Icon(Icons.Default.CreditCard, contentDescription = "Card", tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                Icon(Icons.Default.Payments, contentDescription = "Pay", tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                Icon(Icons.Default.Savings, contentDescription = "Save", tint = PrimaryBlue, modifier = Modifier.size(32.dp))
            }
        }
    }
}
