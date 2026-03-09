package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yespaybiz.ui.theme.*

// ──────────────────────────────────────────────────────────
//  Colors from Figma CSS
// ──────────────────────────────────────────────────────────
private val ProfileGradientStart = Color(0xFF15317E)
private val ProfileGradientEnd = Color(0xFFA4B8F0)
private val UpgradeBannerGradientStart = Color(0xFF40A2DA)
private val UpgradeBannerGradientEnd = Color(0xFF37BEC7)
private val UpgradeCardBgStart = Color(0xFFCDF0FF)
private val UpgradeCardBgEnd = Color(0xFFE4F7FF)
private val UpgradeButtonColor = Color(0xFF1C85DB)
private val AlertRedBadge = Color(0xFFDA5555)

@Composable
fun ProfileScreen(onBackClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── HEADER ──
        ProfileHeader(onBackClick)

        // ── SCROLLABLE CONTENT ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 25.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 1: Profile Details, Settlement, Collection Fees ──
            ProfileMenuItem(Icons.Default.PersonOutline, "Profile Details")
            Spacer(modifier = Modifier.height(20.dp))
            ProfileMenuItem(Icons.Default.AccountBalance, "Settlement Account Details")
            Spacer(modifier = Modifier.height(20.dp))
            ProfileMenuItem(Icons.Default.CreditCard, "Collection Fees Management")

            Spacer(modifier = Modifier.height(20.dp))

            // Divider — Figma: 1px solid #E3E9EE
            HorizontalDivider(
                color = StrokeLight,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 2: Other menu items ──
            ProfileMenuItem(Icons.Default.Groups, "Sub-Users  |  4 users")
            Spacer(modifier = Modifier.height(25.dp))
            ProfileMenuItem(Icons.Default.Gavel, "Dispute Management")
            Spacer(modifier = Modifier.height(25.dp))
            ProfileMenuItem(Icons.Default.PhoneAndroid, "Manage Terminals")
            Spacer(modifier = Modifier.height(25.dp))
            ProfileMenuItem(Icons.Default.Language, "Language Setting  |  English")
            Spacer(modifier = Modifier.height(25.dp))
            ProfileMenuItem(Icons.Default.Support, "Support")
            Spacer(modifier = Modifier.height(25.dp))
            ProfileMenuItem(Icons.Default.SwapHoriz, "Switch Account")
            Spacer(modifier = Modifier.height(25.dp))
            ProfileMenuItem(Icons.Default.Call, "Contact Us")

            Spacer(modifier = Modifier.height(40.dp))

            // ── App Version ──
            Text(
                "App Version: 10.1.2",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────
//  HEADER with gradient + merchant info + upgrade card
// ──────────────────────────────────────────────────────────
@Composable
private fun ProfileHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Gradient background — Figma: linear-gradient(180deg, #15317E 0%, #A4B8F0 100%)
        // Rectangle 784: height 318dp, bottom-rounded 30dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(318.dp)
                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ProfileGradientStart, ProfileGradientEnd)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
        ) {
            Spacer(modifier = Modifier.height(47.dp))

            // Top bar: back arrow + "Profile" + "My Business Card" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back arrow
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() }
                )
                Spacer(modifier = Modifier.width(15.dp))
                // "Profile" — Figma: heading 20sp bold white
                Text(
                    "Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                // "My Business Card" pill button
                Surface(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .border(1.dp, StrokeLight, RoundedCornerShape(100.dp)),
                    color = Color(0xFFECF1FF),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "My Business Card",
                            fontSize = 11.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Merchant info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar placeholder — 56dp circle
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color(0xFF2A5CAE)
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = "Merchant",
                        tint = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    // Company name — Figma: H2 16sp bold white
                    Text(
                        "Deforus Technologies Pvt. Ltd.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        lineHeight = 23.sp
                    )
                    // Phone — Figma: 14sp, rgba(255,255,255,0.6)
                    Text(
                        "+91 8262062633",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Merchant type row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Red alert badge
                        Surface(
                            modifier = Modifier.size(14.dp),
                            shape = CircleShape,
                            color = AlertRedBadge
                        ) {
                            Text(
                                "!",
                                modifier = Modifier.fillMaxSize(),
                                textAlign = TextAlign.Center,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // "Basic Merchant" — Figma: H5 11sp bold white
                        Text(
                            "Basic Merchant",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            "  |  ",
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        // "Details Not Verified"
                        Text(
                            "Details Not Verified",
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // ── Upgrade to Elite merchant card ──
            UpgradeCard()
        }
    }
}

// ──────────────────────────────────────────────────────────
//  UPGRADE CARD
// ──────────────────────────────────────────────────────────
@Composable
private fun UpgradeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Bottom background — Figma: Trial/BG2 linear-gradient(180deg, #CDF0FF → #E4F7FF), 0 0 12 12 radius
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .height(76.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(UpgradeCardBgStart, UpgradeCardBgEnd)
                    )
                )
        )

        // Top banner — Figma: Trial/Blue2 GRD gradient 271deg #37BEC7 → #40A2DA, 44dp height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(UpgradeBannerGradientStart, UpgradeBannerGradientEnd)
                    )
                )
        ) {
            // "Upgrade Now" button — right side
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(UpgradeButtonColor)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Upgrade Now",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }

        // Elite badge — bottom-left, overlapping
        Surface(
            modifier = Modifier
                .size(width = 65.dp, height = 53.dp)
                .offset(x = 4.dp, y = 0.dp)
                .align(Alignment.BottomStart),
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(45.dp),
                    shape = CircleShape,
                    color = Color(0xFF2A5CAE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "ELITE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // "Become a Elite Merchant >" text — bottom section
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 15.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(55.dp))
                Text(
                    "Become a Elite Merchant",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextDark,
                    modifier = Modifier.size(16.dp)
                )
            }
            Row {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Increase your limit upto 30 lacs along with exciting benefits",
                    fontSize = 11.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
//  MENU ITEM ROW
// ──────────────────────────────────────────────────────────
@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = TextDark,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(25.dp))
        Text(
            label,
            fontSize = 14.sp,
            color = TextDark,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp
        )
    }
}
