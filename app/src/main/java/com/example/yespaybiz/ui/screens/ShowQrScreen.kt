package com.example.yespaybiz.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.yespaybiz.ui.theme.*

// Figma Colors
private val TopBgStart = Color(0xFFD5E0FF)
private val TopBgEnd = Color(0xFFECF1FF)
private val PrimaryCta = Color(0xFF1C85DB)
private val TextDark80 = Color(0xFF222222)
private val StrokeLight = Color(0xFFE3E9EE)
private val TerminalBg = Color(0xFF798CC2)
private val VpaIconColor = Color(0xFFE9661C)

@Composable
fun ShowQrScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Gradient Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TopBgStart, TopBgEnd)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 45.dp) // SafeArea simulation
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryCta
                    )
                }
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    "Show QR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark80
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Share Icon
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = PrimaryCta,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                // Download Icon (SaveAlt)
                Icon(
                    Icons.Default.SaveAlt,
                    contentDescription = "Download",
                    tint = PrimaryCta,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Merchant Name
            Text(
                "Deforus Technologies Private Limited",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextDark80
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Toggle Button (UPI QR | CBDC QR)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(193.dp)
                        .height(33.dp),
                    shape = RoundedCornerShape(39.dp),
                    color = PrimaryCta,
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Selected Pill (UPI QR)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(4.dp)
                                .width(90.dp)
                                .height(25.dp),
                            shape = RoundedCornerShape(36.dp),
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "UPI QR",
                                    fontSize = 12.sp,
                                    color = TextDark80,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // Unselected (CBDC QR)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 15.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "CBDC QR",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // YES BANK LOGO
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Red Tick Placeholder for Logo
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFED1F48),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(-15f)
                )
                Text(
                    "YES BANK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF002EDC),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Card
                Surface(
                    modifier = Modifier
                        .width(325.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.5f.dp, StrokeLight)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Scan the code below to make the payment",
                            fontSize = 14.sp,
                            color = TextDark80,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // QR Code Image (Placeholder)
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(281.dp),
                            tint = TextDark80
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // VPA Line
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Custom VPA Icon (Two overlapping triangles per Figma)
                            Canvas(modifier = Modifier.size(width = 12.dp, height = 18.dp)) {
                                val path1 = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(size.width, size.height/2)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(path1, color = VpaIconColor.copy(alpha = 0.6f))
                                
                                val path2 = Path().apply {
                                    moveTo(size.width * 0.25f, 0f)
                                    lineTo(size.width, size.height/2)
                                    lineTo(size.width * 0.25f, size.height)
                                    close()
                                }
                                drawPath(path2, color = VpaIconColor)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "VPA : abc123@yesbankltd",
                                fontSize = 12.sp,
                                color = TextDark80
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Account Name
                        Text(
                            "Mr. BHUSHAN ANIL DIXIT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark80
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Terminal Selector
                Surface(
                    modifier = Modifier
                        .width(325.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = TerminalBg,
                    border = BorderStroke(1.dp, StrokeLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dadar Branch (Terminal 1)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Terminal",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pay with any app
                Text(
                    "Pay with any app",
                    fontSize = 11.sp,
                    color = TextDark80,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // App Icons Row
                Row(
                    modifier = Modifier.width(309.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Placeholders for YesPay, GPay, PhonePe, Paytm, Amazon, BHIM, UPI
                    PaymentAppIcon(Color.Blue, "Yes")
                    PaymentAppIcon(Color.White, "G", hasBorder = true)
                    PaymentAppIcon(Color(0xFF6739B7), "Pe")
                    PaymentAppIcon(Color.White, "Pay", hasBorder = true)
                    PaymentAppIcon(Color.White, "a", hasBorder = true)
                    PaymentAppIcon(Color.White, "BHIM", hasBorder = true)
                    PaymentAppIcon(Color.White, "UPI", hasBorder = true)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun PaymentAppIcon(bgColor: Color, label: String, hasBorder: Boolean = false) {
    Surface(
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = bgColor,
        border = if (hasBorder) BorderStroke(1.dp, StrokeLight) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (bgColor == Color.White) TextDark80 else Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
