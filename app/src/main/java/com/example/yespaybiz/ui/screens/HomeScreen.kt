package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yespaybiz.ui.theme.*

@Composable
fun HomeScreen(onProfileClick: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        item { HeaderAndCollectSection(onProfileClick = onProfileClick) }
        item { StatsSection() }
        item { DisputesSection() }
        item { ManageBusinessSection() }
    }
}

// ──────────────────────────────────────────────────────────
//  HEADER + COLLECT MONEY
// ──────────────────────────────────────────────────────────

@Composable
fun HeaderAndCollectSection(onProfileClick: () -> Unit = {}) {
    // Figma: Rectangle 1029 — "Trial/BG1" reversed: ECF1FF → D5E0FF top area
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFECF1FF), Color(0xFFD5E0FF))
                )
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 25.dp)) {
            Spacer(modifier = Modifier.height(48.dp))

            // Merchant Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo / avatar
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryCTA
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = "Store",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sethiya Gold",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark
                    )
                    Text(
                        "Collect Requests",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
                // Profile icon — navigates to Profile page
                IconButton(onClick = { onProfileClick() }) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = TextDark
                    )
                }
                // Volume icon
                IconButton(onClick = {}) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Sound",
                        tint = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Collect Money label — Figma: H2, 16sp, bold, #222222
            Text(
                "Collect Money",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input Box — Figma: white, shadow 0px 10px 30px rgba(0,0,0,0.1), 12px radius
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Figma: "₹" in TextDark 16sp
                    Text(
                        "₹",
                        fontSize = 16.sp,
                        color = TextDark,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Enter Amount",
                        fontSize = 16.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Figma: "Select a mode to proceed" — 12sp, #677F9D
            Text(
                "Select a mode to proceed",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row — 4 buttons: UPI QR, UPI Collect, Cash@UPI, eRUPI
            // Figma: #2A7DC0 blue rectangle 45.71x45.71dp, 10px radius; label 11sp #222 0.8 opacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PaymentModeItem(Icons.Default.QrCode, "UPI QR")
                PaymentModeItem(Icons.Default.TouchApp, "UPI Collect")
                PaymentModeItem(Icons.Default.CurrencyRupee, "Cash@UPI")
                PaymentModeItem(Icons.Default.QrCodeScanner, "eRUPI")
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun PaymentModeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        // Figma: Rectangle 840 — #2A7DC0 bg, 10dp radius, 45.71x45.71dp
        Surface(
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(10.dp),
            color = PrimaryCTA
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.padding(11.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Figma: 11sp, #222222, opacity 0.8, center
        Text(
            label,
            fontSize = 11.sp,
            color = TextDark.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ──────────────────────────────────────────────────────────
//  STATS SECTION
// ──────────────────────────────────────────────────────────

@Composable
fun StatsSection() {
    // Figma: Rectangle 841 — "Trial/BG1" gradient #D5E0FF→#ECF1FF, 12dp radius
    Box(
        modifier = Modifier
            .padding(horizontal = 25.dp)
            .padding(top = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFD5E0FF), Color(0xFFECF1FF))
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Figma: Rectangle 842 — #798CC2 left panel, 12dp radius, shadow
            Box(
                modifier = Modifier
                    .weight(192f / 325f) // 192px out of 325px total card width
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF798CC2))
                    .padding(horizontal = 19.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Today's UPI Collection column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Today's UPI",
                            fontSize = 11.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Collection",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD6F5),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "₹10,125",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Figma: Vector 271 — 0px wide, 1px border white 10% opacity divider
                    Spacer(modifier = Modifier.width(1.dp).height(55.dp).background(Color.White.copy(alpha = 0.1f)))

                    // Last Month's Collection column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Last Month's",
                            fontSize = 11.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Collection",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD6F5),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "₹58,873",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Settlement Amount — right 1/3 of card
            Column(
                modifier = Modifier
                    .weight(133f / 325f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Settlement Amount",
                    fontSize = 11.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    "(Pending till date)",
                    fontSize = 11.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Figma: amount + right-pointing caret in #1C85DB
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "₹10,8730",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF1C85DB),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
//  DISPUTES SECTION
// ──────────────────────────────────────────────────────────

@Composable
fun DisputesSection() {
    // Figma: Rectangle 928 — #FFF3F3 bg, 12dp radius
    Box(
        modifier = Modifier
            .padding(horizontal = 25.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3F3))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Figma: Group 2108 — 3 concentric semi-transparent circles then solid #DA5555 circle
            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                // Outer circles — Ellipse 348/347/346 — 20% opacity red rings
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = ErrorRed.copy(alpha = 0.07f)
                ) {}
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = ErrorRed.copy(alpha = 0.12f)
                ) {}
                // Inner solid circle
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = ErrorRed
                ) {
                    // Bell icon — rotated -45deg in design, use NotificationsActive
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = "Disputes",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Figma: "Pending Disputes" H2 16sp bold #000
            Text(
                "Pending Disputes",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )

            // Figma: Dispute count in #DA5555
            Text(
                "0",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ErrorRed
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Figma: fi-rs-angle-small-down rotated -90deg → pointing left
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
//  MANAGE BUSINESS
// ──────────────────────────────────────────────────────────

@Composable
fun ManageBusinessSection() {
    Column(
        modifier = Modifier
            .padding(horizontal = 25.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        // Figma: H2 16sp bold #000
        Text(
            "Manage your Business",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            item { BusinessCard("Order YESPAY BIZ\nQR Standee") }
            item { BusinessCard("Order YESPAY BIZ\nSoundBox") }
        }
    }
}

@Composable
fun BusinessCard(titleText: String) {
    // Figma: Frame 2182 — 153x173dp, #F6F7FC bg, 8dp radius
    Box(
        modifier = Modifier
            .width(153.dp)
            .height(173.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ItemCardBg)
    ) {
        // Illustration bottom area — Figma: Vector 365 #798CC2, Vector 366 #1C85DB
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            // Figma: #798CC2 full-width band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .background(Color(0xFF798CC2))
            )
        }
        // #1C85DB overlay band at bottom (57dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(57.dp)
                .background(Color(0xFF1C85DB))
        )

        // Product label — Figma: 96x38dp centered, 12sp #000
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 15.dp)
                .width(96.dp)
        ) {
            Text(
                titleText,
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }

        // Device illustration placeholder — centered icon
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Icon(
                    if (titleText.contains("Standee")) Icons.Default.QrCode
                    else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
