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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
    val context     = LocalContext.current
    val modelState  by viewModel.modelState.collectAsState()
    val messages    by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    // TTS engine — single instance for the whole screen
    var ttsReady by remember { mutableStateOf(false) }
    var speakingId by remember { mutableStateOf<Long?>(null) }  // message ID currently being spoken
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
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun speak(msg: ChatMessage) {
        if (!ttsReady) return
        if (speakingId == msg.id) {
            tts?.stop()
            speakingId = null
            return
        }
        tts?.stop()
        speakingId = msg.id
        val utteranceId = msg.id.toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uid: String?) {}
            override fun onDone(uid: String?)  { speakingId = null }
            override fun onError(uid: String?) { speakingId = null }
        })
        // Strip emoji/symbols for cleaner TTS
        val cleaned = msg.text.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\s₹]"), "")
            .replace("₹", " rupees ")
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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

    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        // ── Header ───────────────────────────────────────────────────────────────
        ChatHeader(modelState)

        // ── Body ─────────────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (modelState) {
                is AIService.ModelState.Loading,
                is AIService.ModelState.ImportingModel,
                is AIService.ModelState.MissingModel -> ModelLoadingState()
                is AIService.ModelState.Error -> ModelErrorState(
                    (modelState as AIService.ModelState.Error).message
                )
                is AIService.ModelState.Ready -> {
                    if (messages.isEmpty()) EmptyConversationHint()
                    else MessageList(
                        messages       = messages,
                        speakingId     = speakingId,
                        onChipClick    = { viewModel.sendMessage(it) },
                        onSpeakClick   = { speak(it) }
                    )
                }
            }
        }

        // ── Input Bar ────────────────────────────────────────────────────────────
        InputBar(
            enabled      = modelState is AIService.ModelState.Ready && !isGenerating,
            isGenerating = isGenerating,
            onSend       = { viewModel.sendMessage(it) }
        )
    }
}

// ── Missing model screen ──────────────────────────────────────────────────────────

@Composable
private fun MissingModelScreen(importing: Boolean, onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (importing) {
            CircularProgressIndicator(color = PrimaryCTA)
            Spacer(Modifier.height(16.dp))
            Text("Importing Model...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Copying the 300MB model to internal storage. This may take a minute.",
                textAlign = TextAlign.Center, color = TextGray)
        } else {
            Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = PrimaryCTA)
            Spacer(Modifier.height(16.dp))
            Text("No Model Found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Please download the Gemma-3 1B model file (.task) and select it below.",
                textAlign = TextAlign.Center, color = TextGray)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPickFile) { Text("Select Model File") }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(modelState: AIService.ModelState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF234E91), Color(0xFF2A7DC0))))
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI Assistant", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Text(
                    when (modelState) {
                        is AIService.ModelState.Ready -> "On-device · Private"
                        is AIService.ModelState.Error -> "Error loading model"
                        else -> "Loading model…"
                    },
                    fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f)
                )
            }
            Spacer(Modifier.weight(1f))
            val dotColor = when (modelState) {
                is AIService.ModelState.Ready -> Color(0xFF4CAF50)
                is AIService.ModelState.Error -> ErrorRed
                else -> Color(0xFFFFC107)
            }
            Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = dotColor) {}
        }
    }
}

// ── Body states ───────────────────────────────────────────────────────────────────

@Composable
private fun ModelLoadingState() {
    Column(modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = PrimaryCTA, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text("Loading AI model…", fontSize = 14.sp, color = TextGray, fontWeight = FontWeight.Medium)
        Text("This may take a few seconds", fontSize = 12.sp, color = TextMuted,
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ModelErrorState(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = ErrorRed.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)) {
            Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Model not found", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text("The model file was not found. Please use the import button to install it.",
            fontSize = 12.sp, color = TextGray, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(message, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF6F7FC)).padding(8.dp))
    }
}

@Composable
private fun EmptyConversationHint() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = SecondaryBlue) {
            Icon(Icons.Default.SmartToy, null, tint = PrimaryCTA, modifier = Modifier.padding(20.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Ask me anything", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text("Powered by Gemma-3 1B · Runs fully on-device",
            fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HintChip("Aaj ka hisaab")
            HintChip("Show transactions")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HintChip("Settlement status")
            HintChip("Weekly trend")
        }
    }
}

@Composable
private fun HintChip(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = SecondaryBlue) {
        Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp, color = PrimaryCTA, fontWeight = FontWeight.Medium)
    }
}

// ── Message list ──────────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    speakingId: Long?,
    onChipClick: (String) -> Unit,
    onSpeakClick: (ChatMessage) -> Unit
) {
    val listState    = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) coroutineScope.launch {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(
                message      = msg,
                isSpeaking   = speakingId == msg.id,
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
    onChipClick: (String) -> Unit,
    onSpeakClick: (ChatMessage) -> Unit
) {
    val isUser = message.role == Role.USER
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(
                            topStart    = if (isUser) 16.dp else 4.dp,
                            topEnd      = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd   = if (isUser) 4.dp else 16.dp
                        ))
                        .background(
                            if (isUser) Brush.linearGradient(listOf(Color(0xFF234E91), PrimaryCTA))
                            else Brush.linearGradient(listOf(Color.White, Color.White))
                        )
                        .then(
                            if (!isUser) Modifier
                                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .background(Color.White) else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        if (message.text.isNotEmpty()) {
                            Text(
                                text       = message.text,
                                color      = if (isUser) Color.White else TextDark,
                                fontSize   = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                        if (message.isStreaming) {
                            Spacer(Modifier.height(4.dp))
                            StreamingDot()
                        }
                    }
                }

                // Speaker icon — below bubble, right-aligned, shown when not streaming
                if (!message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val speakerTint = if (isSpeaking) PrimaryCTA else TextMuted
                        Icon(
                            imageVector        = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop speaking" else "Read aloud",
                            tint               = speakerTint,
                            modifier           = Modifier
                                .size(16.dp)
                                .clickable { onSpeakClick(message) }
                        )
                    }
                }
                
                // Timestamp
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start).padding(horizontal = 4.dp)
                )
            }

            // User avatar
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Surface(modifier = Modifier.size(28.dp).align(Alignment.Bottom),
                    shape = CircleShape, color = PrimaryCTA) {
                    Text("Y", modifier = Modifier.fillMaxSize().wrapContentSize(),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Follow-up chips (assistant only, done streaming)
        if (!isUser && !message.isStreaming && message.suggestedFollowUps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .padding(start = 36.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestedFollowUps.forEach { chip ->
                    Surface(
                        shape    = RoundedCornerShape(16.dp),
                        color    = SecondaryBlue,
                        modifier = Modifier.clickable { onChipClick(chip) }
                    ) {
                        Text(chip,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp, color = PrimaryCTA, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingDot() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val alpha by rememberInfiniteTransition(label = "dot$i").animateFloat(
                initialValue = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot_alpha_$i"
            )
            Box(Modifier.size(6.dp).alpha(alpha).clip(CircleShape).background(TextGray))
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
    var text         by remember { mutableStateOf("") }
    var voiceState   by remember { mutableStateOf(VoiceState.IDLE) }
    var partialText  by remember { mutableStateOf("") }
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

    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Status label
            when (voiceState) {
                VoiceState.LISTENING -> Text(
                    if (partialText.isNotEmpty()) partialText else "Listening…",
                    fontSize = 12.sp, color = PrimaryCTA, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
                )
                VoiceState.TIMEOUT_ERROR -> Text(
                    "Didn't catch that — tap mic to try again",
                    fontSize = 12.sp, color = ErrorRed, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
                )
                else -> {}
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mic button
                Surface(
                    modifier = Modifier.size(44.dp)
                        .then(if (voiceState == VoiceState.LISTENING) Modifier.scale(micScale) else Modifier),
                    shape = CircleShape,
                    color = if (voiceState == VoiceState.LISTENING) PrimaryCTA else Color(0xFFF0F4FF),
                    onClick = { onMicTap() }
                ) {
                    Icon(
                        Icons.Default.Mic,
                        if (voiceState == VoiceState.LISTENING) "Stop" else "Voice input",
                        tint = if (voiceState == VoiceState.LISTENING) Color.White else PrimaryCTA,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))

                val displayText = if (voiceState == VoiceState.LISTENING && partialText.isNotEmpty())
                    partialText else text

                OutlinedTextField(
                    value = displayText,
                    onValueChange = { if (voiceState != VoiceState.LISTENING) text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                isGenerating -> "Generating response…"
                                voiceState == VoiceState.LISTENING -> "Listening…"
                                else -> "Tap mic to speak, or type a message…"
                            },
                            fontSize = 13.sp, color = TextMuted
                        )
                    },
                    enabled = enabled && voiceState != VoiceState.LISTENING,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryCTA,
                        unfocusedBorderColor = StrokeLight,
                        disabledBorderColor  = StrokeLight.copy(alpha = 0.5f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                Spacer(Modifier.width(8.dp))

                // Send button
                FloatingActionButton(
                    onClick = { if (text.isNotBlank() && enabled) { onSend(text); text = "" } },
                    modifier = Modifier.size(44.dp),
                    containerColor = if (enabled && text.isNotBlank()) PrimaryCTA else StrokeLight,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    if (isGenerating)
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp))
                    else
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
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
