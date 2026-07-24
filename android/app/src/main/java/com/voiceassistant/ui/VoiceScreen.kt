package com.voiceassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Єдиний екран: велика кнопка push-to-talk (hold-to-talk — тримай, щоб говорити),
 * індикатор стану, розпізнаний текст і відповідь.
 */
@Composable
fun VoiceScreen(
    vm: VoiceViewModel,
    micPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = ui.status,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        // Кнопка: колір відображає стан
        val color = when (ui.state) {
            UiState.RECORDING -> Color(0xFFD32F2F) // червоний — пишемо
            UiState.PROCESSING -> Color(0xFFF9A825) // жовтий — думаємо
            UiState.SPEAKING -> Color(0xFF388E3C) // зелений — говоримо
            UiState.ERROR -> Color(0xFF616161) // сірий — помилка
            UiState.IDLE -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(color)
                .pointerInput(micPermissionGranted) {
                    detectTapGestures(
                        onPress = {
                            if (!micPermissionGranted) {
                                onRequestPermission()
                            } else {
                                vm.onPressStart()
                                try {
                                    awaitRelease()
                                } finally {
                                    vm.onPressEnd()
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (ui.state == UiState.RECORDING) "Говори" else "Тримай",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (ui.recognizedText.isNotBlank()) {
            Text("Почув: ${ui.recognizedText}", textAlign = TextAlign.Center)
        }
        if (ui.replyText.isNotBlank()) {
            Text(
                text = ui.replyText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        ui.ttsWarning?.let {
            Text(it, color = Color(0xFFD32F2F), textAlign = TextAlign.Center)
        }
        if (!micPermissionGranted) {
            Text(
                "Потрібен дозвіл на мікрофон. Натисни кнопку, щоб надати.",
                textAlign = TextAlign.Center,
            )
        }
    }
}
