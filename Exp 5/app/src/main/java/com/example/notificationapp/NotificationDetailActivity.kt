package com.example.notificationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notificationapp.ui.theme.AccentLavender
import com.example.notificationapp.ui.theme.CardSurface
import com.example.notificationapp.ui.theme.GradientBottom
import com.example.notificationapp.ui.theme.GradientMid
import com.example.notificationapp.ui.theme.GradientTop
import com.example.notificationapp.ui.theme.NotificationAppTheme

/**
 * Destination of the content PendingIntent — proves the notification carried
 * its data across into the app.
 */
class NotificationDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val title = intent.getStringExtra(IntentKeys.EXTRA_NOTIF_TITLE).orEmpty()
        val message = intent.getStringExtra(IntentKeys.EXTRA_NOTIF_MESSAGE).orEmpty()
        val type = intent.getStringExtra(IntentKeys.EXTRA_NOTIF_TYPE).orEmpty()

        setContent {
            NotificationAppTheme(dynamicColor = false) {
                NotificationDetailScreen(
                    title = title,
                    message = message,
                    type = type,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun NotificationDetailScreen(
    title: String,
    message: String,
    type: String,
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientTop, GradientMid, GradientBottom)))
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(AccentLavender.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MarkEmailRead,
                            contentDescription = null,
                            tint = AccentLavender,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Notification opened",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        type.ifEmpty { "Unknown" } + " notification",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Title", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            Text(
                                title.ifEmpty { "unknown" },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Message", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            Text(
                                message.ifEmpty { "unknown" },
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Back", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
