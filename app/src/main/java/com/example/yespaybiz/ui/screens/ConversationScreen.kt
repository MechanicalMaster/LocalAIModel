package com.example.yespaybiz.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.util.Locale

@Composable
fun ConversationScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val modelState by viewModel.modelState.collectAsState()

    // File picker launcher
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importModelFromUri(context, uri)
        }
    }

    if (modelState is AIService.ModelState.MissingModel || modelState is AIService.ModelState.ImportingModel) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (modelState is AIService.ModelState.ImportingModel) {
                CircularProgressIndicator(color = PrimaryCTA)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Importing Model...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Copying the 300MB model to internal storage. This may take a minute.", textAlign = TextAlign.Center, color = TextGray)
            } else {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = PrimaryCTA)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No Model Found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please download the Gemma-3 1B model file (.task) and select it below to install.",
                    textAlign = TextAlign.Center,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    fileLauncher.launch(arrayOf("*/*"))
                }) {
                    Text("Select Model File")
                }
            }
        }
        return
    }

    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF234E91), Color(0xFF2A7DC0))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "YES BIZ AI Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        when (modelState) {
                            is AIService.ModelState.Loading,
                            is AIService.ModelState.ImportingModel,
                            is AIService.ModelState.MissingModel -> "Loading model…"
                            is AIService.ModelState.Ready -> "On-device · Offline"
                            is AIService.ModelState.Error -> "Error loading model"
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val dotColor = when (modelState) {
                    is AIService.ModelState.Ready -> Color(0xFF4CAF50)
                    is AIService.ModelState.Loading,
                    is AIService.ModelState.ImportingModel,
                    is AIService.ModelState.MissingModel -> Color(0xFFFFC107)
                    is AIService.ModelState.Error -> ErrorRed
                }
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = dotColor
                ) {}
            }
        }

        // ── Body ─────────────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (modelState) {
                is AIService.ModelState.Loading,
                is AIService.ModelState.ImportingModel,
                is AIService.ModelState.MissingModel -> ModelLoadingState()
                is AIService.ModelState.Error -> ModelErrorState(
                    message = (modelState as AIService.ModelState.Error).message
                )
                is AIService.ModelState.Ready -> {
                    if (messages.isEmpty()) {
                        EmptyConversationHint()
                    } else {
                        MessageList(messages = messages)
                    }
                }
            }
        }

        // ── Input Bar ───────────────────────────────────────────────────────────
        InputBar(
            enabled = modelState is AIService.ModelState.Ready && !isGenerating,
            isGenerating = isGenerating,
            onSend = { viewModel.sendMessage(it) }
        )
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────────

@Composable
private fun ModelLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = PrimaryCTA, strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Loading AI model…",
            fontSize = 14.sp,
            color = TextGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            "This may take a few seconds",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ModelErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = ErrorRed.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Model not found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "The model file was not found in internal storage.\nPlease use the button on the \"No Model Found\" screen to import it.",
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF6F7FC))
                .padding(8.dp)
        )
    }
}

@Composable
private fun EmptyConversationHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = SecondaryBlue
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = PrimaryCTA,
                modifier = Modifier.padding(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Ask me anything",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Powered by Gemma-3 1B · Runs fully on-device",
            fontSize = 13.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip("Show last 5 transactions")
            SuggestionChip("Today's collection")
        }
    }
}

@Composable
private fun SuggestionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SecondaryBlue
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = PrimaryCTA,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(msg)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Bottom),
                shape = CircleShape,
                color = SecondaryBlue
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = PrimaryCTA,
                    modifier = Modifier.padding(5.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser)
                            Brush.linearGradient(listOf(Color(0xFF234E91), PrimaryCTA))
                        else
                            Brush.linearGradient(listOf(Color.White, Color.White))
                    )
                    .then(
                        if (!isUser) Modifier
                            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                            .background(Color.White)
                        else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            color = if (isUser) Color.White else TextDark,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.height(4.dp))
                        StreamingDot()
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Bottom),
                shape = CircleShape,
                color = PrimaryCTA
            ) {
                Text(
                    "Y",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StreamingDot() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val delayedAlpha by rememberInfiniteTransition(label = "dot$i").animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(delayedAlpha)
                    .clip(CircleShape)
                    .background(TextGray)
            )
        }
    }
}

// ── Voice state ──────────────────────────────────────────────────────────────────

private enum class VoiceState { IDLE, LISTENING, ERROR }

@Composable
private fun InputBar(
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
    val context = LocalContext.current

    // SpeechRecognizer lives as a remembered ref so we can cancel it
    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) {
        onDispose { recognizer.destroy() }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening(recognizer, context) { result ->
                text = result
                voiceState = VoiceState.IDLE
            }
            voiceState = VoiceState.LISTENING
        }
    }

    fun onMicTap() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        if (voiceState == VoiceState.LISTENING) {
            recognizer.stopListening()
            voiceState = VoiceState.IDLE
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening(recognizer, context) { result ->
                text = result
                voiceState = VoiceState.IDLE
            }
            voiceState = VoiceState.LISTENING
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Pulsing scale animation for mic while listening
    val micScale by rememberInfiniteTransition(label = "mic_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // "Listening…" label
            if (voiceState == VoiceState.LISTENING) {
                Text(
                    "Listening…",
                    fontSize = 12.sp,
                    color = PrimaryCTA,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic button (left of input field)
                val micBg = if (voiceState == VoiceState.LISTENING)
                    PrimaryCTA
                else
                    Color(0xFFF0F4FF)

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .then(
                            if (voiceState == VoiceState.LISTENING)
                                Modifier.scale(micScale)
                            else Modifier
                        ),
                    shape = CircleShape,
                    color = micBg,
                    onClick = { onMicTap() }
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = if (voiceState == VoiceState.LISTENING) "Stop listening" else "Voice input",
                        tint = if (voiceState == VoiceState.LISTENING) Color.White else PrimaryCTA,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text field
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                isGenerating -> "Generating response…"
                                voiceState == VoiceState.LISTENING -> "Listening…"
                                else -> "Tap mic to speak, or type a message…"
                            },
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    enabled = enabled && voiceState != VoiceState.LISTENING,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCTA,
                        unfocusedBorderColor = StrokeLight,
                        disabledBorderColor = StrokeLight.copy(alpha = 0.5f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                FloatingActionButton(
                    onClick = {
                        if (text.isNotBlank() && enabled) {
                            onSend(text)
                            text = ""
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    containerColor = if (enabled && text.isNotBlank()) PrimaryCTA else StrokeLight,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── SpeechRecognizer helper ───────────────────────────────────────────────────────

private fun startListening(
    recognizer: SpeechRecognizer,
    context: android.content.Context,
    onResult: (String) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}

        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcript = matches?.firstOrNull().orEmpty()
            onResult(transcript)
        }

        override fun onError(error: Int) {
            // Surface error only for non-trivial cases (not user cancelled)
            if (error != SpeechRecognizer.ERROR_CLIENT &&
                error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY
            ) {
                onResult("") // clear / reset gracefully
            } else {
                onResult("")
            }
        }
    })

    recognizer.startListening(intent)
}
