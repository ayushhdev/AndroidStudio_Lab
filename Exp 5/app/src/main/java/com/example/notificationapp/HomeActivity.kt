package com.example.notificationapp

import android.content.Intent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import java.util.UUID

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val email = intent.getStringExtra(IntentKeys.EXTRA_EMAIL).orEmpty()
        val loginTime = intent.getStringExtra(IntentKeys.EXTRA_LOGIN_TIME).orEmpty()

        setContent {
            NotificationAppTheme(dynamicColor = false) {
                HomeScreen(
                    email = email,
                    loginTime = loginTime,
                    onViewProfile = {
                        val sessionId = UUID.randomUUID().toString().take(8).uppercase()
                        val intent = Intent(this, ProfileActivity::class.java).apply {
                            putExtra(IntentKeys.EXTRA_EMAIL, email)
                            putExtra(IntentKeys.EXTRA_LOGIN_TIME, loginTime)
                            putExtra(IntentKeys.EXTRA_SESSION_ID, sessionId)
                        }
                        startActivity(intent)
                    },
                    onOpenNotifications = {
                        val intent = Intent(this, NotificationsActivity::class.java).apply {
                            putExtra(IntentKeys.EXTRA_EMAIL, email)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    email: String,
    loginTime: String,
    onViewProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }

    val displayName = email.substringBefore("@").ifEmpty { "there" }

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
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(600))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).background(AccentLavender.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Welcome back, $displayName!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "You're signed in",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            InfoRow(icon = Icons.Filled.Person, label = "Email", value = email.ifEmpty { "unknown" })
                            Spacer(modifier = Modifier.height(16.dp))
                            InfoRow(icon = Icons.Filled.Schedule, label = "Logged in at", value = loginTime.ifEmpty { "unknown" })
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onViewProfile,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentLavender,
                                    contentColor = Color(0xFF1A0B2E)
                                ),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text("View Profile", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onOpenNotifications,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Notifications", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
