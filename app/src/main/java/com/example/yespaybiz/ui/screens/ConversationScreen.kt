package com.example.yespaybiz.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.yespaybiz.ai.AIService
import com.example.yespaybiz.ai.ChatMessage
import com.example.yespaybiz.ai.ChatViewModel
import com.example.yespaybiz.ai.Role
import com.example.yespaybiz.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale

// ── Design tokens ─────────────────────────────────────────────────────────────────
private val PageBg        = Color(0xFFF0F2F8)
private val CardWhite     = Color(0xFFFFFFFF)
private val HeaderBg      = Color(0xFFFFFFFF)
private val SparkleBlue   = Color(0xFF5B5FEF)   // the sparkle/diamond icon colour
private val SubtitleGray  = Color(0xFF8A94A6)
private val HeadingDark   = Color(0xFF1A1F36)
private val ChipBg        = Color(0xFFFFFFFF)
private val ChipBorder    = Color(0xFFE3E9EE)
private val InputBarBg    = Color(0xFFFFFFFF)
private val SendBtn       = Color(0xFF3B5BDB)    // deep indigo-blue circle send button
private val HoldOrange    = Color(0xFFF59E0B)
private val CheckGreen    = Color(0xFF22C55E)

@Composable
fun ConversationScreen(viewModel: ChatViewModel) {
    val context      = LocalContext.current
    val modelState   by viewModel.modelState.collectAsState()
    val messages     by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    // TTS engine
    var ttsReady  by remember { mutableStateOf(false) }
    var speakingId by remember { mutableStateOf<Long?>(null) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                ttsReady = true
            }
        }
        engine
    }
    DisposableEffect(Unit) {
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    fun speak(msg: ChatMessage) {
        if (!ttsReady) return
        if (speakingId == msg.id) { tts?.stop(); speakingId = null; return }
        tts?.stop()
        speakingId = msg.id
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uid: String?) {}
            override fun onDone(uid: String?)  { speakingId = null }
            override fun onError(uid: String?) { speakingId = null }
        })
        val cleaned = msg.text.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\s₹]"), "")
            .replace("₹", " rupees ")
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, msg.id.toString())
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importModelFromUri(context, uri) }

    if (modelState is AIService.ModelState.MissingModel ||
        modelState is AIService.ModelState.ImportingModel) {
        MissingModelScreen(
            importing = modelState is AIService.ModelState.ImportingModel,
            onPickFile = { fileLauncher.launch(arrayOf("*/*")) }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ───────────────────────────────────────────────────────────
            ChatHeader(modelState)

            // ── Body ─────────────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when (modelState) {
                    is AIService.ModelState.Loading,
                    is AIService.ModelState.ImportingModel,
                    is AIService.ModelState.MissingModel -> ModelLoadingState()
                    is AIService.ModelState.Error -> ModelErrorState(
                        (modelState as AIService.ModelState.Error).message
                    )
                    is AIService.ModelState.Ready -> {
                        // Show the greeting/empty state only when the sole message is the auto-greeting
                        val onlyGreeting = messages.size == 1 && messages[0].role == Role.ASSISTANT
                        if (messages.isEmpty() || onlyGreeting) {
                            GreetingEmptyState(
                                greetingMessage = messages.firstOrNull(),
                                onChipClick = { viewModel.sendMessage(it) }
                            )
                        } else {
                            MessageList(
                                messages     = messages,
                                speakingId   = speakingId,
                                onChipClick  = { viewModel.sendMessage(it) },
                                onSpeakClick = { speak(it) }
                            )
                        }
                    }
                }
            }

            // ── Input Bar ────────────────────────────────────────────────────────
            InputBar(
                enabled      = modelState is AIService.ModelState.Ready && !isGenerating,
                isGenerating = isGenerating,
                onSend       = { viewModel.sendMessage(it) }
            )
        }
    }
}

// ── Missing model screen ──────────────────────────────────────────────────────────

@Composable
private fun MissingModelScreen(importing: Boolean, onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(PageBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (importing) {
            CircularProgressIndicator(color = SendBtn)
            Spacer(Modifier.height(16.dp))
            Text("Importing Model…", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HeadingDark)
            Spacer(Modifier.height(8.dp))
            Text("Copying the model to internal storage. This may take a minute.",
                textAlign = TextAlign.Center, color = SubtitleGray)
        } else {
            Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = SendBtn)
            Spacer(Modifier.height(16.dp))
            Text("No Model Found", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HeadingDark)
            Spacer(Modifier.height(8.dp))
            Text("Please download the Gemma-3 1B model file (.task) and select it below.",
                textAlign = TextAlign.Center, color = SubtitleGray)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(containerColor = SendBtn)
            ) { Text("Select Model File") }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(modelState: AIService.ModelState) {
    Surface(
        color = HeaderBg,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sparkle / AI icon
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = SparkleBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI Assistant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = HeadingDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusDot, statusLabel) = when (modelState) {
                        is AIService.ModelState.Ready -> CheckGreen to "On-device • Private"
                        is AIService.ModelState.Error -> ErrorRed to "Error loading model"
                        else -> HoldOrange to "Loading model…"
                    }
                    Box(
                        Modifier.size(6.dp).clip(CircleShape).background(statusDot)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(statusLabel, fontSize = 11.sp, color = SubtitleGray)
                }
            }
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = SubtitleGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Body states ───────────────────────────────────────────────────────────────────

@Composable
private fun ModelLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().background(PageBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = SendBtn, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text("Loading AI model…", fontSize = 14.sp, color = SubtitleGray, fontWeight = FontWeight.Medium)
        Text("This may take a few seconds", fontSize = 12.sp, color = SubtitleGray.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ModelErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(PageBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = ErrorRed.copy(alpha = 0.12f), modifier = Modifier.size(64.dp)) {
            Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Model not found", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HeadingDark)
        Spacer(Modifier.height(8.dp))
        Text("The model file was not found. Please use the import button to install it.",
            fontSize = 12.sp, color = SubtitleGray, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(message, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(CardWhite).padding(8.dp), fontSize = 12.sp, color = SubtitleGray)
    }
}

// ── Greeting / empty state ────────────────────────────────────────────────────────

@Composable
private fun GreetingEmptyState(
    greetingMessage: ChatMessage?,
    onChipClick: (String) -> Unit
) {
    val hour = LocalTime.now().hour
    val timeGreeting = when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else      -> "Good evening,"
    }
    val waveEmoji = "\uD83D\uDC4B" // 👋

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        // Greeting text
        Text(
            "$timeGreeting $waveEmoji",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = HeadingDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Here's what's happening today",
            fontSize = 15.sp,
            color = SubtitleGray
        )

        Spacer(Modifier.height(20.dp))

        // Summary card
        DailySummaryCard()

        Spacer(Modifier.height(28.dp))

        // Try asking label
        Text(
            "Try asking",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SubtitleGray
        )
        Spacer(Modifier.height(12.dp))

        // Suggestion chips row
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                icon = Icons.Default.BarChart,
                iconTint = Color(0xFF3B5BDB),
                label = "Today's collections",
                onClick = { onChipClick("Today's collections") }
            )
            SuggestionChip(
                icon = Icons.Default.AccountBalance,
                iconTint = Color(0xFF3B5BDB),
                label = "Settlement status",
                onClick = { onChipClick("Settlement status") }
            )
            SuggestionChip(
                icon = Icons.Default.AccessTime,
                iconTint = HoldOrange,
                label = "Held payments",
                onClick = { onChipClick("Show hold transactions") }
            )
        }

        // Also show any follow-up chips from the greeting message
        if (greetingMessage != null && greetingMessage.suggestedFollowUps.isNotEmpty()) {
            val extras = greetingMessage.suggestedFollowUps.filterNot { chip ->
                listOf("Daily summary", "Settlement status", "Show hold transactions")
                    .any { it.equals(chip, ignoreCase = true) }
            }
            if (extras.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extras.forEach { chip ->
                        SuggestionChip(
                            icon = Icons.Default.Chat,
                            iconTint = SparkleBlue,
                            label = chip,
                            onClick = { onChipClick(chip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left — total collected
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "₹42,850",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeadingDark
                )
                Text(
                    "collected today",
                    fontSize = 13.sp,
                    color = SubtitleGray
                )
            }

            // Right — payment stats
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CheckGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "18 successful payments",
                        fontSize = 13.sp,
                        color = HeadingDark
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = HoldOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "2 transactions on hold",
                        fontSize = 13.sp,
                        color = HeadingDark
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = SendBtn,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChipBg,
        shadowElevation = 1.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = HeadingDark, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Message list ──────────────────────────────────────────────────────────────────

// null = no feedback yet, true = thumbs up, false = thumbs down
@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    speakingId: Long?,
    onChipClick: (String) -> Unit,
    onSpeakClick: (ChatMessage) -> Unit
) {
    val listState      = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // messageId -> true (up) / false (down) / absent (none)
    val feedbackMap    = remember { mutableStateMapOf<Long, Boolean>() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) coroutineScope.launch {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(PageBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(
                message      = msg,
                isSpeaking   = speakingId == msg.id,
                feedback     = feedbackMap[msg.id],
                onFeedback   = { value -> feedbackMap[msg.id] = value },
                onChipClick  = onChipClick,
                onSpeakClick = onSpeakClick
            )
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    feedback: Boolean?,        // null=none, true=up, false=down
    onFeedback: (Boolean) -> Unit,
    onChipClick: (String) -> Unit,
    onSpeakClick: (ChatMessage) -> Unit
) {
    val isUser = message.role == Role.USER
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            // AI avatar
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEFFB))
                        .align(Alignment.Bottom),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SparkleBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart    = if (isUser) 18.dp else 4.dp,
                        topEnd      = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd   = if (isUser) 4.dp else 18.dp
                    ),
                    color = if (isUser) SendBtn else CardWhite,
                    shadowElevation = if (isUser) 0.dp else 1.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (message.text.isNotEmpty()) {
                            Text(
                                text       = message.text,
                                color      = if (isUser) Color.White else HeadingDark,
                                fontSize   = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                        if (message.isStreaming) {
                            Spacer(Modifier.height(4.dp))
                            StreamingDot(color = if (isUser) Color.White.copy(alpha = 0.7f) else SubtitleGray)
                        }
                    }
                }

                // Action row: speaker + (assistant-only) thumbs up/down
                if (!message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Speaker
                        Icon(
                            imageVector        = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop speaking" else "Read aloud",
                            tint               = if (isSpeaking) SendBtn else SubtitleGray.copy(alpha = 0.6f),
                            modifier           = Modifier.size(15.dp).clickable { onSpeakClick(message) }
                        )
                        // Thumbs — only for completed assistant messages
                        if (!isUser) {
                            Icon(
                                imageVector        = Icons.Default.ThumbUp,
                                contentDescription = "Helpful",
                                tint               = if (feedback == true) CheckGreen else SubtitleGray.copy(alpha = 0.55f),
                                modifier           = Modifier.size(15.dp).clickable {
                                    onFeedback(true)
                                }
                            )
                            Icon(
                                imageVector        = Icons.Default.ThumbDown,
                                contentDescription = "Not helpful",
                                tint               = if (feedback == false) ErrorRed else SubtitleGray.copy(alpha = 0.55f),
                                modifier           = Modifier.size(15.dp).clickable {
                                    onFeedback(false)
                                }
                            )
                        }
                    }
                }

                // Timestamp
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = message.timestamp,
                    fontSize = 10.sp,
                    color    = SubtitleGray.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(if (isUser) Alignment.End else Alignment.Start)
                        .padding(horizontal = 4.dp)
                )
            }

            // User avatar
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SendBtn.copy(alpha = 0.15f))
                        .align(Alignment.Bottom),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Y",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SendBtn
                    )
                }
            }
        }

        // Follow-up chips (assistant only, done streaming)
        if (!isUser && !message.isStreaming && message.suggestedFollowUps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .padding(start = 38.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestedFollowUps.forEach { chip ->
                    Surface(
                        shape    = RoundedCornerShape(16.dp),
                        color    = CardWhite,
                        shadowElevation = 1.dp,
                        modifier = Modifier.clickable { onChipClick(chip) }
                    ) {
                        Text(
                            chip,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            color    = SendBtn,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingDot(color: Color = SubtitleGray) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val alpha by rememberInfiniteTransition(label = "dot$i").animateFloat(
                initialValue = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot_alpha_$i"
            )
            Box(Modifier.size(6.dp).alpha(alpha).clip(CircleShape).background(color))
        }
    }
}

// ── Voice state ────────────────────────────────────────────────────────────────────

private enum class VoiceState { IDLE, LISTENING, TIMEOUT_ERROR }

// ── Input Bar ─────────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: (String) -> Unit
) {
    var text        by remember { mutableStateOf("") }
    var voiceState  by remember { mutableStateOf(VoiceState.IDLE) }
    var partialText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening(recognizer, context,
                onPartial = { partialText = it },
                onResult  = { result -> text = result; voiceState = VoiceState.IDLE; partialText = "" },
                onTimeout = { voiceState = VoiceState.TIMEOUT_ERROR; partialText = "" }
            )
            voiceState = VoiceState.LISTENING
        }
    }

    fun onMicTap() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        if (voiceState == VoiceState.LISTENING) {
            recognizer.stopListening(); voiceState = VoiceState.IDLE; partialText = ""; return
        }
        if (voiceState == VoiceState.TIMEOUT_ERROR) voiceState = VoiceState.IDLE
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            startListening(recognizer, context,
                onPartial = { partialText = it },
                onResult  = { result -> text = result; voiceState = VoiceState.IDLE; partialText = "" },
                onTimeout = { voiceState = VoiceState.TIMEOUT_ERROR; partialText = "" }
            )
            voiceState = VoiceState.LISTENING
        } else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val micScale by rememberInfiniteTransition(label = "mic_pulse").animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "mic_scale"
    )

    // Outer surface gives the bottom bar a white pill-within-page look
    Surface(color = PageBg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Status label (voice feedback)
            when (voiceState) {
                VoiceState.LISTENING -> Text(
                    if (partialText.isNotEmpty()) partialText else "Listening…",
                    fontSize = 12.sp, color = SendBtn, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
                )
                VoiceState.TIMEOUT_ERROR -> Text(
                    "Didn't catch that — tap mic to try again",
                    fontSize = 12.sp, color = ErrorRed, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
                )
                else -> {}
            }

            // Input pill
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = InputBarBg,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sparkle icon inside the input pill
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SparkleBlue.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))

                    val displayText = if (voiceState == VoiceState.LISTENING && partialText.isNotEmpty())
                        partialText else text

                    BasicInputField(
                        value          = displayText,
                        onValueChange  = { if (voiceState != VoiceState.LISTENING) text = it },
                        placeholder    = when {
                            isGenerating -> "Generating response…"
                            voiceState == VoiceState.LISTENING -> "Listening…"
                            else -> "Ask about collections, settlements, or transactions…"
                        },
                        enabled        = enabled && voiceState != VoiceState.LISTENING,
                        modifier       = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(6.dp))

                    // Mic button (inside pill, ghost style)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .then(if (voiceState == VoiceState.LISTENING) Modifier.scale(micScale) else Modifier)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == VoiceState.LISTENING)
                                    SparkleBlue.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onMicTap() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = if (voiceState == VoiceState.LISTENING) "Stop" else "Voice input",
                            tint = if (voiceState == VoiceState.LISTENING) SparkleBlue else SubtitleGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Send button — filled circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled && text.isNotBlank()) SendBtn
                                else SubtitleGray.copy(alpha = 0.3f)
                            )
                            .clickable(enabled = enabled && text.isNotBlank()) {
                                onSend(text); text = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Thin text field without the OutlinedTextField border chrome
@Composable
private fun BasicInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 13.sp,
                color = SubtitleGray.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = HeadingDark
            ),
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── SpeechRecognizer ──────────────────────────────────────────────────────────────

private fun startListening(
    recognizer: SpeechRecognizer,
    context: android.content.Context,
    onPartial: (String) -> Unit,
    onResult: (String) -> Unit,
    onTimeout: () -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(p: Bundle?)  {}
        override fun onBeginningOfSpeech()          {}
        override fun onRmsChanged(rms: Float)       {}
        override fun onBufferReceived(buf: ByteArray?) {}
        override fun onEndOfSpeech()                {}
        override fun onEvent(type: Int, p: Bundle?) {}
        override fun onPartialResults(b: Bundle?) {
            val partial = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (partial.isNotEmpty()) onPartial(partial)
        }
        override fun onResults(b: Bundle?) {
            onResult(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
        }
        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_NO_MATCH -> onTimeout()
                else -> onResult("")
            }
        }
    })
    recognizer.startListening(intent)
}
